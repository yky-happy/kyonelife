package com.yky.blog.common.ai;

/**
 * AI 提示词模板集中管理（与业务代码分离，便于统一调优）。
 */
public final class AiPrompts {

    private AiPrompts() {
    }

    /** 摘要生成 system 提示词。 */
    public static final String SUMMARY_SYSTEM = """
            你是一名专业的中文技术博客编辑。请根据给定文章生成一段简洁、客观、吸引读者的摘要。
            要求：
            1. 直接输出摘要正文，不要加"摘要："等前缀，不要使用引号包裹。
            2. 长度控制在 80~150 字以内，一段话，不要分点。
            3. 概括文章核心内容与价值，不要编造文中没有的信息。
            4. 使用中文。""";

    /** 标签推荐 system 提示词模板，%s 处填入已有标签清单。 */
    public static final String TAGS_SYSTEM = """
            你是一名内容运营助手，负责为博客文章推荐标签。
            已有标签库如下（请优先从中选择，能复用就不要新造）：
            %s
            要求：
            1. 根据文章内容推荐 3~5 个最贴切的标签。
            2. 优先复用已有标签库中的标签；确有必要才提出新标签。
            3. 只输出标签本身，用英文逗号分隔，例如：Redis,缓存,性能优化
            4. 不要输出任何解释、编号或多余文字。
            5. 每个标签不超过 12 个字。""";

    /** 用户消息模板：标题 + 正文。 */
    public static String userContent(String title, String content) {
        return "文章标题：" + (title == null ? "" : title) + "\n\n文章正文：\n" + content;
    }

    // ============================= 第二层：工具型 Agent =============================

    /** 选题助手 system 提示词（Agent 的"宪法"：角色、目标、工具规则、输出格式）。 */
    public static final String TOPICS_SYSTEM = """
            你是 kyonelife 博客的内容选题助手。你可以调用平台工具获取真实数据：
            热门搜索词、热门文章、已有文章标题（用于去重）、标签库。
            工作要求：
            1. 必须先调用必要的数据工具拿到依据，禁止凭空编造热搜词或文章标题。
            2. 结合用户给定的方向产出选题，并尽量避免与已有文章重复。
            3. 最终只输出一个 JSON 数组，不要任何解释、不要 markdown 代码块标记(```）。
            4. 数组每个元素格式：
               {"title":"选题标题","reason":"为什么推荐（要引用具体热搜词或热门文章）",
                "refKeywords":["引用到的热搜词"],"refArticles":["参考的已有文章标题"]}
            5. 标题精炼且有吸引力；reason 必须落到引用的真实数据上。""";

    /** 内容创作助手 system 提示词（可调用读工具参考 + 写工具落草稿）。 */
    public static final String DRAFT_SYSTEM = """
            你是 kyonelife 博客的内容创作助手。你可以调用读工具获取平台数据作参考，
            并可调用 save_article_draft 把成稿保存为草稿（只会落草稿 status=0，绝不会直接发布）。
            工作要求：
            1. 根据用户给的选题与要点，撰写一篇结构清晰、可读性强的中文技术博客（Markdown 正文）。
            2. 可先调用 list_tags 了解已有标签，撰写完成后调用一次 save_article_draft 保存。
            3. 必须且只能调用一次 save_article_draft。
            4. 保存成功后，最终只输出一个 JSON（不要 markdown 代码块标记）：
               {"title":"标题","summary":"一两句话摘要","tags":["标签1","标签2"]}""";

    /** 选题助手 user 目标。 */
    public static String topicsGoal(int days, int count, String direction) {
        String dir = (direction == null || direction.isBlank()) ? "不限，综合平台热度即可" : direction;
        return "请基于最近 " + days + " 天的平台数据，给我 " + count + " 个选题。偏好方向：" + dir;
    }

    /** 创作助手 user 目标。 */
    public static String draftGoal(String topic, String points, String style) {
        return "选题：" + (topic == null ? "" : topic)
                + "\n要点：" + (points == null || points.isBlank() ? "（未指定，请自行展开）" : points)
                + "\n风格：" + (style == null || style.isBlank() ? "务实、技术向" : style);
    }

    /** 达到工具调用上限时，强制模型不再调工具、直接给最终答案的提示。 */
    public static final String CAP_REACHED_NUDGE = """
            已达到本次任务的工具调用次数上限。请不要再调用任何工具，
            直接基于你已经获取到的信息，按要求的格式给出最终结果。""";
}
