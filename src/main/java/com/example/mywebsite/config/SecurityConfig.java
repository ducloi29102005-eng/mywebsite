package com.example.mywebsite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // QUAN TRỌNG: Bật tính năng kiểm tra @PreAuthorize trong Controller
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationSuccessHandler successHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF gọn gàng trong 1 lệnh duy nhất để tránh lỗi 403/405 khi gọi API AJAX POST
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // 1. Công khai cho khách
                        .requestMatchers(
                                "/", "/shop", "/register", "/login", "/banned",
                                "/forgot-password", "/verify-otp", "/reset-password",
                                "/ws-chat/**", "/error", "/403", "/css/**", "/images/**", "/js/**"
                        ).permitAll()

                        // 2. PHÂN QUYỀN KHU VỰC QUẢN TRỊ (/admin/**)
                        // Bỏ việc khóa cứng từng link (như /admin/dashboard).
                        // Thay vào đó: Chỉ cần là Admin, Quản lý hoặc Nhân viên thì được qua cổng.
                        // Quyền xem chi tiết (READ, WRITE, DELETE) sẽ do @PreAuthorize trong Controller quyết định!
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
                        .successHandler(successHandler)
                        .defaultSuccessUrl("/admin", true) // Bắt buộc chuyển hướng về trang /admin
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(successHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}