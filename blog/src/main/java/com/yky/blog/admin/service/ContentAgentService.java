package com.yky.blog.admin.service;

import com.yky.blog.admin.dto.AgentDraftDTO;
import com.yky.blog.admin.dto.AgentTopicsDTO;
import com.yky.blog.admin.vo.AgentDraftVO;
import com.yky.blog.admin.vo.AgentTopicsVO;

/**
 * 第二层：工具型 Agent（选题助手 / 内容创作助手）。
 */
public interface ContentAgentService {

    /** 基于平台埋点数据生成选题列表。 */
    AgentTopicsVO suggestTopics(AgentTopicsDTO dto, Object adminId);

    /** 按选题/要点生成文章草稿并落库（status=0）。 */
    AgentDraftVO generateDraft(AgentDraftDTO dto, Object adminId);
}
