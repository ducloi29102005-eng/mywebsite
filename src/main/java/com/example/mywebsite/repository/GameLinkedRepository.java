package com.example.mywebsite.repository;

import com.example.mywebsite.model.GameLinked;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface GameLinkedRepository extends MongoRepository<GameLinked, String> {
    Optional<GameLinked> findByUserId(String userId);
    void deleteByUserId(String userId);
}