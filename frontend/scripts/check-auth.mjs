// 使用本地契约服务检查前端请求与登录状态逻辑，不代表真实后端联调。
import assert from 'node:assert/strict'
import http from 'node:http'
import { registerHooks } from 'node:module'
import { readFileSync } from 'node:fs'
import ts from 'typescript'
import { createPinia, setActivePinia } from 'pinia'

const sourceRoot = new URL('../src/', import.meta.url)
registerHooks({
  resolve(specifier, context, next) {
    if (specifier.startsWith('@/'))
      return { url: new URL(specifier.slice(2) + '.ts', sourceRoot).href, shortCircuit: true }
    if (
      context.parentURL?.startsWith(sourceRoot.href) &&
      specifier.startsWith('.') &&
      !specifier.endsWith('.ts')
    ) {
      return { url: new URL(specifier + '.ts', context.parentURL).href, shortCircuit: true }
    }
    return next(specifier, context)
  },
  load(url, context, next) {
    if (url.startsWith(sourceRoot.href) && url.endsWith('.ts')) {
      const source = readFileSync(new URL(url), 'utf8').replace(
        'import.meta.env.VITE_API_BASE_URL',
        'undefined',
      )
      return {
        format: 'module',
        source: ts.transpileModule(source, {
          compilerOptions: { target: ts.ScriptTarget.ESNext, module: ts.ModuleKind.ESNext },
        }).outputText,
        shortCircuit: true,
      }
    }
    return next(url, context)
  },
})

const storage = new Map()
globalThis.localStorage = {
  getItem: (key) => storage.get(key) ?? null,
  setItem: (key, value) => storage.set(key, value),
  removeItem: (key) => storage.delete(key),
}
globalThis.window = new EventTarget()
const user = { id: 7, username: 'contract', displayName: '真实用户', skills: ['Vue'] }
let unauthorized = false
let unavailable = false
let authorization
let onProfileRequest
let releaseProfile
const backend = http.createServer(async (req, res) => {
  let body = ''
  for await (const chunk of req) body += chunk
  authorization = req.headers.authorization
  res.setHeader('Content-Type', 'application/json')
  if (unavailable) {
    res.statusCode = 503
    res.end('{}')
    return
  }
  if (req.url === '/auth/login' || req.url === '/auth/register') {
    if (JSON.parse(body).password === 'wrong') {
      res.statusCode = 401
      res.end(JSON.stringify({ code: 40100, message: 'invalid credentials', data: null }))
    } else res.end(JSON.stringify({ code: 0, data: { token: 'contract-token', user } }))
    return
  }
  if (unauthorized || authorization !== 'Bearer contract-token') {
    res.statusCode = 401
    res.end('{}')
    return
  }
  if (req.method === 'PATCH' && onProfileRequest) {
    onProfileRequest()
    await new Promise((resolve) => {
      releaseProfile = resolve
    })
  }
  res.end(
    JSON.stringify({
      code: 0,
      data: req.method === 'PATCH' ? { ...user, ...JSON.parse(body) } : user,
    }),
  )
})
await new Promise((resolve) => backend.listen(0, '127.0.0.1', resolve))
try {
  setActivePinia(createPinia())
  const { request } = await import('../src/api/request.ts')
  request.defaults.baseURL = `http://127.0.0.1:${backend.address().port}`
  const api = await import('../src/api/users.ts')
  const { useAuthStore } = await import('../src/stores/auth.ts')
  const { setSessionToken } = await import('../src/api/session.ts')
  const auth = useAuthStore()
  let expiryEvents = 0
  window.addEventListener('teamflow:unauthorized', () => {
    expiryEvents++
    auth.clear()
  })
  await assert.rejects(api.login({ username: 'contract', password: 'wrong' }))
  assert.equal(expiryEvents, 0, '错误密码不能触发全局过期跳转')
  auth.accept(
    await api.register({ username: 'contract', password: 'correct', displayName: '测试用户' }),
  )
  assert.equal(auth.token, 'contract-token', '注册成功后建立登录状态')
  auth.accept(await api.login({ username: 'contract', password: 'correct' }))
  assert.equal(authorization, undefined, '登录请求不携带旧 Token')
  assert.equal(storage.get('teamflow.token'), 'contract-token')
  auth.user = null
  await auth.restore()
  assert.equal(auth.user.id, 7, '刷新后从当前用户接口恢复身份')
  assert.equal(authorization, 'Bearer contract-token')
  assert.equal((await api.updateProfile({ displayName: '新昵称' })).displayName, '新昵称')
  const arrived = new Promise((resolve) => {
    onProfileRequest = resolve
  })
  const saving = auth.saveProfile({ displayName: '旧账号昵称' })
  await arrived
  auth.clear()
  auth.accept({ token: 'contract-token', user: { ...user, id: 8, displayName: '新账号' } })
  releaseProfile()
  assert.equal(await saving, false, '退出再登录后，旧保存请求不能覆盖新身份')
  assert.equal(auth.user.id, 8)
  onProfileRequest = undefined
  auth.user = null
  unavailable = true
  await auth.restore()
  assert.equal(auth.token, 'contract-token', '网络或服务失败不能清除登录凭据')
  assert.ok(auth.error)
  unavailable = false
  unauthorized = true
  await assert.rejects(api.getCurrentUser())
  assert.equal(expiryEvents, 1)
  assert.equal(auth.user, null)
  assert.equal(storage.has('teamflow.token'), false)
  // 迟到的旧请求返回 401 时，不能清除刚建立的新会话。
  const oldConfig = { url: '/users/me', headers: { Authorization: 'Bearer old-token' } }
  setSessionToken('new-token')
  const { default: axios } = await import('axios')
  const expired = new axios.AxiosError('expired', undefined, oldConfig, undefined, { status: 401 })
  const handler = request.interceptors.response.handlers[0]
  await assert.rejects(handler.rejected(expired))
  assert.equal(auth.token, 'new-token')
  auth.clear()
  console.log(
    'PASS: 注册登录、Token 持久化、身份恢复、资料修改、网络失败、401 清理、跨会话保存隔离',
  )
} finally {
  await new Promise((resolve) => backend.close(resolve))
}
