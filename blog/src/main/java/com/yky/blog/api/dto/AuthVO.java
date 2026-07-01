package com.yky.blog.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthVO {
    private String token;
    private UserVO user;
}
