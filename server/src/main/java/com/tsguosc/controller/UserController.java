package com.tsguosc.controller;

import com.tsguosc.common.result.Result;
import com.tsguosc.dto.ChangePasswordRequest;
import com.tsguosc.dto.ProfileUpdateRequest;
import com.tsguosc.dto.StudentIdUpdateRequest;
import com.tsguosc.dto.UserVO;
import com.tsguosc.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 当前用户接口（需登录）—— 个人中心 F-007。
 *
 * <p>**姓名与手机号没有对应接口**：按 PRD 需联系管理员，由干部走成员档案（T10）修改。
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 当前登录用户信息（不含密码） */
    @GetMapping("/current")
    public Result<UserVO> current() {
        return Result.ok(userService.current());
    }

    /** 修改密码：成功后强制登出，需用新密码重新登录 */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return Result.ok(null, "密码修改成功，请重新登录");
    }

    /** 编辑自己的资料：学院 / 专业 / 性别 / 生源地 / 个人简介（不含姓名、手机号） */
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.ok(userService.updateProfile(request), "保存成功");
    }

    /** 学号自助补录（仅当学号为空；已被占用则提示冲突） */
    @PutMapping("/student-id")
    public Result<UserVO> updateStudentId(@Valid @RequestBody StudentIdUpdateRequest request) {
        return Result.ok(userService.updateStudentId(request), "学号已保存");
    }

    /** 上传头像（jpg / png，≤2MB）；返回更新后的当前用户 */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(userService.uploadAvatar(file), "头像已更新");
    }
}
