package com.example.mywebsite.config;

import com.example.mywebsite.model.User;
import com.example.mywebsite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user != null) {
            // 1. Nếu bị khóa -> chuyển hướng sang trang hiển thị chữ BAN
            if ("BANNED".equalsIgnoreCase(user.getStatus())) {
                response.sendRedirect("/banned");
                return;
            }

            // 2. Nếu là ADMIN -> vào thẳng Dashboard
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                response.sendRedirect("/admin/dashboard");
                return;
            }
        }

        // 3. Người dùng thông thường -> vào Profile
        response.sendRedirect("/profile");
    }
}