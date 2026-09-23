package com.tsguosc.service;

import com.tsguosc.dto.ChangePasswordRequest;
import com.tsguosc.dto.ProfileUpdateRequest;
import com.tsguosc.dto.StudentIdUpdateRequest;
import com.tsguosc.dto.UserVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 当前用户相关（F-002 首登改密 / F-007 个人中心 / F-006 成员档案 都会用到）。
 *
 * <p>边界：**姓名与手机号不可自助修改**（需管理员在成员档案里改，见 T10）；
 * 学号仅在为空时允许自助补录（§6 D77）。
 */
public interface UserService {

    /** 当前登录用户信息 */
    UserVO current();

    /** 修改密码：校验旧密码 → 校验新密码强度 → 更新 → 强制登出 */
    void changePassword(ChangePasswordRequest request);

    /** 编辑自己的资料：学院 / 专业 / 性别 / 生源地 / 个人简介 */
    UserVO updateProfile(ProfileUpdateRequest request);

    /** 学号自助补录（仅当当前学号为空） */
    UserVO updateStudentId(StudentIdUpdateRequest request);

    /** 上传头像（jpg/png，≤2MB），返回更新后的当前用户 */
    UserVO uploadAvatar(MultipartFile file);
}
