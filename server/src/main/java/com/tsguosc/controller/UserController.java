package com.tsguosc.controller;

import com.tsguosc.common.result.Result;
import com.tsguosc.dto.ChangePasswordRequest;
import com.tsguosc.dto.UserVO;
import com.tsguosc.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户接口（需登录）。
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
}
