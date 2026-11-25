package com.ucop.service.impl;

import com.ucop.dto.PaymentCalculationDTO;
import com.ucop.entity.*;
import com.ucop.enums.*;
import com.ucop.repository.*;
import com.ucop.service.CartService;
import com.ucop.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final AccountRepository accountRepository;
    private final ItemRepository itemRepository;
    private final PromotionRepository promotionRepository;
    private final StockService stockService;

    public CartServiceImpl(CartRepository cartRepository,
                           AccountRepository accountRepository,
                           ItemRepository itemRepository,
                           PromotionRepository promotionRepository,
                           StockService stockService) {
        this.cartRepository = cartRepository;
        this.accountRepository = accountRepository;
        this.itemRepository = itemRepository;
        this.promotionRepository = promotionRepository;
        this.stockService = stockService;
    }

    @Override
    public Cart getOrCreateCart(Long accountId) {
        return cartRepository.findByAccountId(accountId).orElseGet(() -> {
            Account acc = accountRepository.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản!"));
            Cart c = new Cart();
            c.setAccount(acc);
            return cartRepository.save(c);
        });
    }

    @Override
    public void addItem(Long accountId, Long itemId, int quantity) {
        if (quantity <= 0) quantity = 1;
        Cart cart = getOrCreateCart(accountId);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm!"));

        if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) {
            throw new IllegalStateException("Sản phẩm này hiện ngừng kinh doanh!");
        }

        CartItem existing = cart.getItems().stream()
                .filter(ci -> ci.getItem().getId().equals(itemId))
                .findFirst().orElse(null);

        int desiredQty = (existing != null ? existing.getQuantity() : 0) + quantity;
        int available = stockService.getAvailableStock(itemId);
        if (desiredQty > available) {
            throw new IllegalStateException("Tồn kho khả dụng chỉ còn " + available + " " + item.getUnit() + "!");
        }

        if (existing != null) {
            existing.setQuantity(desiredQty);
            existing.setUnitPrice(item.getPrice());
        } else {
            CartItem ci = new CartItem();
            ci.setCart(cart);
            ci.setItem(item);
            ci.setQuantity(quantity);
            ci.setUnitPrice(item.getPrice());
            cart.getItems().add(ci);
        }

        recalculateCartPromotions(cart);
        cartRepository.save(cart);
    }

    @Override
    public void updateItemQuantity(Long accountId, Long cartItemId, int quantity) {
        Cart cart = getOrCreateCart(accountId);
        CartItem target = cart.getItems().stream()
                .filter(ci -> ci.getId().equals(cartItemId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ!"));

        if (quantity <= 0) {
            cart.getItems().remove(target);
        } else {
            int available = stockService.getAvailableStock(target.getItem().getId());
            if (quantity > available) {
                throw new IllegalStateException("Tồn kho khả dụng chỉ còn " + available + "!");
            }
            target.setQuantity(quantity);
        }

        recalculateCartPromotions(cart);
        cartRepository.save(cart);
    }

    @Override
    public void removeItem(Long accountId, Long cartItemId) {
        Cart cart = getOrCreateCart(accountId);
        cart.getItems().removeIf(ci -> ci.getId().equals(cartItemId));
        recalculateCartPromotions(cart);
        cartRepository.save(cart);
    }

    @Override
    public String applyPromotion(Long accountId, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã giảm giá!");
        }
        Cart cart = getOrCreateCart(accountId);
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Giỏ hàng đang trống!");
        }

        Promotion promo = promotionRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã khuyến mãi '" + code + "' không tồn tại!"));

        if (!promo.isValidNow()) {
            throw new IllegalArgumentException("Mã khuyến mãi đã hết hạn hoặc hết lượt sử dụng!");
        }

        BigDecimal subtotal = cart.getSubtotal();
        if (promo.getMinOrderAmount() != null && subtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu (" + promo.getMinOrderAmount().toPlainString() + " đ)!");
        }

        if (promo.getScope() == PromotionScope.ITEM && promo.getTargetItem() != null) {
            boolean containsTarget = cart.getItems().stream()
                    .anyMatch(ci -> ci.getItem().getId().equals(promo.getTargetItem().getId()));
            if (!containsTarget) {
                throw new IllegalArgumentException("Mã này chỉ áp dụng cho sản phẩm: " + promo.getTargetItem().getName());
            }
        }

        cart.setPromotionCode(promo.getCode());
        recalculateCartPromotions(cart);
        cartRepository.save(cart);
        return "Áp dụng mã " + promo.getCode() + " thành công!";
    }

    @Override
    public void removePromotion(Long accountId) {
        Cart cart = getOrCreateCart(accountId);
        cart.setPromotionCode(null);
        cart.setItemDiscount(BigDecimal.ZERO);
        cart.setCartDiscount(BigDecimal.ZERO);
        cartRepository.save(cart);
    }

    private void recalculateCartPromotions(Cart cart) {
        cart.setItemDiscount(BigDecimal.ZERO);
        cart.setCartDiscount(BigDecimal.ZERO);

        if (cart.getPromotionCode() == null || cart.getItems().isEmpty()) {
            cart.setPromotionCode(null);
            return;
        }

        Promotion promo = promotionRepository.findByCodeIgnoreCase(cart.getPromotionCode()).orElse(null);
        if (promo == null || !promo.isValidNow()) {
            cart.setPromotionCode(null);
            return;
        }

        BigDecimal subtotal = cart.getSubtotal();
        if (promo.getMinOrderAmount() != null && subtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            cart.setPromotionCode(null);
            return;
        }

        if (promo.getScope() == PromotionScope.ITEM && promo.getTargetItem() != null) {
            BigDecimal itemDisc = BigDecimal.ZERO;
            for (CartItem ci : cart.getItems()) {
                if (ci.getItem().getId().equals(promo.getTargetItem().getId())) {
                    BigDecimal lineSub = ci.getLineSubtotal();
                    if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
                        itemDisc = itemDisc.add(lineSub.multiply(promo.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP));
                    } else {
                        itemDisc = itemDisc.add(promo.getDiscountValue().multiply(BigDecimal.valueOf(ci.getQuantity())).min(lineSub));
                    }
                }
            }
            if (itemDisc.compareTo(BigDecimal.ZERO) == 0) {
                cart.setPromotionCode(null);
            } else {
                cart.setItemDiscount(itemDisc);
            }
        } else {
            BigDecimal cartDisc;
            if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
                cartDisc = subtotal.multiply(promo.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            } else {
                cartDisc = promo.getDiscountValue().min(subtotal);
            }
            cart.setCartDiscount(cartDisc);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentCalculationDTO calculateCartSummary(Cart cart) {
        double totalWeight = cart.getItems().stream()
                .filter(ci -> ci.getItem().getItemType() == ItemType.PHYSICAL)
                .mapToDouble(ci -> (ci.getItem().getWeight() != null ? ci.getItem().getWeight().doubleValue() : 0.5) * ci.getQuantity())
                .sum();
        boolean hasPhysical = cart.getItems().stream()
                .anyMatch(ci -> ci.getItem().getItemType() == ItemType.PHYSICAL);

        return PaymentCalculationDTO.calculate(
                cart.getSubtotal(),
                cart.getItemDiscount(),
                cart.getCartDiscount(),
                totalWeight,
                hasPhysical,
                PaymentMethod.BANK_TRANSFER,
                BigDecimal.ZERO,
                false
        );
    }
}
