package com.example.mywebsite.repository;

import com.example.mywebsite.model.RolePermission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RolePermissionRepository extends MongoRepository<RolePermission, String> {
    List<RolePermission> findByRoleName(String roleName);
}