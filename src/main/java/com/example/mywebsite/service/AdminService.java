package com.example.mywebsite.service;

import com.example.mywebsite.model.Permission;
import com.example.mywebsite.model.Product;
import com.example.mywebsite.model.RolePermission;
import com.example.mywebsite.model.User;
import com.example.mywebsite.repository.PermissionRepository;
import com.example.mywebsite.repository.ProductRepository;
import com.example.mywebsite.repository.RolePermissionRepository;
import com.example.mywebsite.repository.UserRepository;
import com.example.mywebsite.model.Order;
import com.example.mywebsite.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderRepository orderRepository;

    // ================== COMMON LOGIC ==================

    public String getUserIdFromPrincipal(Principal principal) {
        if (principal == null) return null;
        String identifier;

        if (principal instanceof OAuth2AuthenticationToken) {
            identifier = (String) ((org.springframework.security.oauth2.core.user.OAuth2User)
                    ((OAuth2AuthenticationToken) principal).getPrincipal()).getAttribute("email");
            return userRepository.findByGmail(identifier).map(User::getUserId).orElse(null);
        } else {
            identifier = principal.getName();
            return userRepository.findByUsername(identifier).map(User::getUserId).orElse(null);
        }
    }

    public User getCurrentUser(String userId) {
        if (userId == null) return null;
        return userRepository.findByUserId(userId).orElse(null);
    }

    // ================== USER MANAGEMENT ==================

    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public List<RolePermission> getAllRolePermissions() {
        return rolePermissionRepository.findAll();
    }

    public void updateUserRole(String userId, String role) {
        userRepository.findByUserId(userId).ifPresent(user -> {
            if (!user.getRole().equals("ADMIN") || !role.equals("USER")) {
                user.setRole(role);
                userRepository.save(user);
            }
        });
    }

    public void toggleUserBan(String userId, String reason) {
        userService.toggleBanStatus(userId, reason);
    }

    public void deleteUser(String userId) {
        userService.deleteUser(userId);
    }

    // ================== PRODUCT MANAGEMENT ==================

    public List<Product> getAllProductsDesc() {
        return productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public long getTotalProductsCount() {
        return productRepository.count();
    }

    public long getHotProductsCount(List<Product> products) {
        return products.stream()
                .filter(p -> p.getTag() != null && !p.getTag().trim().isEmpty())
                .count();
    }

    public long getApparelProductsCount(List<Product> products) {
        return products.stream()
                .filter(p -> "apparel".equalsIgnoreCase(p.getCategory()))
                .count();
    }

    public void saveOrUpdateProduct(Product product, String sizeType, String customSizes, User currentUser) {
        String role = (currentUser != null) ? currentUser.getRole() : "USER";

        if ("EMPLOYEE".equals(role)) {
            if (product.getId() != null) {
                Product existing = productRepository.findById(product.getId()).orElse(null);
                if (existing != null) {
                    existing.setStockQuantity(product.getStockQuantity());
                    productRepository.save(existing);
                }
            }
            return;
        }

        if (product.getId() != null && product.getId().trim().isEmpty()) {
            product.setId(null);
        }

        if ("CLOTHING".equalsIgnoreCase(sizeType)) {
            product.setSizes(List.of("S", "M", "L", "XL", "2XL"));
        } else if ("CUSTOM".equalsIgnoreCase(sizeType) && customSizes != null && !customSizes.trim().isEmpty()) {
            String[] split = customSizes.split(",");
            List<String> cleanSizes = new ArrayList<>();
            for (String s : split) {
                if (!s.trim().isEmpty()) cleanSizes.add(s.trim().toUpperCase());
            }
            product.setSizes(cleanSizes.isEmpty() ? List.of("Freesize") : cleanSizes);
        } else {
            product.setSizes(List.of("Freesize"));
        }

        productRepository.save(product);
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }

    // ================== PERMISSION MANAGEMENT ==================

    public void createNewPermission(String name, String description) {
        String cleanName = name.trim().toUpperCase();
        if (permissionRepository.findByName(cleanName).isEmpty()) {
            permissionRepository.save(new Permission(cleanName, description));
        }
    }

    public void toggleRolePermission(String roleName, String permissionName, boolean isGranted) {
        String rName = roleName.trim().toUpperCase();
        String pName = permissionName.trim().toUpperCase();

        if (isGranted) {
            boolean exists = rolePermissionRepository.findByRoleName(rName).stream()
                    .anyMatch(rp -> rp.getPermissionName().equals(pName));
            if (!exists) rolePermissionRepository.save(new RolePermission(rName, pName));
        } else {
            List<RolePermission> mappings = rolePermissionRepository.findByRoleName(rName);
            for (RolePermission mapping : mappings) {
                if (mapping.getPermissionName().equalsIgnoreCase(pName)) {
                    rolePermissionRepository.delete(mapping);
                    break;
                }
            }
        }
    }

    public void removePermissionFromRole(String roleName, String permissionName) {
        toggleRolePermission(roleName, permissionName, false);
    }
    // ================== ORDER MANAGEMENT (QUẢN LÝ ĐƠN HÀNG) ==================

    public List<Order> getAllOrdersDesc() {
        // Lấy tất cả đơn hàng, sắp xếp mới nhất lên đầu
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate"));
    }

    public void updateOrderStatus(String orderId, String newStatus) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(newStatus);
            orderRepository.save(order);
        });
    }
}