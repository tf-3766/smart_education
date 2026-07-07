package com.zhongruan.edu.biz.auth.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhongruan.edu.biz.shared.persistence.BaseAuditEntity;

@TableName("sys_permission")
public class PermissionEntity extends BaseAuditEntity {
    private String permissionCode;
    private String permissionName;
    private Integer enabled;

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}

