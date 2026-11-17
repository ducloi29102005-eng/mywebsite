package com.example.mywebsite.config;

import com.example.mywebsite.model.RolePermission;
import com.example.mywebsite.model.User;
import com.example.mywebsite.repository.RolePermissionRepository;
import com.example.mywebsite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Autowired
    private UserRepository userRepository;

    // THAY ĐỔI: Kéo RolePermissionRepository (Bảng trung gian) vào thay vì RoleRepository
    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if ((auth == null) || (targetDomainObject == null) || !(permission instanceof String)) return false;
        return checkPrivilege(auth, targetDomainObject.toString().toUpperCase(), permission.toString().toUpperCase());
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        if ((auth == null) || (targetType == null) || !(permission instanceof String)) return false;
        return checkPrivilege(auth, targetType.toUpperCase(), permission.toString().toUpperCase());
    }

    private boolean checkPrivilege(Authentication auth, String targetType, String permission) {
        String identifier = "";
        User user = null;

        // 1. SỬA LỖI LẤY ĐỊNH DANH (Đảm bảo lấy đúng username dạng chuỗi)
        if (auth instanceof OAuth2AuthenticationToken) {
            identifier = (String) ((OAuth2User) auth.getPrincipal()).getAttribute("email");
            user = userRepository.findByGmail(identifier).orElse(null);
        } else {
            // Trích xuất chuẩn xác Username từ UserDetails của Spring Security
            if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                identifier = ((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername();
            } else {
                identifier = auth.getName();
            }
            user = userRepository.findByUsername(identifier).orElse(null);
        }

        // Bắt lỗi 1: Không tìm thấy User
        if (user == null || user.getRole() == null) {
            System.out.println("❌ ACL TỪ CHỐI: Không tìm thấy User trong DB, hoặc User không có Role. Identifier: " + identifier);
            return false;
        }

        // 2. SỬA LỖI TIỀN TỐ: Tự động cắt bỏ chữ "ROLE_" nếu có trong DB để khớp với bảng trung gian
        String dbRoleName = user.getRole().toUpperCase().replace("ROLE_", "");
        String requiredPermission = targetType + "_" + permission;

        // 3. Tìm trong bảng trung gian
        List<RolePermission> mappings = rolePermissionRepository.findByRoleName(dbRoleName);

        // Bắt lỗi 2: Bảng phân quyền trống
        if (mappings == null || mappings.isEmpty()) {
            System.out.println("❌ ACL TỪ CHỐI: Role [" + dbRoleName + "] chưa được cấp bất kỳ quyền nào trong bảng role_permissions (Seeder chưa chạy).");
            return false;
        }

        // 4. Quét quyền
        for (RolePermission mapping : mappings) {
            if (mapping.getPermissionName().equalsIgnoreCase(requiredPermission)) {
                System.out.println("✅ ACL CHẤP NHẬN: Role [" + dbRoleName + "] được phép thực thi: " + requiredPermission);
                return true; // Cấp quyền thành công
            }
        }

        // Bắt lỗi 3: Có quyền nhưng không khớp
        System.out.println("❌ ACL TỪ CHỐI: Role [" + dbRoleName + "] KHÔNG CÓ chìa khóa để mở: " + requiredPermission);
        return false;
    }
}