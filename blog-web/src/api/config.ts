import request from '../utils/request'

export interface WebConfig {
  siteName: string
  logo: string | null
  summary: string | null
  author: string
  authorAvatar: string | null
  signature: string
  github: string
  email: string
  aboutMe: string | null
  icpNumber: string
  bulletin: string
}

/** 站点配置（站点名/作者/社交/公告/页脚/备案号等） */
export const getSiteConfig = () => request.get<unknown, WebConfig>('/api/config')
