import { request } from './request'
import { ApplicationError } from './applications'
import type { Recruitment } from '@/types/recruitment'

export async function listFavorites(signal?: AbortSignal) {
  const { data } = await request.get<{ code: number; message: string; data: Recruitment[] }>(
    '/recruitments/favorites',
    { signal },
  )
  if (data.code !== 0) throw new ApplicationError(data.code, data.message)
  return data.data
}
export async function setFavorite(id: number, favorite: boolean) {
  const path = `/recruitments/${id}/favorite`
  const { data } = await (favorite ? request.post(path) : request.delete(path))
  if (data.code !== 0) throw new ApplicationError(data.code, data.message)
}
