package com.example.mywebsite.service;

import com.example.mywebsite.model.User;
import com.example.mywebsite.model.UserProfile;
import com.example.mywebsite.repository.UserRepository;
import com.example.mywebsite.repository.UserProfileRepository;
import com.example.mywebsite.repository.UserDataRepository;
import com.example.mywebsite.repository.GameLinkedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID; // Dùng để tạo userId tự động

@Service
public class UserService {

    // Dán đoạn này vào bên trong class UserService
    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(User user) {
        // Sinh userId tự động cho tài khoản mới
        user.setUserId(UUID.randomUUID().toString());
        // Mã hóa mật khẩu
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        userRepository.save(user);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserDataRepository userDataRepository;

    @Autowired
    private GameLinkedRepository gameLinkedRepository;

    // =======================================================
    // 1. CHỨC NĂNG USER: Cập nhật hồ sơ
    // =======================================================
    public void updateProfile(String username, UserProfile updatedInfo) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String userId = user.getUserId();

            if (userId != null) {
                UserProfile profile = userProfileRepository.findByUserId(userId).orElse(new UserProfile());
                profile.setUserId(userId);

                // Cập nhật các trường thông tin từ form vào database
                if (updatedInfo.getFullName() != null) {
                    profile.setFullName(updatedInfo.getFullName());
                }
                userProfileRepository.save(profile);
            }
        }
    }

    // =======================================================
    // 2. CHỨC NĂNG ADMIN: Quản lý người dùng
    // =======================================================

    // Lấy danh sách tất cả người dùng
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Đổi vai trò (ADMIN <-> USER)
    public void toggleAdminRole(String userId) {
        userRepository.findByUserId(userId).ifPresent(user -> {
            if ("ADMIN".equals(user.getRole())) {
                user.setRole("USER");
            } else {
                user.setRole("ADMIN");
            }
            userRepository.save(user);
        });
    }

    // Khóa / Mở khóa tài khoản
    public void toggleBanStatus(String userId, String reason) {
        userRepository.findByUserId(userId).ifPresent(user -> {
            if ("BANNED".equalsIgnoreCase(user.getStatus())) {
                user.setStatus("ACTIVE");
                user.setBanReason(null);
            } else {
                user.setStatus("BANNED");
                user.setBanReason(reason);
            }
            userRepository.save(user);
        });
    }

    // Xóa tài khoản (Xóa sạch dữ liệu ở cả 4 bảng dựa trên userId)
    public void deleteUser(String userId) {
        userRepository.findByUserId(userId).ifPresent(user -> userRepository.delete(user));
        userProfileRepository.findByUserId(userId).ifPresent(profile -> userProfileRepository.delete(profile));

        // Đảm bảo không bị lỗi null nếu bạn chưa dùng tới bảng UserData hoặc GameLinked
        if (userDataRepository != null) {
            userDataRepository.findByUserId(userId).ifPresent(data -> userDataRepository.delete(data));
        }
        if (gameLinkedRepository != null) {
            gameLinkedRepository.findByUserId(userId).ifPresent(game -> gameLinkedRepository.delete(game));
        }
    }
}