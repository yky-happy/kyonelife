import request from '../utils/request'
import type { TagItem } from './types'

export const getTagList = () =>
  request.get<unknown, TagItem[]>('/api/tag/list')
