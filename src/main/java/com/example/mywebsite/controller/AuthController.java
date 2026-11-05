package com.example.mywebsite.controller;

import com.example.mywebsite.model.User;
import com.example.mywebsite.repository.UserRepository;
import com.example.mywebsite.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;

@Controller
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user) {
        System.out.println("====== NHẬN DỮ LIỆU ĐĂNG KÝ ======");
        System.out.println("Username: " + user.getUsername());
        System.out.println("Password: " + user.getPassword());
        userService.registerUser(user);
        System.out.println("====== LƯU USER THÀNH CÔNG ======");
        return "redirect:/login?registered";
    }
    @GetMapping("/banned")
    public String showBannedPage(Principal principal, Model model) {
        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                model.addAttribute("username", user.getUsername());

                // Lấy lý do bị khóa từ database (kiểm tra getter tương ứng trong User entity của bạn)
                String reason = user.getBanReason();
                if (reason == null || reason.trim().isEmpty()) {
                    reason = "Tài khoản của bạn đã bị khóa do vi phạm tiêu chuẩn cộng đồng.";
                }
                model.addAttribute("reason", reason);
            }
        }
        return "banned";
    }
}
