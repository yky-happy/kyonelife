export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface TagItem {
  id: number
  name: string
  color: string
  articleCount: number
}

export interface CollectionItem {
  id: number
  name: string
  cover: string
  description: string
  sort: number
  articleCount: number
  createTime: string
}

export interface ArticleCard {
  id: number
  title: string
  cover: string
  summary: string
  collectionId: number | null
  collectionName: string
  tags: TagItem[]
  isStick: number
  viewCount: number
  createTime: string
}

export interface ArticleDetail extends ArticleCard {
  content: string
  contentMd: string
  keywords: string
  isOriginal: number
  originalUrl: string
}

export interface ArchiveArticle {
  id: number
  title: string
  summary: string
  createTime: string
}

export interface ArchiveMonth {
  month: string
  articles: ArchiveArticle[]
}
