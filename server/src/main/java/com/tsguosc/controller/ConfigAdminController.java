package com.tsguosc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tsguosc.common.constant.ConfigKeys;
import com.tsguosc.common.constant.Roles;
import com.tsguosc.common.result.Result;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.dto.ConfigUpdateRequest;
import com.tsguosc.dto.ConfigVO;
import com.tsguosc.service.SysConfigService;
import com.tsguosc.util.ClubLogoStorage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    private final ClubLogoStorage clubLogoStorage;

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

    /**
     * 上传社团 Logo（F-001 第 1 步，仅超管）。
     *
     * <p>Logo 存的是**对象 key**、不经文本更新接口，所以单独一个上传端点；
     * 换图时先把旧对象删掉（不留孤儿文件），再写新 key。
     *
     * @return 新的 Logo 可访问地址；未配置 Logo 时为 {@code null}
     */
    @PostMapping("/admin/logo")
    @SaCheckRole(Roles.SUPER_ADMIN)
    public Result<String> uploadLogo(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要上传的 Logo 图片");
        }
        String previous = sysConfigService.find(ConfigKeys.CLUB_LOGO).orElse(null);
        String key = clubLogoStorage.upload(file);
        sysConfigService.writeInternal(ConfigKeys.CLUB_LOGO, key);
        // 写库成功后再删旧对象；删失败只记日志，不回滚（新图已生效）
        clubLogoStorage.delete(previous);
        return Result.ok(resolvePublicUrl(key), "Logo 已更新");
    }

    /** 清除社团 Logo（删除对象 + 置空配置；报名页回到不显示 Logo 的状态） */
    @DeleteMapping("/admin/logo")
    @SaCheckRole(Roles.SUPER_ADMIN)
    public Result<Void> removeLogo() {
        String previous = sysConfigService.find(ConfigKeys.CLUB_LOGO).orElse(null);
        sysConfigService.writeInternal(ConfigKeys.CLUB_LOGO, null);
        clubLogoStorage.delete(previous);
        return Result.ok(null, "Logo 已清除");
    }

    /** 对象 key → 可访问地址（与报名页出参同一口径） */
    private String resolvePublicUrl(String key) {
        return key == null ? null : clubLogoStorage.publicUrl(key);
    }
}
