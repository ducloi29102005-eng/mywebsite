package com.example.mywebsite.repository;

import com.example.mywebsite.model.UserData;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserDataRepository extends MongoRepository<UserData, String> {
    Optional<UserData> findByUserId(String userId);
    void deleteByUserId(String userId);
}
