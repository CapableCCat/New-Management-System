package com.tsguosc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
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
 * 系统配置管理接口 ——「纳新设置」页用。
 *
 * <p>路径三段（/config/admin/xxx），不在拦截器白名单内，天然需要登录 + 角色校验。
 *
 * <p><b>权限（T27 起）</b>：本页放的都是纳新运营配置（报名开关、审核时效、社团简介、社团 Logo、短信模板），
 * 属于社长团日常要调的东西，故四个端点统一放宽为 **超管 或 社长团**（原仅超管，《收尾需求》§5.3）。
 *
 * <p>⚠️ 两条**没有**跟着放宽的边界（都在别处）：
 * <ul>
 *   <li>**字典管理**仍仅超管（{@code DictController} 未改）—— 系统级配置</li>
 *   <li>**存储维护**仍仅超管（{@code StorageAdminController} 未改）—— 会真删对象存储的文件</li>
 * </ul>
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigAdminController {

    private final SysConfigService sysConfigService;
    private final ClubLogoStorage clubLogoStorage;

    /** 全部配置（含短信模板等，便于超管核对） */
    @GetMapping("/admin/list")
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP}, mode = SaMode.OR)
    public Result<List<ConfigVO>> list() {
        return Result.ok(sysConfigService.list());
    }

    /** 更新单个配置（仅允许改 ConfigKeys.EDITABLE_KEYS 内的键） */
    @PutMapping("/admin/update")
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP}, mode = SaMode.OR)
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
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP}, mode = SaMode.OR)
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
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP}, mode = SaMode.OR)
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
