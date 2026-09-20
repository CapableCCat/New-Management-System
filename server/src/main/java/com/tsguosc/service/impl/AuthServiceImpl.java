package com.tsguosc.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tsguosc.common.constant.RedisKeys;
import com.tsguosc.common.constant.SessionKeys;
import com.tsguosc.common.exception.BusinessException;
import com.tsguosc.common.result.ResultCode;
import com.tsguosc.config.SecurityProperties;
import com.tsguosc.dto.CaptchaVO;
import com.tsguosc.dto.InitAdminRequest;
import com.tsguosc.dto.InitStatusVO;
import com.tsguosc.dto.LoginRequest;
import com.tsguosc.dto.LoginVO;
import com.tsguosc.dto.UserVO;
import com.tsguosc.entity.User;
import com.tsguosc.mapper.UserMapper;
import com.tsguosc.service.AuthService;
import com.tsguosc.util.PasswordPolicy;
import com.wf.captcha.ArithmeticCaptcha;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证与初始化实现。
 *
 * <p>验证码与登录失败计数都放 Redis：
 * <ul>
 *   <li>{@code osc:captcha:{key}} → 答案，TTL 2 分钟，校验后立即删除（一次性）</li>
 *   <li>{@code osc:login:fail:{phone}} → 失败次数，TTL = 锁定时长（15 分钟）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 超管编码（与 PRD §6.2 一致：0 普通成员 / 2 超管） */
    private static final int ROLE_SUPER_ADMIN = 2;
    /** 社长团 */
    private static final int DEPARTMENT_LEADER_GROUP = 0;
    /** 社长 */
    private static final int DUTY_PRESIDENT = 3;
    /** 账号状态：正常 */
    private static final int STATUS_NORMAL = 0;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final SecurityProperties securityProperties;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public CaptchaVO createCaptcha() {
        // 取一个"结果非负"的算术验证码：easy-captcha 的减法可能给出负结果（如 1-2），
        // 手机上输负号很别扭，所以重掷几次（重掷成本极低）
        ArithmeticCaptcha captcha;
        String answer;
        int attempts = 0;
        do {
            captcha = new ArithmeticCaptcha(130, 48);
            // 2 位数运算（如 3+4=?），手机上更好输入
            captcha.setLen(2);
            answer = captcha.text();
            attempts++;
        } while (answer != null && answer.trim().startsWith("-") && attempts < 8);

        String base64 = captcha.toBase64();
        if (!base64.startsWith("data:")) {
            base64 = "data:image/png;base64," + base64;
        }

        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                RedisKeys.CAPTCHA_PREFIX + captchaKey,
                answer,
                Duration.ofSeconds(securityProperties.getCaptchaTtlSeconds()));

        return new CaptchaVO(captchaKey, base64);
    }

    @Override
    public InitStatusVO initStatus() {
        Long count = userMapper.selectCount(null);
        return new InitStatusVO(count != null && count > 0);
    }

    @Override
    public void initAdmin(InitAdminRequest request) {
        if (initStatus().initialized()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "系统已初始化，无法重复创建超管");
        }
        String phone = request.phone().trim();
        passwordPolicy.validate(request.password(), phone, null);

        User admin = new User();
        admin.setPhone(phone);
        admin.setName(request.name().trim());
        admin.setPassword(passwordEncoder.encode(request.password()));
        admin.setRole(ROLE_SUPER_ADMIN);
        admin.setDepartment(DEPARTMENT_LEADER_GROUP);
        admin.setDuty(DUTY_PRESIDENT);
        admin.setStatus(STATUS_NORMAL);
        admin.setGender(0);
        // 引导页是本人当场设置的密码，直接标记为已激活，不再强制改密；
        // 由审核通过（T7）/ Excel 导入（T13）/ 管理员重置（F-002）创建的账号才用
        // 系统随机初始密码 + activated_at=NULL，登录后被强制改密
        admin.setActivatedAt(LocalDateTime.now());
        userMapper.insert(admin);

        log.info("系统初始化完成，已创建首个超管：id={}, phone={}", admin.getId(), phone);
    }

    @Override
    public LoginVO login(LoginRequest request) {
        validateCaptcha(request.captchaKey(), request.captchaCode());

        String phone = request.phone().trim();
        checkLocked(phone);

        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone));
        if (user == null) {
            // 按 PRD F-002 的文案区分"账号不存在"与"密码错误"
            throw new BusinessException(ResultCode.LOGIN_FAILED, "账号不存在，请确认手机号或先提交报名");
        }
        if (Objects.equals(user.getStatus(), 1)) {
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN);
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            int remaining = increaseFailAndGetRemaining(phone);
            if (remaining <= 0) {
                throw new BusinessException(ResultCode.ACCOUNT_LOCKED,
                        "尝试次数过多，账号已锁定 " + securityProperties.getLoginLockMinutes() + " 分钟");
            }
            throw new BusinessException(ResultCode.LOGIN_FAILED, "密码错误，还剩 " + remaining + " 次机会");
        }

        clearFail(phone);

        StpUtil.login(user.getId());
        boolean needChangePassword = user.getActivatedAt() == null;
        StpUtil.getSession().set(SessionKeys.NEED_CHANGE_PASSWORD, needChangePassword);

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        log.info("登录成功：userId={}, phone={}, needChangePassword={}",
                user.getId(), phone, needChangePassword);
        return new LoginVO(tokenInfo.getTokenName(), tokenInfo.getTokenValue(),
                needChangePassword, UserVO.from(user));
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    // ------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------

    /** 校验验证码：无论对错都立即作废（一次性，防重放） */
    private void validateCaptcha(String captchaKey, String captchaCode) {
        String redisKey = RedisKeys.CAPTCHA_PREFIX + captchaKey;
        String answer = stringRedisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(answer)) {
            throw new BusinessException(ResultCode.CAPTCHA_INVALID, "验证码已过期，请点击图片刷新");
        }
        stringRedisTemplate.delete(redisKey);
        if (!answer.equalsIgnoreCase(captchaCode.trim())) {
            throw new BusinessException(ResultCode.CAPTCHA_INVALID);
        }
    }

    private void checkLocked(String phone) {
        String key = RedisKeys.LOGIN_FAIL_PREFIX + phone;
        int fails = getFailCount(key);
        if (fails >= securityProperties.getLoginMaxFail()) {
            Long minutes = stringRedisTemplate.getExpire(key, TimeUnit.MINUTES);
            long remain = (minutes == null || minutes <= 0)
                    ? securityProperties.getLoginLockMinutes() : minutes;
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED,
                    "尝试次数过多，请 " + remain + " 分钟后重试");
        }
    }

    /** 失败次数 +1，返回剩余可尝试次数（<=0 表示已达锁定阈值） */
    private int increaseFailAndGetRemaining(String phone) {
        String key = RedisKeys.LOGIN_FAIL_PREFIX + phone;
        int max = securityProperties.getLoginMaxFail();
        Long count = stringRedisTemplate.opsForValue().increment(key);
        long current = count == null ? max : count;
        if (current == 1L) {
            // TTL 只在第一次失败时设置：15 分钟内的连续失败累计，不因每次失败而顺延
            stringRedisTemplate.expire(key, Duration.ofMinutes(securityProperties.getLoginLockMinutes()));
        }
        return (int) (max - current);
    }

    private int getFailCount(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("登录失败计数解析异常，已重置：key={}, value={}", key, value);
            stringRedisTemplate.delete(key);
            return 0;
        }
    }

    private void clearFail(String phone) {
        stringRedisTemplate.delete(RedisKeys.LOGIN_FAIL_PREFIX + phone);
    }
}
