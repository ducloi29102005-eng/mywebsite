package com.example.mywebsite.controller;

import com.example.mywebsite.model.User;
import com.example.mywebsite.repository.UserRepository;
import com.example.mywebsite.service.UserService;
import com.example.mywebsite.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {

        // KIỂM TRA TRÙNG LẶP TÊN ĐĂNG NHẬP
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            // Truyền thông báo lỗi chuẩn theo yêu cầu của bạn xuống giao diện
            model.addAttribute("error", "Tài khoản mật khẩu của bạn đã tồn tại, hãy kiểm tra lại!");
            return "register"; // Giữ người dùng ở lại trang register
        }

        // KIỂM TRA TRÙNG LẶP GMAIL (Nếu bạn có ô nhập email)
        if (user.getGmail() != null && userRepository.findByGmail(user.getGmail()).isPresent()) {
            model.addAttribute("error", "Email này đã được sử dụng, hãy kiểm tra lại!");
            return "register";
        }

        // Nếu qua được các bước trên thì mới tiến hành mã hóa mật khẩu và lưu
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        return "redirect:/login?success";
    }

    // ĐÃ GỘP 2 HÀM BANNED LÀM 1 ĐỂ TRÁNH LỖI
    @GetMapping("/banned")
    public String showBannedPage(@RequestParam(value = "username", required = false) String paramUsername,
                                 Principal principal,
                                 Model model) {
        String targetUsername = null;

        if (paramUsername != null && !paramUsername.trim().isEmpty()) {
            targetUsername = paramUsername;
        } else if (principal != null) {
            targetUsername = principal.getName();
        }

        if (targetUsername != null) {
            User user = userRepository.findByUsername(targetUsername).orElse(null);
            if (user != null) {
                model.addAttribute("username", user.getUsername());
                String reason = user.getBanReason();
                if (reason == null || reason.trim().isEmpty()) {
                    reason = "Tài khoản của bạn đã bị khóa do vi phạm tiêu chuẩn cộng đồng.";
                }
                model.addAttribute("reason", reason);
            }
        }
        return "banned";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, Model model) {
        Optional<User> userOptional = userRepository.findByUsername(email);
        if (userOptional.isEmpty()) {
            model.addAttribute("error", "Email không tồn tại trong hệ thống.");
            return "forgot-password";
        }

        User user = userOptional.get();
        String otp = emailService.generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        try {
            emailService.sendOtpEmail(email, otp);
        } catch (Exception e) {
            model.addAttribute("error", "Hệ thống gửi mail đang bị lỗi!");
            return "forgot-password";
        }

        model.addAttribute("email", email);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtpCode(@RequestParam("email") String email, @RequestParam("otpCode") String otpCode, Model model) {
        Optional<User> userOptional = userRepository.findByUsername(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getOtpCode() != null && user.getOtpCode().equals(otpCode) && user.getOtpExpiryTime().isAfter(LocalDateTime.now())) {
                model.addAttribute("email", email);
                return "reset-password";
            }
        }

        model.addAttribute("error", "Mã OTP không chính xác hoặc đã hết hạn!");
        model.addAttribute("email", email);
        return "verify-otp";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("email") String email, @RequestParam("newPassword") String newPassword) {
        Optional<User> userOptional = userRepository.findByUsername(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setOtpCode(null);
            userRepository.save(user);
        }
        return "redirect:/login?resetSuccess";
    }
}