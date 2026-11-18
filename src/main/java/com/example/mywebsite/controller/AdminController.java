package com.example.mywebsite.controller;

import com.example.mywebsite.model.Product;
import com.example.mywebsite.model.User;
import com.example.mywebsite.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // ==================== CÁC CHỨC NĂNG QUẢN LÝ TÀI KHOẢN ====================

    @PreAuthorize("hasPermission('USER', 'READ')")
    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("users", adminService.getAllUsers());
        model.addAttribute("allPermissions", adminService.getAllPermissions());
        model.addAttribute("allRolePermissions", adminService.getAllRolePermissions());

        String currentUserId = adminService.getUserIdFromPrincipal(principal);
        User currentUser = adminService.getCurrentUser(currentUserId);
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }

        return "admin-dashboard";
    }

    @GetMapping({"", "/", "/login"})
    public String redirectToDashboard(Principal principal) {
        String userId = adminService.getUserIdFromPrincipal(principal);
        User currentUser = adminService.getCurrentUser(userId);

        if (currentUser != null) {
            String role = currentUser.getRole();
            if ("SHOP_MANAGER".equals(role) || "EMPLOYEE".equals(role)) {
                return "redirect:/admin/products";
            }
        }
        return "redirect:/admin/dashboard";
    }

    @PreAuthorize("hasPermission('USER', 'WRITE')")
    @PostMapping("/users/update-role")
    public String updateRole(@RequestParam String userId, @RequestParam String role) {
        adminService.updateUserRole(userId, role);
        return "redirect:/admin/dashboard";
    }

    @PreAuthorize("hasPermission('USER', 'DELETE')")
    @PostMapping("/users/toggle-ban")
    public String toggleBan(@RequestParam String userId, @RequestParam(required = false) String reason) {
        adminService.toggleUserBan(userId, reason);
        return "redirect:/admin/dashboard";
    }

    @PreAuthorize("hasPermission('USER', 'DELETE')")
    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam String userId) {
        adminService.deleteUser(userId);
        return "redirect:/admin/dashboard";
    }

    // ==================== CÁC CHỨC NĂNG QUẢN LÝ SẢN PHẨM ====================

    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    @GetMapping("/products")
    public String manageProducts(Principal principal, Model model) {
        String currentUserId = adminService.getUserIdFromPrincipal(principal);
        User currentUser = adminService.getCurrentUser(currentUserId);
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }

        List<Product> products = adminService.getAllProductsDesc();

        model.addAttribute("products", products);
        model.addAttribute("totalProducts", adminService.getTotalProductsCount());
        model.addAttribute("hotProducts", adminService.getHotProductsCount(products));
        model.addAttribute("apparelProducts", adminService.getApparelProductsCount(products));
        model.addAttribute("newProduct", new Product());

        return "admin-products";
    }

    @PreAuthorize("hasPermission('PRODUCT', 'WRITE') or hasPermission('PRODUCT', 'EDIT_STOCK')")
    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute("newProduct") Product product,
                              @RequestParam(value = "sizeType", defaultValue = "FREESIZE") String sizeType,
                              @RequestParam(value = "customSizes", required = false) String customSizes,
                              Principal principal) {

        String userId = adminService.getUserIdFromPrincipal(principal);
        User currentUser = adminService.getCurrentUser(userId);

        adminService.saveOrUpdateProduct(product, sizeType, customSizes, currentUser);

        if (currentUser != null && "EMPLOYEE".equals(currentUser.getRole())) {
            return "redirect:/admin/products?success=stock_updated";
        }
        return "redirect:/admin/products?success=saved";
    }

    @PreAuthorize("hasPermission('PRODUCT', 'DELETE')")
    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable("id") String id) {
        adminService.deleteProduct(id);
        return "redirect:/admin/products?success=deleted";
    }

    // ==================== CÁC CHỨC NĂNG QUẢN LÝ QUYỀN ====================

    @PreAuthorize("hasPermission('USER', 'WRITE')")
    @PostMapping("/permissions/add")
    public String addNewPermission(@RequestParam String name, @RequestParam String description) {
        adminService.createNewPermission(name, description);
        return "redirect:/admin/dashboard?success=permission_created";
    }

    @PreAuthorize("hasPermission('USER', 'WRITE')")
    @PostMapping("/roles/assign-permission")
    public String assignPermissionToRole(@RequestParam String roleName, @RequestParam String permissionName) {
        adminService.toggleRolePermission(roleName, permissionName, true);
        return "redirect:/admin/dashboard?success=permission_assigned";
    }

    @PreAuthorize("hasPermission('USER', 'WRITE')")
    @PostMapping("/roles/remove-permission")
    public String removePermissionFromRole(@RequestParam String roleName, @RequestParam String permissionName) {
        adminService.removePermissionFromRole(roleName, permissionName);
        return "redirect:/admin/dashboard?success=permission_removed";
    }

    @PreAuthorize("hasPermission('USER', 'WRITE')")
    @PostMapping("/api/roles/toggle-permission")
    @ResponseBody
    public String togglePermission(@RequestParam String roleName, @RequestParam String permissionName, @RequestParam boolean isGranted) {
        adminService.toggleRolePermission(roleName, permissionName, isGranted);
        return "OK";
    }

    // ==================== QUẢN LÝ WEBSOCKET TRÊN SERVER ====================

    @GetMapping("/sockets")
    public String monitorSockets() {
        return "admin-sockets";
    }

    @GetMapping("/api/sockets")
    @ResponseBody
    public List getActiveSockets() {
        return com.example.mywebsite.config.WebSocketEventListener.getAllActiveSessions();
    }
    // ==================== CÁC CHỨC NĂNG QUẢN LÝ ĐƠN HÀNG ====================

    @PreAuthorize("hasPermission('PRODUCT', 'READ')") // Dùng tạm quyền đọc sản phẩm hoặc bạn có thể tạo quyền ORDER riêng
    @GetMapping("/orders")
    public String manageOrders(Principal principal, Model model) {
        String currentUserId = adminService.getUserIdFromPrincipal(principal);
        User currentUser = adminService.getCurrentUser(currentUserId);
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }

        // Lấy danh sách đơn hàng
        model.addAttribute("orders", adminService.getAllOrdersDesc());

        return "admin-orders"; // Gọi file giao diện admin-orders.html
    }

    @PreAuthorize("hasPermission('PRODUCT', 'WRITE') or hasPermission('PRODUCT', 'EDIT_STOCK')")
    @PostMapping("/orders/update-status")
    public String updateOrderStatus(@RequestParam("orderId") String orderId,
                                    @RequestParam("status") String status) {
        adminService.updateOrderStatus(orderId, status);
        return "redirect:/admin/orders?success=status_updated";
    }
}