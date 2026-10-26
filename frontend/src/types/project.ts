export interface Project {
  id: number
  name: string
  description: string | null
  ownerId: number
  ownerDisplayName: string
  status: 'ACTIVE' | 'ARCHIVED'
  createdAt: string
  updatedAt: string
}
