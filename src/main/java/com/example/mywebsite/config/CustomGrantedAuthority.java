package com.example.mywebsite.config;

import org.springframework.security.core.GrantedAuthority;
import java.util.List;

public class CustomGrantedAuthority implements GrantedAuthority {
    private String role;
    private List<String> permissions;

    public CustomGrantedAuthority(String role, List<String> permissions) {
        this.role = role;
        this.permissions = permissions;
    }

    @Override
    public String getAuthority() {
        return this.role;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}