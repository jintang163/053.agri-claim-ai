package com.agri.claim.system.entity;

import com.agri.claim.common.core.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String userName;
    private String nickName;
    private String password;
    private Long deptId;
    private String email;
    private String phone;
    private String gender;
    private String avatar;
    private Integer status;
    private String remark;
    private LocalDateTime loginTime;
    private String loginIp;
}
