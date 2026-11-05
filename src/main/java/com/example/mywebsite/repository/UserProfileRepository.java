package com.example.mywebsite.repository;

import com.example.mywebsite.model.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
    Optional<UserProfile> findByUserId(String userId); // Tìm hồ sơ dựa vào ID của User
    void deleteByUserId(String userId);
}