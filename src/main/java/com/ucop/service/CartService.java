package com.ucop.service;

import com.ucop.dto.PaymentCalculationDTO;
import com.ucop.entity.Cart;

public interface CartService {
    Cart getOrCreateCart(Long accountId);
    void addItem(Long accountId, Long itemId, int quantity);
    void updateItemQuantity(Long accountId, Long cartItemId, int quantity);
    void removeItem(Long accountId, Long cartItemId);
    String applyPromotion(Long accountId, String code);
    void removePromotion(Long accountId);
    PaymentCalculationDTO calculateCartSummary(Cart cart);
}
