package com.example.mywebsite.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "role_permissions")
public class RolePermission {
    @Id
    private String id;

    private String roleName;       // Cột 1: Tên Role (VD: ADMIN)
    private String permissionName; // Cột 2: Tên Quyền (VD: USER_READ)

    public RolePermission() {}

    public RolePermission(String roleName, String permissionName) {
        this.roleName = roleName;
        this.permissionName = permissionName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getPermissionName() { return permissionName; }
    public void setPermissionName(String permissionName) { this.permissionName = permissionName; }
}