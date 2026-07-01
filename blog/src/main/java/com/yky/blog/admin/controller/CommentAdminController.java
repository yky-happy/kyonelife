package com.yky.blog.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yky.blog.admin.service.CommentAdminService;
import com.yky.blog.admin.vo.AdminCommentVO;
import com.yky.blog.common.annotation.OperationLogRecord;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台评论管理")
@RestController
@RequestMapping("/admin/comment")
@RequiredArgsConstructor
public class CommentAdminController {

    private final CommentAdminService commentAdminService;

    @Operation(summary = "分页查询评论")
    @SaCheckPermission("comment:list")
    @GetMapping("/page")
    public Result<IPage<AdminCommentVO>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) Long articleId,
                                              @RequestParam(required = false) String keyword) {
        return Result.success(commentAdminService.page(page, size, articleId, keyword));
    }

    @Operation(summary = "删除评论")
    @SaCheckPermission("comment:delete")
    @OperationLogRecord(module = "评论管理", operation = "删除评论")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentAdminService.delete(id);
        return Result.success();
    }

    @Operation(summary = "隐藏/显示评论")
    @SaCheckPermission("comment:edit")
    @OperationLogRecord(module = "评论管理", operation = "隐藏/显示评论")
    @PatchMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        commentAdminService.updateStatus(id, status);
        return Result.success();
    }
}
