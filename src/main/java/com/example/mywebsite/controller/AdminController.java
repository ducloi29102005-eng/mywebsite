package com.example.mywebsite.controller;

import com.example.mywebsite.model.User;
import com.example.mywebsite.service.UserService;
import com.example.mywebsite.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    // 1. Vào thẳng Dashboard hiển thị danh sách người dùng ngay lập tức
    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        List<User> userList = userService.getAllUsers();
        model.addAttribute("users", userList);
        // Lấy thông tin tài khoản đang thao tác
        if (principal != null) {
            userRepository.findByUsername(principal.getName()).ifPresent(currentUser -> {
                model.addAttribute("currentUser", currentUser);
            });
        }
        return "admin-dashboard";
    }

    // 2. Chuyển hướng tự động sang dashboard nếu truy cập /admin hoặc /admin/login
    @GetMapping({"", "/", "/login"})
    public String redirectToDashboard() {
        return "redirect:/admin/dashboard";
    }

    // 3. Xử lý đổi vai trò (ADMIN <-> USER)
    @PostMapping("/users/toggle-role")
    public String toggleRole(@RequestParam String userId) {
        userService.toggleAdminRole(userId);
        return "redirect:/admin/dashboard";
    }

    // 4. Xử lý Khóa / Mở khóa tài khoản (Đã sửa khớp với form toggle-ban và hàm toggleBanStatus)
    @PostMapping("/users/toggle-ban")
    public String toggleBan(@RequestParam String userId,
                            @RequestParam(required = false) String reason) {
        userService.toggleBanStatus(userId, reason);
        return "redirect:/admin/dashboard";
    }

    // 5. Xử lý Xóa tài khoản
    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam String userId) {
        userService.deleteUser(userId);
        return "redirect:/admin/dashboard";
    }
}