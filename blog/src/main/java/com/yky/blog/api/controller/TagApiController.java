package com.yky.blog.api.controller;

import com.yky.blog.api.dto.TagApiVO;
import com.yky.blog.api.service.TagApiService;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台标签")
@RestController
@RequestMapping("/api/tag")
@RequiredArgsConstructor
public class TagApiController {

    private final TagApiService tagApiService;

    @Operation(summary = "前台标签列表")
    @GetMapping("/list")
    public Result<List<TagApiVO>> list() {
        return Result.success(tagApiService.listTags());
    }
}
