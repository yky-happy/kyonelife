package com.yky.blog.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserVO {
    private Long id;
    private String account;
    private String nickname;
    private String avatar;
    private String email;
}
