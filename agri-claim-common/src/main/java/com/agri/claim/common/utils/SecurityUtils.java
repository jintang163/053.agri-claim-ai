package com.agri.claim.common.utils;

import com.agri.claim.common.constant.Constants;
import com.agri.claim.common.core.model.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SecurityUtils {

    private static final String LOGIN_USER_ATTR = "LOGIN_USER";

    public static LoginUser getLoginUser() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        Object obj = request.getAttribute(LOGIN_USER_ATTR);
        if (obj instanceof LoginUser) {
            return (LoginUser) obj;
        }
        return null;
    }

    public static void setLoginUser(LoginUser loginUser) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.getRequest().setAttribute(LOGIN_USER_ATTR, loginUser);
        }
    }

    public static Long getUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    public static String getUserName() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserName() : "system";
    }

    public static Long getDeptId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getDeptId() : null;
    }

    public static String getRoleKey() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getRoleKey() : null;
    }

    public static boolean isAdmin() {
        String roleKey = getRoleKey();
        return Constants.SUPER_ADMIN_ROLE.equals(roleKey);
    }

    public static String getToken(HttpServletRequest request) {
        String token = request.getHeader(Constants.TOKEN_HEADER);
        if (token != null && token.startsWith(Constants.TOKEN_PREFIX)) {
            return token.substring(Constants.TOKEN_PREFIX.length());
        }
        return token;
    }
}
