package com.yky.blog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EventReportDTO {

    @NotBlank(message = "事件类型不能为空")
    @Size(max = 50, message = "事件类型长度不能超过50")
    private String eventType;

    @NotBlank(message = "访客标识不能为空")
    @Size(max = 100, message = "访客标识长度不能超过100")
    private String visitorId;

    private Long articleId;

    private Long tagId;

    private Long collectionId;

    @Size(max = 200, message = "关键词长度不能超过200")
    private String keyword;

    @Size(max = 500, message = "页面地址长度不能超过500")
    private String pageUrl;

    @Size(max = 500, message = "来源页面长度不能超过500")
    private String referrer;

    private Long duration;
}
