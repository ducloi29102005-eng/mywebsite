package com.example.mywebsite.config;

import com.example.mywebsite.model.User;
import com.example.mywebsite.model.UserData;
import com.example.mywebsite.repository.UserRepository;
import com.example.mywebsite.repository.UserDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDataRepository userDataRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        User currentUser = null;

        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            String googleEmail = oauthUser.getAttribute("email");

            // ==========================================
            // 1. LUỒNG LIÊN KẾT GOOGLE (Dùng linkingUserId)
            // ==========================================
            String linkingUserId = (String) request.getSession().getAttribute("linkingUserId");
            if (linkingUserId != null) {

                // Kiểm tra 1: Gmail này đã bị ai đó lấy chưa?
                Optional<User> ownerOfGmail = userRepository.findByGmail(googleEmail);

                // Nếu đã có người dùng Gmail này VÀ người đó không phải là tài khoản hiện tại -> LỖI TRÙNG LẶP
                if (ownerOfGmail.isPresent() && !ownerOfGmail.get().getUserId().equals(linkingUserId)) {
                    request.getSession().removeAttribute("linkingUserId");
                    response.sendRedirect("/profile/security?error=email_exists");
                    return;
                }

                // Tiến hành liên kết: Cập nhật cho cả User và UserData dựa vào userId
                Optional<User> userOpt = userRepository.findByUserId(linkingUserId);
                if (userOpt.isPresent()) {
                    User u = userOpt.get();
                    u.setGmail(googleEmail);
                    userRepository.save(u);

                    UserData data = userDataRepository.findByUserId(linkingUserId).orElse(new UserData());
                    data.setUserId(linkingUserId);
                    data.setGmail(googleEmail);
                    userDataRepository.save(data);
                }

                request.getSession().removeAttribute("linkingUserId");
                response.sendRedirect("/profile/security?linkSuccess");
                return;
            }

            // ==========================================
            // 2. LUỒNG ĐĂNG NHẬP GOOGLE TRỰC TIẾP
            // ==========================================
            Optional<User> existingUser = userRepository.findByGmail(googleEmail);

            if (existingUser.isPresent()) {
                // Đã có tài khoản liên kết Gmail này -> Lấy tài khoản đó làm currentUser
                currentUser = existingUser.get();
            } else {
                // Lần đầu tiên đăng nhập Google VÀ chưa ai dùng Gmail này -> Tạo tài khoản hoàn toàn mới
                User newUser = new User();
                String newUserId = UUID.randomUUID().toString(); // Tự sinh userId
                newUser.setUserId(newUserId);
                newUser.setUsername(googleEmail);
                newUser.setGmail(googleEmail);
                newUser.setPassword("");
                newUser.setRole("USER");
                newUser.setStatus("ACTIVE");
                currentUser = userRepository.save(newUser);

                UserData newData = new UserData();
                newData.setUserId(newUserId);
                newData.setGmail(googleEmail);
                userDataRepository.save(newData);
            }
        }

        // ==========================================
        // 3. XÁC ĐỊNH CURRENT USER & PHÂN LUỒNG ADMIN/USER
        // ==========================================
        if (currentUser == null) {
            String username = authentication.getName();
            currentUser = userRepository.findByUsername(username).orElse(null);
        }

        if (currentUser != null) {
            if ("BANNED".equalsIgnoreCase(currentUser.getStatus())) {
                request.getSession().invalidate();
                response.sendRedirect("/banned?username=" + java.net.URLEncoder.encode(currentUser.getUsername(), java.nio.charset.StandardCharsets.UTF_8));
                return;
            }

            if ("ADMIN".equals(currentUser.getRole())) {
                response.sendRedirect("/admin/dashboard");
            } else {
                response.sendRedirect("/profile");
            }
        } else {
            response.sendRedirect("/login");
        }
    }
}