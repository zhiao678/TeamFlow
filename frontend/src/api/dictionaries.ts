import { request } from './request'
export interface TagOption {
  id: number
  name: string
}
export interface CategoryOption extends TagOption {
  parentId: number | null
  sortOrder: number
}
async function read<T>(path: string) {
  const { data } = await request.get<{ code: number; message: string; data: T }>(path)
  if (data.code !== 0) throw new Error(data.message || '选项加载失败')
  return data.data
}
export const listCategories = () => read<CategoryOption[]>('/categories')
export const listTags = () => read<TagOption[]>('/tags')
