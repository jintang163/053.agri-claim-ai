package com.agri.claim.auth.controller;

import com.agri.claim.auth.dto.LoginDTO;
import com.agri.claim.auth.service.AuthService;
import com.agri.claim.auth.vo.CaptchaVO;
import com.agri.claim.auth.vo.LoginVO;
import com.agri.claim.common.constant.Constants;
import com.agri.claim.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证管理", description = "登录、登出、验证码")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public R<CaptchaVO> captcha() {
        return R.ok(authService.generateCaptcha());
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok("登录成功", authService.login(dto));
    }

    @Operation(summary = "用户登出")
    @DeleteMapping("/logout")
    public R<Void> logout(@RequestHeader(value = Constants.USER_ID, required = false) Long userId) {
        authService.logout(userId);
        return R.ok("登出成功");
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(
            @RequestHeader(value = Constants.USER_ID) Long userId,
            @RequestHeader(value = Constants.USER_NAME, required = false) String userName) {
        return R.ok("刷新成功", authService.refreshToken(userId, userName));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public R<LoginVO> info(@RequestHeader(Constants.USER_ID) Long userId) {
        return R.ok(authService.getUserInfo(userId));
    }
}
