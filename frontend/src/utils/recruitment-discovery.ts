import type { RecruitmentQuery } from '@/types/recruitment'
export const blankDiscovery = () => ({
  keyword: '',
  categoryId: '',
  courseName: '',
  teacherName: '',
  status: 'RECRUITING',
  sort: 'LATEST',
  tagIds: '',
  minRemainingSlots: '',
})
export function discoveryFilters(query: Record<string, unknown>) {
  const result = blankDiscovery()
  for (const key of Object.keys(result) as (keyof typeof result)[]) {
    const value = query[key]
    if (typeof value === 'string') result[key] = value.trim()
  }
  if (!['RECRUITING', 'TEAMED', 'CLOSED'].includes(result.status)) result.status = 'RECRUITING'
  if (!['LATEST', 'DEADLINE', 'POPULAR'].includes(result.sort)) result.sort = 'LATEST'
  result.tagIds = [
    ...new Set(
      result.tagIds
        .split(',')
        .filter((id) => /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id))),
    ),
  ].join(',')
  if (
    !/^\d+$/.test(result.minRemainingSlots) ||
    !Number.isSafeInteger(Number(result.minRemainingSlots))
  )
    result.minRemainingSlots = ''
  return result
}
const positive = (value: unknown, fallback: number) => {
  const n = typeof value === 'string' ? Number(value) : NaN
  return Number.isSafeInteger(n) && n > 0 ? n : fallback
}
export function discoveryQuery(query: Record<string, unknown>): RecruitmentQuery {
  const filters = discoveryFilters(query)
  return {
    ...filters,
    categoryId: positive(filters.categoryId, 0) || undefined,
    tagIds: filters.tagIds || undefined,
    minRemainingSlots:
      filters.minRemainingSlots === '' ? undefined : Number(filters.minRemainingSlots),
    status: filters.status as RecruitmentQuery['status'],
    sort: filters.sort as RecruitmentQuery['sort'],
    page: positive(query.page, 1),
    pageSize: Math.min(100, positive(query.pageSize, 20)),
  }
}
