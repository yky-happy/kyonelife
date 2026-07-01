import { reactive } from 'vue'
import bannerHome from '../assets/prince-planet-hero.jpg'
import bannerPage from '../assets/hero.png'

export type PageHeaderType = 'home' | 'post' | 'page'

export interface PageHeaderState {
  type: PageHeaderType
  /** 站点 / 页面主标题 */
  title: string
  /** 副标题：page 类型显示在标题下方的小字 */
  subtitle: string
  /** 背景图（单图：page / post 类型用） */
  bg: string
  /** home 轮播图列表（多图轮播；为空时回退到 bg） */
  bgList: string[]
  /** home 类型的打字机短句 */
  typewriter: string[]
  /** 标题是否使用宋体 */
  songTitle: boolean
  /** 是否隐藏顶部 Banner（首页搜索时直接展示结果） */
  hidden: boolean
  /** post 类型：发表日期 */
  postDate: string
  /** post 类型：更新日期 */
  postUpdate: string
  /** post 类型：分类 / 合集名 */
  postCategory: string
  /** post 类型：合集 id（用于点击跳转，无合集为 null） */
  postCategoryId: number | null
  /** post 类型：浏览量 */
  postViews: number
}

const DEFAULT: PageHeaderState = {
  type: 'page',
  title: '',
  subtitle: '',
  bg: bannerPage,
  bgList: [],
  typewriter: [],
  songTitle: false,
  hidden: false,
  postDate: '',
  postUpdate: '',
  postCategory: '',
  postCategoryId: null,
  postViews: 0,
}

export const pageHeader = reactive<PageHeaderState>({ ...DEFAULT })

/** 视图在 onMounted / watch 中调用，驱动顶部 Banner 的展示内容 */
export function setPageHeader(patch: Partial<PageHeaderState>) {
  Object.assign(pageHeader, DEFAULT, patch)
}

export const HOME_BANNER = bannerHome
export const PAGE_BANNER = bannerPage
