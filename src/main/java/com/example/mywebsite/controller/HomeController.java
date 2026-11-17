package com.example.mywebsite.controller;

import com.example.mywebsite.model.User;
import com.example.mywebsite.model.UserProfile;
import com.example.mywebsite.repository.UserProfileRepository;
import com.example.mywebsite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller // Bắt buộc dùng @Controller, không dùng @RestController
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private String getUserId(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken) {
            String email = (String) ((org.springframework.security.oauth2.core.user.OAuth2User)
                    ((OAuth2AuthenticationToken) principal).getPrincipal()).getAttribute("email");
            return userRepository.findByGmail(email).map(User::getUserId).orElse(null);
        } else {
            return userRepository.findByUsername(principal.getName()).map(User::getUserId).orElse(null);
        }
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        String userId = getUserId(principal);

        if (userId != null) {
            userRepository.findByUserId(userId).ifPresent(user -> {
                model.addAttribute("currentUser", user);
            });
            userProfileRepository.findByUserId(userId).ifPresent(profile -> {
                model.addAttribute("userProfile", profile);
            });
        }
        return "index"; // Trả về templates/index.html
    }
    @GetMapping("/403")
    public String accessDenied() {
        return "403"; // Trả về file 403.html (bạn có thể tự thiết kế file HTML này với chữ "Bạn không có quyền truy cập")
    }
}