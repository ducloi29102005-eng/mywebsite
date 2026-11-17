package com.example.mywebsite.repository;

import com.example.mywebsite.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    // Lấy lịch sử chat của 1 phòng theo thứ tự thời gian tăng dần
    List<ChatMessage> findByRoomIdOrderByTimestampAsc(String roomId);
}