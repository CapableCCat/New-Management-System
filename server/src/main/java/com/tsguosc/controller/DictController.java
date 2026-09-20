package com.tsguosc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tsguosc.common.constant.Roles;
import com.tsguosc.common.result.Result;
import com.tsguosc.dto.DictCreateRequest;
import com.tsguosc.dto.DictTypeVO;
import com.tsguosc.dto.DictUpdateRequest;
import com.tsguosc.dto.DictVO;
import com.tsguosc.service.DictService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典接口。
 *
 * <p>路径刻意分两段与三段：
 * <ul>
 *   <li>{@code /dict/types}、{@code /dict/{type}}（两段）→ 公开读，在拦截器白名单里（报名页/查询页要用）</li>
 *   <li>{@code /dict/admin/xxx}（三段）→ 不在白名单，天然需要登录，再由 @SaCheckRole 限超管</li>
 * </ul>
 */
@RestController
@RequestMapping("/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    /** 字典类型元数据（公开） */
    @GetMapping("/types")
    public Result<List<DictTypeVO>> types() {
        return Result.ok(dictService.types());
    }

    /** 某类型下启用的条目（公开：只返回 code + label 等非敏感信息） */
    @GetMapping("/{type}")
    public Result<List<DictVO>> listEnabled(@PathVariable String type) {
        return Result.ok(dictService.listEnabled(type));
    }

    /** 管理端：某类型下全部条目（含停用） */
    @GetMapping("/admin/list")
    @SaCheckRole(Roles.SUPER_ADMIN)
    public Result<List<DictVO>> listAll(@RequestParam String type) {
        return Result.ok(dictService.listAll(type));
    }

    /** 管理端：新增条目 */
    @PostMapping("/admin/create")
    @SaCheckRole(Roles.SUPER_ADMIN)
    public Result<Void> create(@Valid @RequestBody DictCreateRequest request) {
        dictService.create(request);
        return Result.ok(null, "新增成功");
    }

    /** 管理端：编辑条目（编码不可改） */
    @PutMapping("/admin/{id}")
    @SaCheckRole(Roles.SUPER_ADMIN)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DictUpdateRequest request) {
        dictService.update(id, request);
        return Result.ok(null, "保存成功");
    }

    /** 管理端：上移 / 下移 */
    @PutMapping("/admin/{id}/move")
    @SaCheckRole(Roles.SUPER_ADMIN)
    public Result<Void> move(@PathVariable Long id, @RequestParam String direction) {
        dictService.move(id, direction);
        return Result.ok(null, "排序已更新");
    }
}
