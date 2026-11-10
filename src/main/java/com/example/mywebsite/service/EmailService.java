package com.example.mywebsite.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Hàm tạo mã OTP 6 số ngẫu nhiên
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    // Hàm gửi email
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();

        // Thay bằng đúng địa chỉ email mà bạn đã cấu hình trong application.properties
        //TODO: remove gmail from code, move to .env file
        message.setFrom("ducloi29102005@gmail.com");

        message.setTo(toEmail);
        message.setSubject("Mã xác thực OTP - Panthera");
        message.setText("Chào bạn,\n\n"
                + "Bạn vừa yêu cầu đổi mật khẩu tại Panthera. Mã xác thực OTP của bạn là: " + otp + "\n\n"
                + "Mã này sẽ hết hạn sau 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ Panthera.");

        // Lệnh thực thi gửi mail
        mailSender.send(message);
    }
}