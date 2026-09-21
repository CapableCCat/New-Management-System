package com.tsguosc.dto;

/**
 * 公开报名页所需的配置信息（F-001）。
 *
 * @param open         报名是否开放（recruit_open = 1）
 * @param clubIntro    社团简介（多段，用空行分段）
 * @param reviewNotice 审核时效文案
 */
public record RecruitInfoVO(boolean open, String clubIntro, String reviewNotice) {
}
