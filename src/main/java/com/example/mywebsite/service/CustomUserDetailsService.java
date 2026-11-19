package com.example.mywebsite.service;

import com.example.mywebsite.model.RolePermission;
import com.example.mywebsite.model.User;
import com.example.mywebsite.repository.RolePermissionRepository;
import com.example.mywebsite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository; // Thêm Repository để lấy quyền

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Tìm user trong database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();

        // 2. Lấy role của User (mặc định là USER nếu null)
        String roleName = user.getRole();
        if (roleName == null || roleName.trim().isEmpty()) {
            roleName = "USER";
        }

        // 3. Cấp Role cho Spring Security nhận diện (Thêm cả prefix ROLE_ để khớp với cấu hình SecurityConfig)
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
        authorities.add(new SimpleGrantedAuthority(roleName));

        // 4. Lấy chi tiết các quyền (Permissions) của Role này từ Database và nạp vào
        List<RolePermission> permissions = rolePermissionRepository.findByRoleName(roleName);
        for (RolePermission rp : permissions) {
            authorities.add(new SimpleGrantedAuthority(rp.getPermissionName())); // Ví dụ: PRODUCT_READ, PRODUCT_WRITE
        }

        // 5. Trả về đối tượng User của Spring Security với ĐẦY ĐỦ QUYỀN
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
}