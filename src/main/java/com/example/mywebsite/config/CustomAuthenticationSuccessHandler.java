package com.example.mywebsite.config;

import com.example.mywebsite.model.User;
import com.example.mywebsite.repository.UserDataRepository;
import com.example.mywebsite.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.security.core.GrantedAuthority; // BỔ SUNG THƯ VIỆN NÀY

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDataRepository userDataRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        HttpSession session = request.getSession();
        String linkingUserId = (String) session.getAttribute("linkingUserId");

        // ====================================================================
        // TRƯỜNG HỢP 1: ĐANG Ở TRONG TÀI KHOẢN VÀ BẤM LIÊN KẾT GMAIL
        // ====================================================================
        if (authentication instanceof OAuth2AuthenticationToken && linkingUserId != null) {
            session.removeAttribute("linkingUserId");

            OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            String email = oauth2User.getAttribute("email");

            // 1. Lấy thông tin tài khoản gốc đang đăng nhập (ví dụ: vumanhhuong)
            User originalUser = userRepository.findByUserId(linkingUserId).orElse(null);

            // 2. GIỮ NGUYÊN PHIÊN ĐĂNG NHẬP GỐC: Ép Spring Security giữ nick hiện tại, không đổi sang nick Google
            if (originalUser != null) {
                String role = originalUser.getRole() != null ? originalUser.getRole() : "USER";
                UsernamePasswordAuthenticationToken resetAuth = new UsernamePasswordAuthenticationToken(
                        originalUser.getUsername(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(resetAuth);
                SecurityContextHolder.setContext(context);
                new HttpSessionSecurityContextRepository().saveContext(context, request, response);
            }

            // 3. KIỂM TRA TRÙNG LẶP: Xem Gmail này đã có tài khoản KHÁC liên kết chưa
            Optional<User> userWithThisGmail = userRepository.findByGmail(email);
            if (userWithThisGmail.isPresent() && !userWithThisGmail.get().getUserId().equals(linkingUserId)) {
                // Gmail đã thuộc về người khác -> Chặn lại và báo đỏ
                response.sendRedirect("/profile/security?linkError=duplicate");
                return;
            }

            // 4. Nếu chưa ai liên kết -> Lưu Gmail vào tài khoản hiện tại
            if (originalUser != null) {
                originalUser.setGmail(email);
                userRepository.save(originalUser);

                userDataRepository.findByUserId(linkingUserId).ifPresent(userData -> {
                    userData.setGmail(email);
                    userDataRepository.save(userData);
                });
            }

            response.sendRedirect("/profile/security?linkSuccess");
            return;
        }

        // ====================================================================
        // TRƯỜNG HỢP 2: ĐĂNG NHẬP THƯỜNG (FORM LOGIN HOẶC GOOGLE LOGIN)
        // ====================================================================
        User currentUser = null;

        if (authentication instanceof OAuth2AuthenticationToken) {
            String email = ((OAuth2AuthenticationToken) authentication).getPrincipal().getAttribute("email");
            currentUser = userRepository.findByGmail(email).orElse(null);
        } else {
            String username = authentication.getName();
            currentUser = userRepository.findByUsername(username).orElse(null);
        }

        if (currentUser != null) {
            if ("BANNED".equalsIgnoreCase(currentUser.getStatus())) {
                request.getSession().invalidate();
                response.sendRedirect("/banned?username=" + URLEncoder.encode(currentUser.getUsername(), StandardCharsets.UTF_8));
                return;
            }

            // --- [ĐIỂM SỬA LỖI QUAN TRỌNG NHẤT] ---
            // ĐỒNG BỘ QUYỀN TỪ MONGODB VÀO SPRING SECURITY
            String dbRole = currentUser.getRole() != null ? currentUser.getRole() : "USER";
            GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + dbRole);

            Authentication newAuth;
            if (authentication instanceof OAuth2AuthenticationToken) {
                newAuth = new OAuth2AuthenticationToken(
                        ((OAuth2AuthenticationToken) authentication).getPrincipal(),
                        Collections.singletonList(authority),
                        ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId()
                );
            } else {
                newAuth = new UsernamePasswordAuthenticationToken(
                        authentication.getPrincipal(),
                        authentication.getCredentials(),
                        Collections.singletonList(authority)
                );
            }

            // Xóa phiên cũ, nạp phiên mới mang theo Role chuẩn từ DB
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(newAuth);
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);

            // Cập nhật lại biến authentication để code chuyển hướng bên dưới đọc đúng
            authentication = newAuth;
            // --------------------------------------

        } else {
            response.sendRedirect("/login?error");
            return;
        }

        // CHUYỂN HƯỚNG THEO ROLE (Sẽ hoạt động chính xác 100%)
        String redirectUrl = "/";

        for (GrantedAuthority auth : authentication.getAuthorities()) {
            String role = auth.getAuthority();
            if (role.equals("ADMIN") || role.equals("ROLE_ADMIN")) {
                redirectUrl = "/admin/dashboard";
                break;
            } else if (role.equals("SHOP_MANAGER") || role.equals("ROLE_SHOP_MANAGER") ||
                    role.equals("EMPLOYEE") || role.equals("ROLE_EMPLOYEE")) {
                redirectUrl = "/admin/products";
                break;
            }
        }

        response.sendRedirect(redirectUrl);
    }
    /**
     * HÀM BỔ SUNG: Cấp phát danh sách các quyền (Permissions) dựa vào Role của hệ thống
     */
    private List<String> getPermissionsForRole(String role) {
        if ("ADMIN".equals(role)) {
            // ADMIN: Toàn quyền thao tác trên người dùng và sản phẩm
            return Arrays.asList("USER_READ", "USER_WRITE", "USER_DELETE", "PRODUCT_READ", "PRODUCT_WRITE", "PRODUCT_DELETE");
        } else if ("SHOP_MANAGER".equals(role)) {
            // QUẢN LÝ CỬA HÀNG: Toàn quyền thao tác với sản phẩm
            return Arrays.asList("PRODUCT_READ", "PRODUCT_WRITE", "PRODUCT_DELETE");
        } else if ("EMPLOYEE".equals(role)) {
            // NHÂN VIÊN KHO: Chỉ xem sản phẩm và cập nhật số lượng kho
            return Arrays.asList("PRODUCT_READ", "PRODUCT_EDIT_STOCK");
        }
        // USER mặc định không có quyền quản trị
        return new ArrayList<>();
    }
}
