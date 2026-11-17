package com.example.mywebsite.config;

import com.example.mywebsite.model.Permission;
import com.example.mywebsite.model.Role;
import com.example.mywebsite.model.RolePermission;
import com.example.mywebsite.repository.PermissionRepository;
import com.example.mywebsite.repository.RolePermissionRepository;
import com.example.mywebsite.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;

    @Override
    public void run(String... args) throws Exception {
        // Kiểm tra nếu chưa có Role nào thì mới tiến hành Seeding
        if (rolePermissionRepository.count() == 0) {

            // BẢN SỬA LỖI: Dọn sạch rác ở các bảng cũ trước khi thêm mới để tránh lỗi duplicate
            roleRepository.deleteAll();
            permissionRepository.deleteAll();
            rolePermissionRepository.deleteAll();

            // 1. Tạo Danh sách Role
            roleRepository.saveAll(Arrays.asList(
                    new Role("ADMIN"), new Role("SHOP_MANAGER"), new Role("EMPLOYEE")
            ));

            // 2. Tạo Danh sách Quyền cốt lõi
            permissionRepository.saveAll(Arrays.asList(
                    new Permission("USER_READ", "Xem danh sách User"),
                    new Permission("USER_WRITE", "Sửa User"),
                    new Permission("USER_DELETE", "Xóa User"),
                    new Permission("PRODUCT_READ", "Xem sản phẩm"),
                    new Permission("PRODUCT_WRITE", "Sửa sản phẩm"),
                    new Permission("PRODUCT_DELETE", "Xóa sản phẩm"),
                    new Permission("PRODUCT_EDIT_STOCK", "Sửa tồn kho")
            ));

            // 3. Rải từng dòng ánh xạ vào bảng trung gian (Chuẩn Relational)
            rolePermissionRepository.saveAll(Arrays.asList(
                    // Phân quyền cho ADMIN
                    new RolePermission("ADMIN", "USER_READ"),
                    new RolePermission("ADMIN", "USER_WRITE"),
                    new RolePermission("ADMIN", "USER_DELETE"),
                    new RolePermission("ADMIN", "PRODUCT_READ"),
                    new RolePermission("ADMIN", "PRODUCT_WRITE"),
                    new RolePermission("ADMIN", "PRODUCT_DELETE"),

                    // Phân quyền cho SHOP_MANAGER
                    new RolePermission("SHOP_MANAGER", "PRODUCT_READ"),
                    new RolePermission("SHOP_MANAGER", "PRODUCT_WRITE"),
                    new RolePermission("SHOP_MANAGER", "PRODUCT_DELETE"),

                    // Phân quyền cho EMPLOYEE
                    new RolePermission("EMPLOYEE", "PRODUCT_READ"),
                    new RolePermission("EMPLOYEE", "PRODUCT_EDIT_STOCK")
            ));

            System.out.println("✅ Đã dọn dẹp và thiết lập bảng Role-Permissions thành công!");
        }
    }
}