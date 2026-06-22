package com.agri.claim.auth.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.agri.claim.auth.dto.LoginDTO;
import com.agri.claim.auth.vo.CaptchaVO;
import com.agri.claim.auth.vo.LoginVO;
import com.agri.claim.common.constant.Constants;
import com.agri.claim.common.core.model.LoginUser;
import com.agri.claim.common.exception.BusinessException;
import com.agri.claim.common.result.ResultCode;
import com.agri.claim.common.utils.JwtUtils;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final StringRedisTemplate stringRedisTemplate;

    private final Map<String, MockUser> mockUserDB = new ConcurrentHashMap<>() {{
        put("admin", new MockUser(1L, "admin", "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918",
                "系统管理员", 1L, "总公司", "admin", "超级管理员", "", "13800138000", Constants.ENABLE));
        put("surveyor", new MockUser(2L, "surveyor", "e3c652f0ba0b4801205814f8b6bc49662b01c5d3d6c98b523f43b3d4b2b8818",
                "张查勘", 2L, "北京分公司", "surveyor", "查勘员", "", "13900139000", Constants.ENABLE));
        put("manager", new MockUser(3L, "manager", "6b3a55e0261b0304143f805a24924d0c1c44524821305f31459df66cf6f65d3",
                "李经理", 2L, "北京分公司", "manager", "定损经理", "", "13700137000", Constants.ENABLE));
    }};

    public CaptchaVO generateCaptcha() {
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Properties props = new Properties();
        props.setProperty("kaptcha.border", "no");
        props.setProperty("kaptcha.textproducer.font.color", "black");
        props.setProperty("kaptcha.textproducer.char.space", "4");
        props.setProperty("kaptcha.image.width", "120");
        props.setProperty("kaptcha.image.height", "40");
        props.setProperty("kaptcha.textproducer.char.length", "4");
        kaptcha.setConfig(new Config(props));

        String code = RandomUtil.randomString(4);
        String key = IdUtil.fastSimpleUUID();

        BufferedImage image = kaptcha.createImage(code);
        String base64 = "";
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", baos);
            base64 = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("生成验证码失败", e);
        }

        stringRedisTemplate.opsForValue()
                .set(Constants.CAPTCHA_KEY + key, code, Constants.CAPTCHA_EXPIRE, TimeUnit.SECONDS);

        return CaptchaVO.builder().key(key).base64(base64).build();
    }

    public LoginVO login(LoginDTO dto) {
        if (StrUtil.isBlank(dto.getCaptchaKey()) || StrUtil.isBlank(dto.getCaptcha())) {
            // 验证码可选，非严格模式
        } else {
            String cached = stringRedisTemplate.opsForValue()
                    .getAndDelete(Constants.CAPTCHA_KEY + dto.getCaptchaKey());
            if (!dto.getCaptcha().equalsIgnoreCase(cached != null ? cached : "")) {
                throw new BusinessException(ResultCode.CAPTCHA_ERROR);
            }
        }

        MockUser mockUser = mockUserDB.get(dto.getUsername());
        if (mockUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        if (Constants.DISABLE.equals(mockUser.status)) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        String hashedInput = sha256(dto.getPassword());
        if (!mockUser.passwordHash.equals(hashedInput)) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        String token = JwtUtils.createToken(mockUser.userId, mockUser.userName,
                mockUser.deptId, mockUser.roleKey);

        LoginUser loginUser = LoginUser.builder()
                .userId(mockUser.userId)
                .userName(mockUser.userName)
                .nickName(mockUser.nickName)
                .deptId(mockUser.deptId)
                .deptName(mockUser.deptName)
                .roleKey(mockUser.roleKey)
                .roleName(mockUser.roleName)
                .avatar(mockUser.avatar)
                .phone(mockUser.phone)
                .permissions(getPermissions(mockUser.roleKey))
                .token(token)
                .expireTime(System.currentTimeMillis() + Constants.TOKEN_EXPIRE * 1000)
                .build();

        stringRedisTemplate.opsForValue()
                .set(Constants.REDIS_USER_KEY + mockUser.userId, token,
                        Constants.REDIS_USER_EXPIRE, TimeUnit.SECONDS);

        log.info("用户登录成功 | userId: {} | userName: {}", mockUser.userId, mockUser.userName);

        return LoginVO.builder()
                .token(token)
                .expireIn(Constants.TOKEN_EXPIRE)
                .userId(mockUser.userId)
                .userName(mockUser.userName)
                .nickName(mockUser.nickName)
                .avatar(mockUser.avatar)
                .deptId(mockUser.deptId)
                .deptName(mockUser.deptName)
                .roleKey(mockUser.roleKey)
                .roleName(mockUser.roleName)
                .permissions(loginUser.getPermissions())
                .build();
    }

    public void logout(Long userId) {
        if (userId != null) {
            stringRedisTemplate.delete(Constants.REDIS_USER_KEY + userId);
            log.info("用户登出 | userId: {}", userId);
        }
    }

    public LoginVO refreshToken(Long userId, String userName) {
        MockUser mockUser = mockUserDB.values().stream()
                .filter(u -> u.userId().equals(userId))
                .findFirst()
                .orElse(null);
        if (mockUser == null && userName != null) {
            mockUser = mockUserDB.get(userName);
        }
        if (mockUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        String newToken = JwtUtils.createToken(mockUser.userId(), mockUser.userName(),
                mockUser.deptId(), mockUser.roleKey());
        stringRedisTemplate.opsForValue()
                .set(Constants.REDIS_USER_KEY + mockUser.userId(), newToken,
                        Constants.REDIS_USER_EXPIRE, TimeUnit.SECONDS);

        log.info("令牌刷新成功 | userId: {} | userName: {}", mockUser.userId(), mockUser.userName());
        return LoginVO.builder()
                .token(newToken)
                .expireIn(Constants.TOKEN_EXPIRE)
                .userId(mockUser.userId())
                .userName(mockUser.userName())
                .nickName(mockUser.nickName())
                .avatar(mockUser.avatar())
                .deptId(mockUser.deptId())
                .deptName(mockUser.deptName())
                .roleKey(mockUser.roleKey())
                .roleName(mockUser.roleName())
                .permissions(getPermissions(mockUser.roleKey()))
                .build();
    }

    public LoginVO getUserInfo(Long userId) {
        MockUser mockUser = mockUserDB.values().stream()
                .filter(u -> u.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_EXIST));

        String token = stringRedisTemplate.opsForValue().get(Constants.REDIS_USER_KEY + userId);
        return LoginVO.builder()
                .token(token)
                .expireIn(Constants.TOKEN_EXPIRE)
                .userId(mockUser.userId())
                .userName(mockUser.userName())
                .nickName(mockUser.nickName())
                .avatar(mockUser.avatar())
                .deptId(mockUser.deptId())
                .deptName(mockUser.deptName())
                .roleKey(mockUser.roleKey())
                .roleName(mockUser.roleName())
                .permissions(getPermissions(mockUser.roleKey()))
                .build();
    }

    private Set<String> getPermissions(String roleKey) {
        return switch (roleKey) {
            case "admin" -> Set.of(
                    "system:user:list", "system:user:add", "system:user:edit", "system:user:delete",
                    "system:role:list", "system:role:add", "system:role:edit", "system:role:delete",
                    "system:menu:list", "system:dept:list",
                    "image:upload", "image:list", "image:download", "image:delete",
                    "ai:segment", "ai:detect", "ai:ndvi",
                    "assess:list", "assess:create", "assess:audit", "assess:report"
            );
            case "surveyor" -> Set.of(
                    "image:upload", "image:list", "image:download",
                    "ai:segment", "ai:detect", "ai:ndvi",
                    "assess:list", "assess:create"
            );
            case "manager" -> Set.of(
                    "image:list", "image:download",
                    "assess:list", "assess:audit", "assess:report"
            );
            default -> Set.of();
        };
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return input;
        }
    }

    public record MockUser(Long userId, String userName, String passwordHash, String nickName,
                            Long deptId, String deptName, String roleKey, String roleName,
                            String avatar, String phone, Integer status) {
    }
}
