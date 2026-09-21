package com.tsguosc.dto;

/**
 * 短信通知提效工具所需配置（F-004）。
 *
 * <p>该工具的使用者是审核台的干部（超管 / 社长团 / 部长），而模板与系统链接的读取接口
 * `/config/admin/list` 仅限超管 —— 故在本接口下只读返回这三个键，不暴露其他配置。
 * 模板的**修改**仍然只走超管的「纳新设置」页，保证口径统一。
 *
 * @param systemUrl      系统访问地址（短信模板变量 {系统链接} 的取值）
 * @param passTemplate   审核通过通知模板（变量：{姓名} {系统链接} {初始密码}）
 * @param rejectTemplate 审核拒绝通知模板（变量：{姓名} {系统链接} {拒绝原因}）
 */
public record RecruitSmsConfigVO(String systemUrl, String passTemplate, String rejectTemplate) {
}
