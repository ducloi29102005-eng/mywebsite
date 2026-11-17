package com.example.mywebsite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    //1. Inject cái handler vừa tạo vào đây
    @Autowired
    private CustomAuthenticationSuccessHandler successHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Tắt CSRF để nhận form POST
                .csrf(csrf -> csrf.ignoringRequestMatchers("/ws-chat/**"))
                .authorizeHttpRequests(auth -> auth
                        // 1. Công khai cho khách
                        .requestMatchers(
                                "/",
                                "/shop",
                                "/register",
                                "/login",
                                "/banned",
                                "/forgot-password",
                                "/verify-otp",
                                "/reset-password",
                                "/ws-chat/**",
                                "/error",
                                "/403",
                                "/css/**",
                                "/images/**",
                                "/js/**"
                        ).permitAll()

                        // 2. PHÂN QUYỀN KHU VỰC QUẢN TRỊ (/admin/**)
                        // 2.1. Quản lý tài khoản: CHỈ ADMIN được phép truy cập
                        .requestMatchers("/admin/dashboard", "/admin/users/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                        // 2.2. Xóa sản phẩm: CHỈ ADMIN và SHOP_MANAGER được phép xóa (EMPLOYEE bị chặn)
                        .requestMatchers("/admin/products/delete/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "SHOP_MANAGER", "ROLE_SHOP_MANAGER")

                        // 2.3. Xem kho hàng & Lưu/Cập nhật sản phẩm: ADMIN, SHOP_MANAGER và EMPLOYEE
                        // (Quyền của EMPLOYEE chỉ được sửa tồn kho đã được khóa chặt trong AdminController)
                        .requestMatchers("/admin/products", "/admin/products/save")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "SHOP_MANAGER", "ROLE_SHOP_MANAGER", "EMPLOYEE", "ROLE_EMPLOYEE")

                        // 2.4. Chặn toàn bộ tài khoản role USER truy cập vào các đường dẫn /admin/** còn lại
                        .requestMatchers("/admin/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "SHOP_MANAGER", "ROLE_SHOP_MANAGER", "EMPLOYEE", "ROLE_EMPLOYEE")

                        // 3. Các trang yêu cầu đăng nhập của người dùng thông thường
                        .requestMatchers("/profile/**", "/link-google-start").authenticated()

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/403")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler) // Dùng handler kiểm tra role từ MongoDB
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(successHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout") // Chuyển về trang đăng nhập sau khi đăng xuất
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());

        return http.build();
    }
    // Viết gọn lại vì bạn đã import PasswordEncoder và BCryptPasswordEncoder ở trên rồi
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}