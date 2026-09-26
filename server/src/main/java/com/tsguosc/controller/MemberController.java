package com.tsguosc.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.tsguosc.common.constant.Roles;
import com.tsguosc.common.result.Result;
import com.tsguosc.dto.ImportResultVO;
import com.tsguosc.dto.MemberQuery;
import com.tsguosc.dto.MemberUpdateRequest;
import com.tsguosc.dto.PageResult;
import com.tsguosc.dto.UserVO;
import com.tsguosc.service.MemberImportService;
import com.tsguosc.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 成员档案（F-006）+ Excel 批量导入（F-009）—— 成员端「成员展板」与管理端「成员档案」共用。
 *
 * <p>权限分层：
 * <ul>
 *   <li>{@code /member/list}、{@code /member/{id}}：登录即可（两段路径不在白名单内），
 *       行范围与列范围由 service 按角色裁剪 —— 普通成员只拿基础列</li>
 *   <li>{@code /member/admin/update}：限超管 / 社长团 / 部长（三段路径 + 类内注解）</li>
 *   <li>{@code /member/admin/import**}：**只限超管 / 社长团** —— PRD 权限矩阵里「Excel 导入」
 *       没有给部长（与成员档案编辑不同，别顺手放宽）</li>
 * </ul>
 */
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberImportService memberImportService;

    /** 成员列表（分页 + 多条件检索；行范围与列范围按当前角色裁剪） */
    @GetMapping("/list")
    public Result<PageResult<UserVO>> list(MemberQuery query) {
        return Result.ok(memberService.list(query));
    }

    /** 成员详情（越出行范围返回 40300） */
    @GetMapping("/{id:\\d+}")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.ok(memberService.detail(id));
    }

    /** 编辑成员档案（部门/职位/状态/学号/手机号等；唯一性校验在服务层） */
    @PutMapping("/admin/update")
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP, Roles.MINISTER}, mode = SaMode.OR)
    public Result<Void> update(@Valid @RequestBody MemberUpdateRequest request) {
        memberService.update(request);
        return Result.ok(null, "保存成功");
    }

    /** 下载 Excel 导入模板（7 列列头 + 填写说明 sheet） */
    @GetMapping("/admin/import-template")
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP}, mode = SaMode.OR)
    public void importTemplate(HttpServletResponse response) throws IOException {
        byte[] data = memberImportService.buildTemplate();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // 文件名用纯 ASCII，避免不同浏览器对非 ASCII filename 的编码差异
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"member-import-template.xlsx\"");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }

    /** 批量导入成员（逐行校验 + 建号；返回成功清单与错误行清单） */
    @PostMapping("/admin/import")
    @SaCheckRole(value = {Roles.SUPER_ADMIN, Roles.LEADER_GROUP}, mode = SaMode.OR)
    public Result<ImportResultVO> importMembers(@RequestParam("file") MultipartFile file) {
        return Result.ok(memberImportService.importMembers(file), "导入完成");
    }
}
