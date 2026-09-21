package com.tsguosc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tsguosc.common.constant.Roles;
import com.tsguosc.common.result.Result;
import com.tsguosc.dto.ConfigUpdateRequest;
import com.tsguosc.dto.ConfigVO;
import com.tsguosc.service.SysConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统配置管理接口（仅超管）——「纳新设置」页用。
 *
 * <p>路径三段（/config/admin/xxx），不在拦截器白名单内，天然需要登录 + 超管角色。
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigAdminController {

    private final SysConfigService sysConfigService;

    /** 全部配置（含短信模板等，便于超管核对） */
    @GetMapping("/admin/list")
    @SaCheckRole(Roles.SUPER_ADMIN)
    public Result<List<ConfigVO>> list() {
        return Result.ok(sysConfigService.list());
    }

    /** 更新单个配置（仅允许改 ConfigKeys.EDITABLE_KEYS 内的键） */
    @PutMapping("/admin/update")
    @SaCheckRole(Roles.SUPER_ADMIN)
    public Result<Void> update(@Valid @RequestBody ConfigUpdateRequest request) {
        sysConfigService.update(request);
        return Result.ok(null, "保存成功");
    }
}
