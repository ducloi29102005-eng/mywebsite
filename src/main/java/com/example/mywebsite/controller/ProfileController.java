package com.example.mywebsite.controller;

import com.example.mywebsite.model.User;
import com.example.mywebsite.model.UserProfile;
import com.example.mywebsite.model.UserData;
import com.example.mywebsite.model.GameLinked;
import com.example.mywebsite.repository.UserRepository;
import com.example.mywebsite.service.UserService;
import com.example.mywebsite.repository.UserProfileRepository;
import com.example.mywebsite.repository.UserDataRepository;
import com.example.mywebsite.repository.GameLinkedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class ProfileController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private UserDataRepository userDataRepository;
    @Autowired
    private GameLinkedRepository gameLinkedRepository;

    // Hàm phụ trợ tiện dùng lại: Lấy userId của tài khoản đang đăng nhập
    private String getUserId(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .map(User::getUserId)
                .orElse(null);
    }

    // ========================================================
    // TAB 1: HỒ SƠ CỦA BẠN
    // ========================================================
    @GetMapping("/profile")
    public String showProfile(Principal principal, Model model) {
        String username = principal.getName();
        User currentUser = userRepository.findByUsername(username).orElse(new User());

        UserProfile currentProfile = new UserProfile();
        if (currentUser.getUserId() != null) {
            currentProfile = userProfileRepository.findByUserId(currentUser.getUserId())
                    .orElse(new UserProfile());
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("profile", currentProfile);
        System.out.println("profile" + model.toString());
        return "profile";
    }

    @PostMapping("/profile/edit")
    public String saveProfile(Principal principal, @ModelAttribute UserProfile updatedInfo) {
        System.out.println("client send " + updatedInfo.getPhoneNumber());
        String username = principal.getName();
        userService.updateProfile(username, updatedInfo);
        return "redirect:/profile?success";
    }

    // ========================================================
    // TAB 2: TRÒ CHƠI ĐÃ LIÊN KẾT
    // ========================================================
    @GetMapping("/profile/games")
    public String showLinkedGames(Principal principal, Model model) {
        String userId = getUserId(principal);
        GameLinked currentGameLinked = new GameLinked();

        if (userId != null) {
            currentGameLinked = gameLinkedRepository.findByUserId(userId)
                    .orElse(new GameLinked());
        }

        model.addAttribute("appList", currentGameLinked.getApp());
        return "games"; // Trả về giao diện games.html
    }

    @PostMapping("/profile/games/save")
    public String saveLinkedGames(Principal principal,
                                  @RequestParam(value = "linkedGames", required = false) List<String> linkedGames) {
        String userId = getUserId(principal);
        if (userId != null) {
            GameLinked games = gameLinkedRepository.findByUserId(userId)
                    .orElse(new GameLinked());
            games.setUserId(userId);
            games.setLinkedGames(linkedGames);
            gameLinkedRepository.save(games);
        }
        return "redirect:/profile/games?success";
    }

    // ========================================================
    // TAB 3: PHƯƠNG THỨC ĐĂNG NHẬP
    // ========================================================
    // Hiển thị giao diện đổi mật khẩu
    @GetMapping("/profile/security")
    public String showSecuritySettings(Principal principal, Model model) {
        String userId = getUserId(principal);
        UserData currentUserData = new UserData();

        if (userId != null) {
            currentUserData = userDataRepository.findByUserId(userId)
                    .orElse(new UserData());
        }

        model.addAttribute("userData", currentUserData);
        return "security"; // Trả về file security.html
    }

    // 2. Hàm này bị thiếu: Xử lý khi bấm nút "LIÊN KẾT" Gmail
    @PostMapping("/profile/security/link-gmail")
    public String linkGmail(Principal principal, @RequestParam("gmail") String gmail) {
        String userId = getUserId(principal);
        if (userId != null) {
            UserData data = userDataRepository.findByUserId(userId)
                    .orElse(new UserData());
            data.setUserId(userId);
            data.setGmail(gmail);
            userDataRepository.save(data);
        }
        return "redirect:/profile/security?success";
    }


    @GetMapping("/profile/security/password")
    public String showChangePasswordForm() {
        return "password";
    }

    //set new password
    @PostMapping("/profile/security/password")
    public String processChangePassword(Principal principal,
                                        @RequestParam("oldPassword") String oldPassword,
                                        @RequestParam("newPassword") String newPassword,
                                        @RequestParam("confirmPassword") String confirmPassword,
                                        Model model) {
        // Kiểm tra mật khẩu mới và xác nhận có giống nhau không
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "password";
        }

        // Gọi UserService để đổi mật khẩu
        boolean success = userService.changePassword(principal.getName(), oldPassword, newPassword);
        if (!success) {
            model.addAttribute("error", "Mật khẩu hiện tại không chính xác!");
            return "password";
        }

        // Đổi thành công, đá về trang Phương thức đăng nhập kèm thông báo
        return "redirect:/profile/security?success";
    }
}