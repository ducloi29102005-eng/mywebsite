package com.example.mywebsite.controller;

import com.example.mywebsite.model.Permission;
import com.example.mywebsite.model.Product;
import com.example.mywebsite.model.RolePermission;
import com.example.mywebsite.model.User;
import com.example.mywebsite.repository.PermissionRepository;
import com.example.mywebsite.repository.ProductRepository;
import com.example.mywebsite.repository.RolePermissionRepository;
import com.example.mywebsite.repository.RoleRepository;
import com.example.mywebsite.repository.UserRepository;
import com.example.mywebsite.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository; // Repository quản lý kho sản phẩm trong MongoDB

    private String getUserId(Principal principal) {
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

    // ==================== CÁC CHỨC NĂNG QUẢN LÝ TÀI KHOẢN (GIỮ NGUYÊN) ====================

    // 1. Vào thẳng Dashboard hiển thị danh sách người dùng ngay lập tức
    @PreAuthorize("hasPermission('USER', 'READ')")
    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        List<User> userList = userService.getAllUsers();
        model.addAttribute("users", userList);

        // THÊM 2 DÒNG NÀY: Lấy toàn bộ quyền và toàn bộ cấu hình ánh xạ hiện tại
        model.addAttribute("allPermissions", permissionRepository.findAll());
        model.addAttribute("allRolePermissions", rolePermissionRepository.findAll());

        // ĐÃ SỬA LỖI: Sử dụng getUserId để lấy chính xác ID dù đăng nhập bằng form hay Google
        String currentUserId = getUserId(principal);
        if (currentUserId != null) {
            userRepository.findByUserId(currentUserId).ifPresent(currentUser -> {
                model.addAttribute("currentUser", currentUser);
            });
        }

        return "admin-dashboard";
    }

    // Chuyển hướng tự động dựa trên Role của tài khoản
    @GetMapping({"", "/", "/login"})
    public String redirectToDashboard(Principal principal) {
        String userId = getUserId(principal);
        if (userId != null) {
            User currentUser = userRepository.findByUserId(userId).orElse(null);
            if (currentUser != null) {
                String role = currentUser.getRole();
                // Nếu là Quản lý cửa hàng hoặc Nhân viên kho -> Chuyển thẳng vào tab Sản phẩm
                if ("SHOP_MANAGER".equals(role) || "EMPLOYEE".equals(role)) {
                    return "redirect:/admin/products";
                }
            }
        }
        // Mặc định ADMIN sẽ vào Dashboard
        return "redirect:/admin/dashboard";
    }

    // 3. Xử lý Phân quyền chi tiết (ADMIN, SHOP_MANAGER, EMPLOYEE, USER)
    @PreAuthorize("hasPermission('USER', 'WRITE')")
    @PostMapping("/users/update-role")
    public String updateRole(@RequestParam String userId, @RequestParam String role) {
        userRepository.findByUserId(userId).ifPresent(user -> {
            // Không cho phép Admin tự hạ quyền chính mình để tránh lỗi mất tài khoản quản trị
            if (!user.getRole().equals("ADMIN") || !role.equals("USER")) {
                user.setRole(role);
                userRepository.save(user);
            }
        });
        return "redirect:/admin/dashboard";
    }

    // 4. Xử lý Khóa / Mở khóa tài khoản
    @PreAuthorize("hasPermission('USER', 'DELETE')")
    @PostMapping("/users/toggle-ban")
    public String toggleBan(@RequestParam String userId,
                            @RequestParam(required = false) String reason) {
        userService.toggleBanStatus(userId, reason);
        return "redirect:/admin/dashboard";
    }

    // 5. Xử lý Xóa tài khoản
    @PreAuthorize("hasPermission('USER', 'DELETE')")
    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam String userId) {
        userService.deleteUser(userId);
        return "redirect:/admin/dashboard";
    }

    // ==================== CÁC CHỨC NĂNG QUẢN LÝ SẢN PHẨM MỚI THÊM ====================

    // 6. Trang danh sách và thống kê kho sản phẩm
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    @GetMapping("/products")
    public String manageProducts(Principal principal, Model model) {
        // Đồng bộ thông tin Admin cho header / avatar
        String currentUserId = getUserId(principal);
        if (currentUserId != null) {
            userRepository.findByUserId(currentUserId).ifPresent(currentUser -> {
                model.addAttribute("currentUser", currentUser);
            });
        }

        List<Product> products = productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        long totalProducts = productRepository.count();
        long hotProducts = products.stream()
                .filter(p -> p.getTag() != null && !p.getTag().trim().isEmpty())
                .count();
        long apparelProducts = products.stream()
                .filter(p -> "apparel".equalsIgnoreCase(p.getCategory()))
                .count();

        model.addAttribute("products", products);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("hotProducts", hotProducts);
        model.addAttribute("apparelProducts", apparelProducts);
        model.addAttribute("newProduct", new Product());

        return "admin-products"; // Nạp view admin-products.html
    }

    // 7. Thêm mới hoặc Cập nhật sản phẩm vào MongoDB
    @PreAuthorize("hasPermission('PRODUCT', 'WRITE') or hasPermission('PRODUCT', 'EDIT_STOCK')")
    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute("newProduct") Product product,
                              @RequestParam(value = "sizeType", defaultValue = "FREESIZE") String sizeType,
                              @RequestParam(value = "customSizes", required = false) String customSizes,
                              Principal principal) {

        // Lấy thông tin User đang thao tác
        String userId = getUserId(principal);
        User currentUser = userRepository.findByUserId(userId).orElse(null);
        String role = (currentUser != null) ? currentUser.getRole() : "USER";

        // ==========================================
        // LOGIC DÀNH CHO ROLE: EMPLOYEE (Chỉ cập nhật kho)
        // ==========================================
        if ("EMPLOYEE".equals(role)) {
            if (product.getId() != null) {
                // Lấy sản phẩm gốc từ DB
                Product existing = productRepository.findById(product.getId()).orElse(null);
                if (existing != null) {
                    // CHỈ ghi đè đúng số lượng tồn kho
                    existing.setStockQuantity(product.getStockQuantity());
                    productRepository.save(existing);
                }
            }
            return "redirect:/admin/products?success=stock_updated";
        }

        // ==========================================
        // LOGIC DÀNH CHO ROLE: ADMIN & SHOP_MANAGER (Full quyền)
        // ==========================================
        if (product.getId() != null && product.getId().trim().isEmpty()) {
            product.setId(null);
        }

        // Xử lý logic size (như cũ)
        if ("CLOTHING".equalsIgnoreCase(sizeType)) {
            product.setSizes(List.of("S", "M", "L", "XL", "2XL"));
        } else if ("CUSTOM".equalsIgnoreCase(sizeType) && customSizes != null && !customSizes.trim().isEmpty()) {
            String[] split = customSizes.split(",");
            List<String> cleanSizes = new java.util.ArrayList<>();
            for (String s : split) {
                if (!s.trim().isEmpty()) cleanSizes.add(s.trim().toUpperCase());
            }
            product.setSizes(cleanSizes.isEmpty() ? List.of("Freesize") : cleanSizes);
        } else {
            product.setSizes(List.of("Freesize"));
        }

        productRepository.save(product);
        return "redirect:/admin/products?success=saved";
    }

    // 8. Xóa sản phẩm khỏi MongoDB theo ID
    @PreAuthorize("hasPermission('PRODUCT', 'DELETE')")
    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable("id") String id) {
        productRepository.deleteById(id);
        return "redirect:/admin/products?success=deleted";
    }

    // ==================== CÁC CHỨC NĂNG QUẢN LÝ QUYỀN (MỚI THÊM) ====================

    // 1. Thêm một Quyền hoàn toàn mới vào hệ thống (Bảng permissions)
    @PreAuthorize("hasPermission('USER', 'WRITE')") // Hoặc bạn có thể tạo quyền 'ROLE_WRITE' riêng
    @PostMapping("/permissions/add")
    public String addNewPermission(@RequestParam String name, @RequestParam String description) {
        String cleanName = name.trim().toUpperCase();

        // Kiểm tra xem quyền này đã tồn tại trong DB chưa để tránh lỗi trùng lặp
        if (permissionRepository.findByName(cleanName).isEmpty()) {
            permissionRepository.save(new Permission(cleanName, description));
        }
        return "redirect:/admin/dashboard?success=permission_created";
    }

    // 2. Cấp một Quyền cho một Role cụ thể (Thêm 1 dòng vào bảng role_permissions)
    @PreAuthorize("hasPermission('USER', 'WRITE')")
    @PostMapping("/roles/assign-permission")
    public String assignPermissionToRole(@RequestParam String roleName, @RequestParam String permissionName) {
        String rName = roleName.trim().toUpperCase();
        String pName = permissionName.trim().toUpperCase();

        // Kiểm tra xem Role này đã được cấp quyền này chưa
        boolean alreadyAssigned = rolePermissionRepository.findByRoleName(rName).stream()
                .anyMatch(rp -> rp.getPermissionName().equals(pName));

        // Nếu chưa có thì mới lưu xuống bảng trung gian
        if (!alreadyAssigned) {
            rolePermissionRepository.save(new RolePermission(rName, pName));
        }
        return "redirect:/admin/dashboard?success=permission_assigned";
    }

    // 3. Thu hồi (Xóa) một Quyền khỏi một Role (Xóa 1 dòng trong bảng role_permissions)
    @PreAuthorize("hasPermission('USER', 'WRITE')")
    @PostMapping("/roles/remove-permission")
    public String removePermissionFromRole(@RequestParam String roleName, @RequestParam String permissionName) {
        // Tìm tất cả các quyền đang có của Role
        List<RolePermission> mappings = rolePermissionRepository.findByRoleName(roleName.trim().toUpperCase());

        // Tìm đúng dòng ánh xạ cần xóa và ra lệnh xóa
        for (RolePermission mapping : mappings) {
            if (mapping.getPermissionName().equalsIgnoreCase(permissionName.trim())) {
                rolePermissionRepository.delete(mapping);
                break; // Xóa xong thì thoát vòng lặp
            }
        }
        return "redirect:/admin/dashboard?success=permission_removed";
    }
    // ==================== API CHO GIAO DIỆN AJAX (MỚI THÊM) ====================
    // API để tích/bỏ tích checkbox
    @PreAuthorize("hasPermission('USER', 'WRITE')")
    @PostMapping("/api/roles/toggle-permission")
    @ResponseBody
    public String togglePermission(@RequestParam String roleName, @RequestParam String permissionName, @RequestParam boolean isGranted) {
        String rName = roleName.trim().toUpperCase();
        String pName = permissionName.trim().toUpperCase();

        if (isGranted) {
            // Nếu đánh dấu tích -> Thêm vào DB (nếu chưa có)
            boolean exists = rolePermissionRepository.findByRoleName(rName).stream()
                    .anyMatch(rp -> rp.getPermissionName().equals(pName));
            if (!exists) rolePermissionRepository.save(new RolePermission(rName, pName));
        } else {
            // Nếu bỏ tích -> Xóa khỏi DB
            List<RolePermission> mappings = rolePermissionRepository.findByRoleName(rName);
            for (RolePermission mapping : mappings) {
                if (mapping.getPermissionName().equalsIgnoreCase(pName)) {
                    rolePermissionRepository.delete(mapping);
                    break;
                }
            }
        }
        return "OK";
    }
    // ==================== QUẢN LÝ WEBSOCKET TRÊN SERVER ====================

    // 1. Mở trang giao diện giám sát Socket
    @GetMapping("/sockets")
    public String monitorSockets() {
        return "admin-sockets";
    }

    // 2. API trả về danh sách các kết nối Socket đang hoạt động
    @GetMapping("/api/sockets")
    @ResponseBody
    public List<java.util.Map<String, Object>> getActiveSockets() {
        return com.example.mywebsite.config.WebSocketEventListener.getAllActiveSessions();
    }
}