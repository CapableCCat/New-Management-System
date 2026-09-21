package com.tsguosc.common.constant;

import java.util.Set;

/**
 * 系统配置键（sys_config.config_key）。
 */
public final class ConfigKeys {

    /** 系统访问地址（短信模板变量 {系统链接} 用） */
    public static final String SYSTEM_URL = "system_url";

    /** 审核通过短信模板 */
    public static final String SMS_TEMPLATE_PASS = "sms_template_pass";

    /** 审核拒绝短信模板 */
    public static final String SMS_TEMPLATE_REJECT = "sms_template_reject";

    /** 社团简介（报名页顶部展示，支持多段，用空行分段） */
    public static final String CLUB_INTRO = "club_intro";

    /** 审核时效文案（报名成功页展示，如"我们会在 3 个工作日内完成审核"） */
    public static final String REVIEW_NOTICE = "review_notice";

    /** 报名开关：1 开放 / 0 关闭（关闭后报名页只展示结束提示） */
    public static final String RECRUIT_OPEN = "recruit_open";

    /** 允许超管在后台编辑的键（其余键需走运维流程，避免误改） */
    public static final Set<String> EDITABLE_KEYS =
            Set.of(SYSTEM_URL, SMS_TEMPLATE_PASS, SMS_TEMPLATE_REJECT,
                    CLUB_INTRO, REVIEW_NOTICE, RECRUIT_OPEN);

    /** 报名页需要公开读取的键 */
    public static final Set<String> RECRUIT_PUBLIC_KEYS = Set.of(CLUB_INTRO, REVIEW_NOTICE, RECRUIT_OPEN);

    private ConfigKeys() {
    }
}
