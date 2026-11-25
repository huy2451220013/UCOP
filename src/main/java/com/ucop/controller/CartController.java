package com.ucop.controller;

import com.ucop.dto.PaymentCalculationDTO;
import com.ucop.entity.Account;
import com.ucop.entity.Cart;
import com.ucop.service.AccountService;
import com.ucop.service.CartService;
import com.ucop.service.PromotionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final PromotionService promotionService;
    private final AccountService accountService;

    public CartController(CartService cartService,
                          PromotionService promotionService,
                          AccountService accountService) {
        this.cartService = cartService;
        this.promotionService = promotionService;
        this.accountService = accountService;
    }

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        Cart cart = cartService.getOrCreateCart(accountId);
        PaymentCalculationDTO summary = cartService.calculateCartSummary(cart);

        Account acc = accountService.findById(accountId).orElse(null);
        String defaultAddress = (acc != null && acc.getProfile() != null && acc.getProfile().getAddress() != null)
                ? acc.getProfile().getAddress().trim() : "";

        model.addAttribute("cart", cart);
        model.addAttribute("summary", summary);
        model.addAttribute("activePromotions", promotionService.findActivePromotions());
        model.addAttribute("defaultAddress", defaultAddress);
        return "cart/view";
    }

    @GetMapping("/add")
    public String addItemGet(@RequestParam(value = "itemId", required = false) Long itemId,
                             @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                             HttpSession session,
                             RedirectAttributes ra) {
        if (itemId == null) {
            return "redirect:/cart";
        }
        return addItem(itemId, quantity, session, ra);
    }

    @PostMapping("/add")
    public String addItem(@RequestParam("itemId") Long itemId,
                          @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                          HttpSession session,
                          RedirectAttributes ra) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        try {
            cartService.addItem(accountId, itemId, quantity);
            ra.addFlashAttribute("success", "Đã thêm sản phẩm / dịch vụ vào giỏ hàng!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateQuantity(@RequestParam("cartItemId") Long cartItemId,
                                 @RequestParam("quantity") int quantity,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        try {
            cartService.updateItemQuantity(accountId, cartItemId, quantity);
            ra.addFlashAttribute("success", quantity <= 0
                    ? "Đã xóa sản phẩm khỏi giỏ hàng!"
                    : "Đã cập nhật số lượng trong giỏ hàng!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove/{id}")
    public String removeItem(@PathVariable("id") Long id,
                             HttpSession session,
                             RedirectAttributes ra) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        try {
            cartService.removeItem(accountId, id);
            ra.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ hàng!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/apply-coupon")
    public String applyCoupon(@RequestParam("code") String code,
                              HttpSession session,
                              RedirectAttributes ra) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        try {
            String message = cartService.applyPromotion(accountId, code);
            ra.addFlashAttribute("success", message);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove-coupon")
    public String removeCoupon(HttpSession session, RedirectAttributes ra) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        try {
            cartService.removePromotion(accountId);
            ra.addFlashAttribute("success", "Đã gỡ mã khuyến mãi khỏi giỏ hàng!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }
}
