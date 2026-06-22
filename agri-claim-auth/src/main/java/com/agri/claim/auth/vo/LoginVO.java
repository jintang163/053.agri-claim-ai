package com.agri.claim.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    private String token;
    private Long expireIn;
    private Long userId;
    private String userName;
    private String nickName;
    private String avatar;
    private Long deptId;
    private String deptName;
    private String roleKey;
    private String roleName;
    private Set<String> permissions;
}
