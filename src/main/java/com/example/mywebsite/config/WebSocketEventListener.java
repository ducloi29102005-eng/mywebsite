package com.example.mywebsite.config;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    private static final Map<String, Map<String, Object>> activeSessions = new ConcurrentHashMap<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String username = (accessor.getUser() != null) ? accessor.getUser().getName() : "Khách (Chưa đăng nhập)";

        if (sessionId != null) {
            Map<String, Object> sessionInfo = new LinkedHashMap<>();
            sessionInfo.put("sessionId", sessionId);
            sessionInfo.put("username", username);
            sessionInfo.put("connectedAt", LocalDateTime.now().format(formatter));
            sessionInfo.put("subscriptions", new HashSet<String>());
            activeSessions.put(sessionId, sessionInfo);
        }
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();

        if (sessionId != null && destination != null && activeSessions.containsKey(sessionId)) {
            @SuppressWarnings("unchecked")
            Set<String> subs = (Set<String>) activeSessions.get(sessionId).get("subscriptions");
            subs.add(destination);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            activeSessions.remove(sessionId);
        }
    }

    public static ArrayList getAllActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }
}