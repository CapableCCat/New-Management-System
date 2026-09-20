package com.tsguosc.common.constant;

/**
 * 角色名（Sa-Token 里的角色标识，由 {@code StpInterfaceImpl} 按 role / department / duty 推导）。
 *
 * <p>对应 PRD 第五章"角色资格推导"：超管 role=2、社长团 department=0、部长 duty=2，其余为普通成员。
 */
public final class Roles {

    /** 超管（系统级） */
    public static final String SUPER_ADMIN = "super-admin";

    /** 社长团（全社管理） */
    public static final String LEADER_GROUP = "leader-group";

    /** 部长（本部门管理） */
    public static final String MINISTER = "minister";

    /** 普通成员 */
    public static final String MEMBER = "member";

    private Roles() {
    }
}
