package com.example.mywebsite.controller;

import com.example.mywebsite.model.ChatMessage;
import com.example.mywebsite.model.User;
import com.example.mywebsite.repository.ChatMessageRepository;
import com.example.mywebsite.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;

import java.security.Principal;
import java.util.*;

@Controller
public class ChatController {

    @Autowired
    private ChatMessageRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private User getCurrentUser(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken) {
            String email = ((OAuth2AuthenticationToken) principal).getPrincipal().getAttribute("email");
            return userRepository.findByGmail(email).orElse(null);
        }
        return userRepository.findByUsername(principal.getName()).orElse(null);
    }

    // ==================== API DÀNH CHO KHÁCH HÀNG (WIDGET) ====================

    @GetMapping("/api/chat/my-history")
    @ResponseBody
    public ResponseEntity<?> getMyChatHistory(Principal principal) {
        User user = getCurrentUser(principal);
        if (user == null) return ResponseEntity.status(401).body("Chưa đăng nhập");
        return ResponseEntity.ok(chatRepository.findByRoomIdOrderByTimestampAsc(user.getUserId()));
    }

    // API cũ dự phòng - Nhưng giờ ta dùng Socket là chính
    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<?> sendCustomerMessage(@RequestParam String content, Principal principal) {
        User user = getCurrentUser(principal);
        if (user == null) return ResponseEntity.status(401).body("Chưa đăng nhập");
        if (content == null || content.trim().isEmpty()) return ResponseEntity.badRequest().body("Tin nhắn trống");

        ChatMessage msg = new ChatMessage(
                user.getUserId(),
                user.getUserId(),
                user.getUsername(),
                user.getRole() != null ? user.getRole() : "USER",
                content.trim()
        );
        chatRepository.save(msg);

        // SỬA: Phát tin nhắn qua WebSocket ngay khi gọi REST API
        messagingTemplate.convertAndSend("/queue/chat/" + user.getUserId(), msg);
        messagingTemplate.convertAndSend("/topic/admin-chat", msg);

        return ResponseEntity.ok(msg);
    }

    // ==================== GIAO DIỆN & API DÀNH CHO ADMIN ====================

    @PreAuthorize("hasPermission('USER', 'READ')")
    @GetMapping("/admin/chat")
    public String adminChatPage(Model model) {
        List<ChatMessage> allMessages = chatRepository.findAll();
        Map<String, String> activeRooms = new LinkedHashMap<>();

        for (ChatMessage m : allMessages) {
            if ("USER".equals(m.getSenderRole())) {
                activeRooms.put(m.getRoomId(), m.getSenderName());
            } else if (!activeRooms.containsKey(m.getRoomId())) {
                userRepository.findByUserId(m.getRoomId())
                        .ifPresent(u -> activeRooms.put(m.getRoomId(), u.getUsername()));
            }
        }
        model.addAttribute("rooms", activeRooms);
        return "admin-chat";
    }

    @PreAuthorize("hasPermission('USER', 'READ')")
    @GetMapping("/admin/api/chat/history/{roomId}")
    @ResponseBody
    public List<ChatMessage> getRoomHistory(@PathVariable String roomId) {
        return chatRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }

    @PreAuthorize("hasPermission('USER', 'WRITE')")
    @PostMapping("/admin/api/chat/reply")
    @ResponseBody
    public ResponseEntity<?> replyCustomer(@RequestParam String roomId,
                                           @RequestParam String content,
                                           Principal principal) {
        User admin = getCurrentUser(principal);
        if (admin == null || content.trim().isEmpty()) return ResponseEntity.badRequest().build();

        ChatMessage reply = new ChatMessage(
                roomId,
                admin.getUserId(),
                admin.getUsername() + " (Hỗ trợ)",
                admin.getRole(),
                content.trim()
        );
        chatRepository.save(reply);

        // SỬA LỖI: Phát tin nhắn phản hồi qua WebSocket về máy khách hàng
        messagingTemplate.convertAndSend("/queue/chat/" + roomId, reply);

        return ResponseEntity.ok(reply);
    }

    // ==================== TÍNH NĂNG BÁO THÔNG BÁO (REAL-TIME) ====================

    @PreAuthorize("hasPermission('USER', 'READ')")
    @GetMapping("/admin/api/chat/rooms-data")
    @ResponseBody
    public Map<String, String> getActiveRoomsData() {
        List<ChatMessage> allMessages = chatRepository.findAll();
        Map<String, String> activeRooms = new LinkedHashMap<>();
        for (ChatMessage m : allMessages) {
            if ("USER".equals(m.getSenderRole())) {
                activeRooms.put(m.getRoomId(), m.getSenderName());
            } else if (!activeRooms.containsKey(m.getRoomId())) {
                userRepository.findByUserId(m.getRoomId())
                        .ifPresent(u -> activeRooms.put(m.getRoomId(), u.getUsername()));
            }
        }
        return activeRooms;
    }

    @PreAuthorize("hasPermission('USER', 'READ')")
    @GetMapping("/admin/api/chat/check-new")
    @ResponseBody
    public long checkNewMessages() {
        return chatRepository.findAll().stream().filter(m -> "USER".equals(m.getSenderRole())).count();
    }

    // ==================== XỬ LÝ WEBSOCKET TRỰC TIẾP ====================

    // 1. Khách hàng gửi tin nhắn qua Socket
    @MessageMapping("/chat.send")
    public void handleCustomerMessage(@Payload Map<String, String> payload, Principal principal) {
        String content = payload.get("content");
        User user = getCurrentUser(principal);

        if (user == null || content == null || content.trim().isEmpty()) return;

        ChatMessage msg = new ChatMessage(
                user.getUserId(),
                user.getUserId(),
                user.getUsername(),
                user.getRole() != null ? user.getRole() : "USER",
                content.trim()
        );
        chatRepository.save(msg);

        // SỬA: Đảm bảo đường dẫn này khớp 100% với hàm subscribe ở Frontend
        messagingTemplate.convertAndSend("/queue/chat/" + user.getUserId(), msg);
        messagingTemplate.convertAndSend("/topic/admin-chat", msg);
    }

    // 2. Admin trả lời Khách hàng qua Socket
    @MessageMapping("/chat.reply")
    public void handleAdminReply(@Payload Map<String, String> payload, Principal principal) {
        User admin = getCurrentUser(principal);
        String roomId = payload.get("roomId");
        String content = payload.get("content");

        if (admin == null || content == null || content.trim().isEmpty()) return;

        ChatMessage reply = new ChatMessage(
                roomId, admin.getUserId(), admin.getUsername() + " (Hỗ trợ)",
                admin.getRole(), content.trim()
        );
        chatRepository.save(reply);

        messagingTemplate.convertAndSend("/queue/chat/" + roomId, reply);
        messagingTemplate.convertAndSend("/topic/admin-chat", reply);
    }

    // 3. Admin gửi tin nhắn trực tiếp từ trang Giám sát Socket tới một Session/User cụ thể
    @MessageMapping("/chat.send-to-session")
    public void handleDirectSocketMessage(@Payload Map<String, String> payload, Principal principal) {
        User admin = getCurrentUser(principal);
        String targetUsername = payload.get("username");
        String targetSessionId = payload.get("sessionId");
        String content = payload.get("content");

        if (admin == null || content == null || content.trim().isEmpty()) return;

        // Tìm User đang sở hữu Socket đó
        Optional<User> targetUserOpt = userRepository.findByUsername(targetUsername);
        if (targetUserOpt.isPresent()) {
            User targetUser = targetUserOpt.get();
            ChatMessage msg = new ChatMessage(
                    targetUser.getUserId(),
                    admin.getUserId(),
                    admin.getUsername() + " (Hỗ trợ trực tiếp)",
                    admin.getRole(),
                    content.trim()
            );
            chatRepository.save(msg);

            // Đẩy tin nhắn vào đúng phòng chat của User và đồng bộ sang trang Admin Chat
            messagingTemplate.convertAndSend("/queue/chat/" + targetUser.getUserId(), msg);
            messagingTemplate.convertAndSend("/topic/admin-chat", msg);
        } else if (targetSessionId != null) {
            // Dự phòng: Nếu là khách vãng lai chưa đăng nhập, gửi thẳng vào kênh theo Session ID
            Map<String, String> directMsg = new HashMap<>();
            directMsg.put("senderName", admin.getUsername() + " (Hỗ trợ)");
            directMsg.put("senderRole", admin.getRole());
            directMsg.put("content", content.trim());
            messagingTemplate.convertAndSend("/queue/session/" + targetSessionId, directMsg);
        }
    }
}