import { ref } from 'vue'
import { getArticlePage, getHotArticles } from '../api/article'
import { getTagList } from '../api/tag'
import { getCollectionList } from '../api/collection'
import type { ArticleCard, CollectionItem, TagItem } from '../api/types'

export const hotArticles = ref<ArticleCard[]>([])
export const allTags = ref<TagItem[]>([])
export const allCollections = ref<CollectionItem[]>([])
export const totalArticles = ref(0)

let loaded = false
let inflight: Promise<void> | null = null

/** 站点侧栏数据：进程内只拉取一次，跨页面复用 */
export function ensureSiteData(): Promise<void> {
  if (loaded) return Promise.resolve()
  if (inflight) return inflight
  inflight = (async () => {
    try {
      const [page, hot, tagList, collectionList] = await Promise.all([
        getArticlePage({ page: 1, size: 1 }), // 仅取总数
        getHotArticles(5),
        getTagList(),
        getCollectionList(),
      ])
      totalArticles.value = page.total
      hotArticles.value = hot
      allTags.value = tagList
      allCollections.value = collectionList
      loaded = true
    } catch {
      // 侧栏数据加载失败不影响主内容
    } finally {
      inflight = null
    }
  })()
  return inflight
}
