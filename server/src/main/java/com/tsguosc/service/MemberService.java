package com.tsguosc.service;

import com.tsguosc.dto.MemberQuery;
import com.tsguosc.dto.MemberUpdateRequest;
import com.tsguosc.dto.PageResult;
import com.tsguosc.dto.UserVO;

/**
 * 成员档案管理（F-006）+ 成员端成员列表。
 *
 * <p>两条范围规则都在后端落地（前端只控制可见性）：
 * <ul>
 *   <li>行范围：部长=本部门；社长团/超管=全部；普通成员查看时不受行限制（成员展板）</li>
 *   <li>列范围：普通成员只拿基础列（手机号 / 学号被抹掉）</li>
 * </ul>
 */
public interface MemberService {

    /** 成员列表：分页 + 多条件检索，行范围与列范围按当前角色裁剪 */
    PageResult<UserVO> list(MemberQuery query);

    /** 成员详情（越出行范围抛 40300） */
    UserVO detail(Long id);

    /** 编辑成员档案：行范围 + 字段权限 + 手机号/学号唯一性校验 */
    void update(MemberUpdateRequest request);
}
