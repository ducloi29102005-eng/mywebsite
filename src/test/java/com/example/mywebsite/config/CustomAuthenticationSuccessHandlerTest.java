package com.example.mywebsite.config;

import com.example.mywebsite.model.User;
import com.example.mywebsite.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomAuthenticationSuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private OAuth2AuthenticationToken oAuth2AuthenticationToken;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private CustomAuthenticationSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        // Cấu hình mock mặc định cho OAuth2User
        lenient().when(oAuth2AuthenticationToken.getPrincipal()).thenReturn(oAuth2User);
        lenient().when(oAuth2User.getAttribute("email")).thenReturn("test@gmail.com");
        lenient().when(oAuth2User.getAttribute("name")).thenReturn("Test User");
    }

    @Test
    void onAuthenticationSuccess_NewGoogleUser_ShouldSaveAndRedirectToProfile() throws IOException, ServletException {
        // Giả lập user chưa tồn tại trong DB
        when(userRepository.findByUsername("test@gmail.com")).thenReturn(Optional.empty());

        successHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);

        // Kiểm tra hàm save đã được gọi để tạo user mới
        verify(userRepository, times(1)).save(any(User.class));
        // Kiểm tra chuyển hướng đúng về /profile
        verify(response, times(1)).sendRedirect("/profile");
    }

    @Test
    void onAuthenticationSuccess_ExistingAdminUser_ShouldRedirectToAdminDashboard() throws IOException, ServletException {
        // Giả lập user đã tồn tại và có quyền ADMIN
        User adminUser = new User();
        adminUser.setUsername("test@gmail.com");
        adminUser.setRole("ADMIN");
        adminUser.setStatus("ACTIVE");

        when(userRepository.findByUsername("test@gmail.com")).thenReturn(Optional.of(adminUser));

        successHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);

        // Kiểm tra không tạo mới user
        verify(userRepository, never()).save(any(User.class));
        // Kiểm tra chuyển hướng về trang admin
        verify(response, times(1)).sendRedirect("/admin/dashboard");
    }

    @Test
    void onAuthenticationSuccess_BannedUser_ShouldRedirectToBannedPage() throws IOException, ServletException {

        // Giả lập user đã tồn tại nhưng bị khoá (BANNED)
        User bannedUser = new User();
        bannedUser.setUsername("test@gmail.com");
        bannedUser.setRole("USER");
        bannedUser.setStatus("BANNED");

        when(userRepository.findByUsername("test@gmail.com")).thenReturn(Optional.of(bannedUser));

        successHandler.onAuthenticationSuccess(request, response, oAuth2AuthenticationToken);

        verify(userRepository, never()).save(any(User.class));
        verify(response, times(1)).sendRedirect("/banned");
    }

    @Test
    void onAuthenticationSuccess_FormLogin_NormalUser_ShouldRedirectToProfile() throws IOException, ServletException {
        // Giả lập đăng nhập bằng Form bình thường (không phải OAuth2)
        Authentication normalAuth = mock(Authentication.class);
        when(normalAuth.getName()).thenReturn("normal_user");

        User normalUser = new User();
        normalUser.setUsername("normal_user");
        normalUser.setRole("USER");
        normalUser.setStatus("ACTIVE");

        when(userRepository.findByUsername("normal_user")).thenReturn(Optional.of(normalUser));

        successHandler.onAuthenticationSuccess(request, response, normalAuth);

        verify(response, times(1)).sendRedirect("/profile");
    }
}