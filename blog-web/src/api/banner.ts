import request from '../utils/request'
import type { BannerItem } from './types'

/** 前台首页启用的轮播图列表（按 sort 倒序） */
export const getBannerList = () =>
  request.get<unknown, BannerItem[]>('/api/banner/list')
