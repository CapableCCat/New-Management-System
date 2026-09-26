package com.tsguosc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.tsguosc.common.constant.Roles;
import com.tsguosc.common.result.Result;
import com.tsguosc.dto.AnnouncementDetailVO;
import com.tsguosc.dto.AnnouncementQuery;
import com.tsguosc.dto.AnnouncementSaveRequest;
import com.tsguosc.dto.AnnouncementVO;
import com.tsguosc.dto.PageResult;
import com.tsguosc.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 公告系统（PRD F-008）。
 *
 * <p>权限分层（PRD 第五章权限矩阵）：
 * <ul>
 *   <li>{@code /announcement/list}、{@code /announcement/{id}}：**登录即可**（成员端只读）。
 *       两段路径不在 {@code SaTokenConfig} 白名单内，天然要求登录</li>
 *   <li>{@code /announcement/admin/**}：限超管 / 社长团 / 部长（PRD「发布公告」一行）。
 *       三段路径同样天然需登录，再由注解卡角色</li>
 * </ul>
 *
 * <p>注意 {@code /announcement/admin/**} 不会被 {@code /dict/*} 那样的两段通配误放行
 * （Ant 的 {@code *} 只匹配一段路径），白名单无需改动。
 */
@RestController
@RequestMapping("/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /** 公告列表（置顶优先 + 发布时间倒序；成员端与管理端共用，管理端多一个标题检索） */
    @GetMapping("/list")
    public Result<PageResult<AnnouncementVO>> list(AnnouncementQuery query) {
        return Result.ok(announcementService.list(query));
    }

    /** 公告详情（正文为已清洗的富文本 HTML） */
    @GetMapping("/{id:\\d+}")
    public Result<AnnouncementDetailVO> detail(@PathVariable Long id) {
        return Result.ok(announcementService.detail(id));
    }

    /** 发布公告 */
    @PostMapping("/admin/create")
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP, Roles.MINISTER}, mode = SaMode.OR)
    public Result<Long> create(@Valid @RequestBody AnnouncementSaveRequest request) {
        return Result.ok(announcementService.create(request), "发布成功");
    }

    /** 编辑公告（整体提交：标题 / 正文 / 是否置顶） */
    @PutMapping("/admin/{id:\\d+}")
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP, Roles.MINISTER}, mode = SaMode.OR)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AnnouncementSaveRequest request) {
        announcementService.update(id, request);
        return Result.ok(null, "保存成功");
    }

    /** 删除公告（逻辑删除） */
    @DeleteMapping("/admin/{id:\\d+}")
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP, Roles.MINISTER}, mode = SaMode.OR)
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.ok(null, "已删除");
    }

    /** 上传公告配图（jpg / png，≤2MB），返回可直接嵌进富文本的完整地址 */
    @PostMapping("/admin/image")
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP, Roles.MINISTER}, mode = SaMode.OR)
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.ok(announcementService.uploadImage(file));
    }
}
