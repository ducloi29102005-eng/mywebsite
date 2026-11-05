package com.example.mywebsite.service;

import com.example.mywebsite.model.*;
import com.example.mywebsite.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private UserInfoRepository userInfoRepository;
    @Autowired
    private UserDataRepository userDataRepository;
    @Autowired
    private GameLinkedRepository gameLinkedRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }
        if (user.getStatus() == null || user.getStatus().isEmpty()) {
            user.setStatus("ACTIVE");
        }

        // 1. Tạo mã userId riêng có dấu gạch ngang
        String customUserId = UUID.randomUUID().toString();
        user.setUserId(customUserId);

        // 2. Lưu User gốc
        User savedUser = userRepository.save(user);
        String commonUserId = savedUser.getUserId();

        // 3. Khởi tạo và liên kết các bảng phụ bằng chung 1 mã commonUserId
        // -> Bảng 1: user_infor
        UserInfo info = new UserInfo();
        info.setUserId(commonUserId);
        userInfoRepository.save(info);

        // -> Bảng 2: user_data (Gmail)
        UserData data = new UserData();
        data.setUserId(commonUserId);
        userDataRepository.save(data);

        // -> Bảng 3: game_linked (Game)
        GameLinked games = new GameLinked();
        games.setUserId(commonUserId);
        gameLinkedRepository.save(games);

        // -> Bảng 4: user_profiles
        UserProfile profile = new UserProfile();
        profile.setUserId(commonUserId);
        userProfileRepository.save(profile);
    }

    public void updateProfile(String username, UserProfile updatedInfo) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        String commonUserId = user.getUserId();

        // Tìm profile dựa trên UUID
        UserProfile existingProfile = userProfileRepository.findByUserId(commonUserId)
                .orElse(new UserProfile());

        // Cập nhật thông tin cá nhân
        existingProfile.setUserId(commonUserId);
        existingProfile.setFullName(updatedInfo.getFullName());
        existingProfile.setDateOfBirth(updatedInfo.getDateOfBirth());
        existingProfile.setPhoneNumber(updatedInfo.getPhoneNumber());
        existingProfile.setAddress(updatedInfo.getAddress());

        // Lưu vào bảng user_profiles
        userProfileRepository.save(existingProfile);
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);

        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            // Trả về user để LoginController kiểm tra status == 'BANNED' và chuyển trang banned.html
            return user;
        }
        return null;
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ DÀNH RIÊNG CHO QUẢN TRỊ VIÊN
    // ==========================================

    // 1. Lấy toàn bộ danh sách tài khoản hiển thị trên Dashboard
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 2. Chuyển đổi vai trò qua lại giữa ADMIN và USER
    public void toggleAdminRole(String userId) {
        userRepository.findByUserId(userId).ifPresent(user -> {
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                user.setRole("USER");
            } else {
                user.setRole("ADMIN");
            }
            userRepository.save(user);
        });
    }

    // 3. Khóa hoặc Mở khóa tài khoản (BANNED <-> ACTIVE)
    public void toggleBanStatus(String userId, String reason) {
        userRepository.findByUserId(userId).ifPresent(user -> {
            if ("BANNED".equalsIgnoreCase(user.getStatus())) {
                user.setStatus("ACTIVE");
                user.setBanReason(null);
            } else {
                user.setStatus("BANNED");
                user.setBanReason(reason != null && !reason.trim().isEmpty() ? reason : "Vi phạm quy định hệ thống");
            }
            userRepository.save(user);
        });
    }

    // 4. Xóa tài khoản chính và dọn dẹp các collection liên quan
    public void deleteUser(String userId) {
        userRepository.findByUserId(userId).ifPresent(user -> {
            userProfileRepository.deleteByUserId(userId);
            userInfoRepository.deleteByUserId(userId);
            userDataRepository.deleteByUserId(userId);
            gameLinkedRepository.deleteByUserId(userId);
            userRepository.delete(user);
        });
    }
}