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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

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

    // ========================================================
    // HÀM QUAN TRỌNG: Lấy đúng username dù đăng nhập bằng Google hay Form
    // ========================================================
    private String getActualUsername(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken) {
            return ((OAuth2AuthenticationToken) principal).getPrincipal().getAttribute("email");
        }
        return principal.getName();
    }

    // ========================================================
    // HÀM CHUẨN HÓA: Tìm trực tiếp userId
    // ========================================================
    private String getUserId(Principal principal) {
        if (principal == null) return null;
        String identifier;

        // Nếu đăng nhập Google -> Tìm qua cột gmail
        if (principal instanceof OAuth2AuthenticationToken) {
            identifier = ((OAuth2AuthenticationToken) principal).getPrincipal().getAttribute("email");
            return userRepository.findByGmail(identifier).map(User::getUserId).orElse(null);
        } else {
            // Nếu đăng nhập thường -> Tìm qua cột username
            identifier = principal.getName();
            return userRepository.findByUsername(identifier).map(User::getUserId).orElse(null);
        }
    }

    // ========================================================
    // TAB 1: HỒ SƠ CỦA BẠN (Đã chuyển sang dùng userId)
    // ========================================================
    @GetMapping("/profile")
    public String showProfile(Principal principal, Model model) {
        String userId = getUserId(principal);
        if (userId != null) {
            User currentUser = userRepository.findByUserId(userId).orElse(new User());
            UserProfile currentProfile = userProfileRepository.findByUserId(userId).orElse(new UserProfile());

            model.addAttribute("user", currentUser);
            model.addAttribute("profile", currentProfile);
            model.addAttribute("userData", currentUser);
        } else {
            return "redirect:/login"; // Nếu chưa đăng nhập thì đẩy về trang login
        }
        return "profile";
    }

    @PostMapping("/profile/edit")
    public String saveProfile(Principal principal, @ModelAttribute UserProfile updatedInfo) {
        String username = getActualUsername(principal);
        if (username != null) {
            userService.updateProfile(username, updatedInfo);
        }
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
    // TAB 3: PHƯƠNG THỨC ĐĂNG NHẬP / CÀI ĐẶT BẢO MẬT
    // ========================================================
    @GetMapping("/profile/security")
    public String showSecurityPage(Principal principal, Model model) {
        String userId = getUserId(principal);

        if (userId != null) {
            User currentUser = userRepository.findByUserId(userId).orElse(new User());
            model.addAttribute("user", currentUser);

            // Lấy thông tin từ bảng UserData dựa trên userId
            UserData userData = userDataRepository.findByUserId(userId).orElse(new UserData());

            // Nếu bảng UserData chưa kịp cập nhật, lấy tạm Gmail từ bảng User sang để hiển thị không bị lỗi
            if (userData.getGmail() == null || userData.getGmail().trim().isEmpty()) {
                userData.setGmail(currentUser.getGmail());
            }

            // Gửi userData lên giao diện HTML
            model.addAttribute("userData", userData);
        } else {
            return "redirect:/login";
        }
        return "security";
    }

    // Endpoint bắt đầu liên kết Google (Lưu userId vào session)
    @GetMapping("/link-google-start")
    public String startGoogleLink(Principal principal, jakarta.servlet.http.HttpSession session) {
        String userId = getUserId(principal);
        if (userId != null) {
            session.setAttribute("linkingUserId", userId);
            return "redirect:/oauth2/authorization/google";
        }
        return "redirect:/login";
    }

    // Endpoint Hủy Liên Kết Gmail (Đã cập nhật để xóa sạch ở cả 2 bảng bằng userId)
    @RequestMapping(value = "/profile/security/unlink-gmail", method = {RequestMethod.GET, RequestMethod.POST})
    public String unlinkGmail(Principal principal) {
        String userId = getUserId(principal);
        if (userId != null) {
            // 1. Gỡ liên kết gmail ở bảng User
            userRepository.findByUserId(userId).ifPresent(user -> {
                user.setGmail(null);
                userRepository.save(user);
            });

            // 2. Gỡ liên kết gmail ở bảng UserData
            userDataRepository.findByUserId(userId).ifPresent(userData -> {
                userData.setGmail(null);
                userDataRepository.save(userData);
            });
        }
        return "redirect:/profile/security?unlinkSuccess";
    }
}