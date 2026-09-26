package com.tsguosc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.tsguosc.common.constant.Roles;
import com.tsguosc.common.result.Result;
import com.tsguosc.dto.FeedbackHandleRequest;
import com.tsguosc.dto.FeedbackQuery;
import com.tsguosc.dto.FeedbackSubmitRequest;
import com.tsguosc.dto.FeedbackVO;
import com.tsguosc.dto.PageResult;
import com.tsguosc.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 轻量反馈接口（F-014）。
 *
 * <p>路径分两段与三段，正好用白名单区分权限（同 {@code DictController} 的做法）：
 *   <ul>
 *     <li>{@code /feedback/submit}（两段）→ 在白名单里，**免登录**（提交时仍需图形验证码）</li>
 *     <li>{@code /feedback/admin/xxx}（三段）→ 不在白名单，天然要求登录，再由 {@code @SaCheckRole} 限社长团 / 超管</li>
 *   </ul>
 */
@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /** 提交反馈（免登录，带一次性图形验证码） */
    @PostMapping("/submit")
    public Result<Void> submit(@Valid @RequestBody FeedbackSubmitRequest request) {
        feedbackService.submit(request);
        return Result.ok(null, "感谢反馈，我们会尽快查看");
    }

    /** 反馈列表（社长团 / 超管，供复盘） */
    @GetMapping("/admin/list")
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP}, mode = SaMode.OR)
    public Result<PageResult<FeedbackVO>> list(FeedbackQuery query) {
        return Result.ok(feedbackService.list(query));
    }

    /** 标记已处理 / 未处理（社长团 / 超管） */
    @PostMapping("/admin/handle")
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP}, mode = SaMode.OR)
    public Result<Void> handle(@Valid @RequestBody FeedbackHandleRequest request) {
        feedbackService.handle(request);
        return Result.ok(null, Objects.equals(request.handled(), 1) ? "已标记为已处理" : "已标记为未处理");
    }
}
