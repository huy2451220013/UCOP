package com.ucop.controller;

import com.ucop.entity.CartItem;
import com.ucop.repository.CartRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final CartRepository cartRepository;

    public GlobalModelAdvice(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @ModelAttribute
    public void populateUserContext(HttpSession session, Model model) {
        if (session != null) {
            Long userId = (Long) session.getAttribute("currentUserId");
            model.addAttribute("currentUserId", userId);
            model.addAttribute("currentUsername", session.getAttribute("username"));
            model.addAttribute("currentUserRole", session.getAttribute("userRole"));
            model.addAttribute("isAdmin", Boolean.TRUE.equals(session.getAttribute("isAdmin")));
            model.addAttribute("isStaff", Boolean.TRUE.equals(session.getAttribute("isStaff")));

            int cartCount = 0;
            if (userId != null) {
                try {
                    cartCount = cartRepository.findByAccountId(userId)
                            .map(c -> c.getItems().stream().mapToInt(CartItem::getQuantity).sum())
                            .orElse(0);
                } catch (Exception ignored) {
                }
            }
            model.addAttribute("cartItemCount", cartCount);
        } else {
            model.addAttribute("isAdmin", false);
            model.addAttribute("isStaff", false);
            model.addAttribute("cartItemCount", 0);
        }
    }
}
