package com.example.mywebsite.controller;

import com.example.mywebsite.model.Cart;
import com.example.mywebsite.model.CartItem;
import com.example.mywebsite.model.Order;
import com.example.mywebsite.model.Product;
import com.example.mywebsite.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
public class ShopController {

    @Autowired
    private ShopService shopService;

    @GetMapping("/shop")
    public String showShopPage(@RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "size", defaultValue = "12") int size,
                               @RequestParam(value = "sort", defaultValue = "dateDesc") String sort,
                               @RequestParam(value = "category", required = false) String category,
                               Principal principal,
                               Model model) {
        String userId = shopService.getUserId(principal);
        shopService.populateUserHeaderData(userId, model);

        int currentPage = Math.max(page, 1);
        Page<Product> productPage = shopService.getProductsPage(currentPage, size, sort, category);

        int totalPages = productPage.getTotalPages();
        if (totalPages == 0) totalPages = 1;

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("currentSort", sort);
        model.addAttribute("selectedCategory", category != null ? category : "all");
        model.addAttribute("totalProducts", productPage.getTotalElements());

        return "shop";
    }

    @GetMapping("/shop/product/{id}")
    public String productDetail(@PathVariable("id") String id, Principal principal, Model model) {
        String userId = shopService.getUserId(principal);
        shopService.populateUserHeaderData(userId, model);

        Product product = shopService.getProductById(id);
        if (product == null) return "redirect:/shop";

        model.addAttribute("product", product);
        return "product-detail";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("productId") String productId,
                            @RequestParam(value = "size", defaultValue = "Freesize") String size,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            @RequestParam(value = "action", defaultValue = "add") String action,
                            Principal principal) {
        String userId = shopService.getUserId(principal);
        if (userId == null) return "redirect:/login";

        shopService.addProductToCart(userId, productId, size, quantity);

        if ("buy_now".equals(action)) {
            return "redirect:/cart";
        }
        return "redirect:/shop/product/" + productId + "?added=true";
    }

    @GetMapping("/cart")
    public String viewCart(Principal principal, Model model) {
        String userId = shopService.getUserId(principal);
        if (userId == null) return "redirect:/login";

        shopService.populateUserHeaderData(userId, model);

        Cart cart = shopService.getCartByUserId(userId);
        double total = shopService.calculateCartTotal(cart);
        int cartCount = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();

        model.addAttribute("cart", cart);
        model.addAttribute("total", total);
        model.addAttribute("cartCount", cartCount);

        return "cart";
    }

    @PostMapping("/cart/remove")
    public String removeCartItem(@RequestParam("productId") String productId,
                                 @RequestParam("size") String size,
                                 Principal principal) {
        String userId = shopService.getUserId(principal);
        if (userId != null) {
            shopService.removeProductFromCart(userId, productId, size);
        }
        return "redirect:/cart";
    }

    // ---------------- THÊM MỚI: Mở trang điền thông tin thanh toán ----------------
    @GetMapping("/checkout")
    public String showCheckoutPage(Principal principal, Model model) {
        String userId = shopService.getUserId(principal);
        if (userId == null) return "redirect:/login";

        shopService.populateUserHeaderData(userId, model); // Tính năng cũ: Giữ nguyên Header

        Cart cart = shopService.getCartByUserId(userId);
        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        model.addAttribute("cart", cart);
        model.addAttribute("total", shopService.calculateCartTotal(cart));
        return "checkout"; // Sẽ tạo file checkout.html ở bước sau
    }

    // ---------------- CẬP NHẬT: Xử lý lưu đơn hàng khi bấm Đặt hàng ----------------
    @PostMapping("/checkout")
    public String processCheckout(@RequestParam("receiverName") String receiverName,
                                  @RequestParam("phone") String phone,
                                  @RequestParam("address") String address,
                                  @RequestParam("paymentMethod") String paymentMethod,
                                  Principal principal) {
        String userId = shopService.getUserId(principal);
        if (userId == null) return "redirect:/login";

        // Truyền thêm dữ liệu người nhận vào Service
        Order savedOrder = shopService.processCheckout(userId, receiverName, phone, address, paymentMethod);
        if (savedOrder == null) {
            return "redirect:/cart";
        }

        // Tính năng cũ: Chuyển hướng sang trang báo thành công
        return "redirect:/order-success/" + savedOrder.getId();
    }

    @GetMapping("/order-success/{orderId}")
    public String showOrderSuccess(@PathVariable("orderId") String orderId, Principal principal, Model model) {
        String userId = shopService.getUserId(principal);
        if (userId == null) return "redirect:/login";

        shopService.populateUserHeaderData(userId, model);

        Order order = shopService.getOrderForUser(orderId, userId);
        if (order == null) {
            return "redirect:/shop";
        }

        model.addAttribute("order", order);
        return "order-success";
    }

    @GetMapping("/orders")
    public String showOrderHistory(Principal principal, Model model) {
        String userId = shopService.getUserId(principal);
        if (userId == null) return "redirect:/login";

        shopService.populateUserHeaderData(userId, model);

        List<Order> orders = shopService.getUserOrderHistory(userId);
        model.addAttribute("orders", orders);

        return "orders";
    }

    @GetMapping("/shop/add-test-products")
    public String addTestProducts() {
        shopService.generateTestProducts(5);
        return "redirect:/shop";
    }
    @PostMapping("/orders/action")
    public String processOrderAction(@RequestParam("orderId") String orderId,
                                     @RequestParam("action") String action,
                                     Principal principal) {
        String userId = shopService.getUserId(principal);
        if (userId != null) {
            shopService.handleUserOrderAction(userId, orderId, action);
        }
        return "redirect:/orders"; // Load lại trang lịch sử đơn hàng
    }
}