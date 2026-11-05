package com.example.mywebsite.repository;

import com.example.mywebsite.model.UserInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserInfoRepository extends MongoRepository<UserInfo, String> {
    Optional<UserInfo> findByUserId(String userId);
    void deleteByUserId(String userId);
}