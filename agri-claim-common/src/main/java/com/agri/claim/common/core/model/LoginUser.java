package com.agri.claim.common.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String userName;
    private String nickName;
    private Long deptId;
    private String deptName;
    private String roleKey;
    private String roleName;
    private String avatar;
    private String phone;
    private Set<String> permissions;
    private String token;
    private Long expireTime;
    private String loginIp;
    private String loginLocation;
}
