package com.tsguosc.service;

import com.tsguosc.dto.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel 批量导入（PRD F-009）。
 *
 * <p>权限：PRD 权限矩阵「Excel 导入」只给**社长团 / 超管**（部长没有），
 * 判定在 Controller 的角色注解上，本层不重复推导角色。
 */
public interface MemberImportService {

    /**
     * 生成导入模板（xlsx 字节）。
     *
     * <p>模板含两个 sheet：`成员名单`（7 列列头，列名与校验用的完全同源）
     * 与 `填写说明`（每列怎么填、哪些必填）。**不放示例数据行**——
     * 免得有人不删示例就直接导入，凭空多出一个账号。
     */
    byte[] buildTemplate();

    /**
     * 解析上传的 Excel：逐行校验并建号。
     *
     * <p>列头不符直接拒绝整表（PRD：提示「请使用标准模板」）；
     * 明细行逐行独立处理，成功建号、失败记入错误清单。
     */
    ImportResultVO importMembers(MultipartFile file);
}
