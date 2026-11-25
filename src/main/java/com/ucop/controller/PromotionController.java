package com.ucop.controller;

import com.ucop.entity.Promotion;
import com.ucop.enums.DiscountType;
import com.ucop.enums.PromotionScope;
import com.ucop.service.CatalogService;
import com.ucop.service.PromotionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/promotions")
public class PromotionController {

    private final PromotionService promotionService;
    private final CatalogService catalogService;

    public PromotionController(PromotionService promotionService, CatalogService catalogService) {
        this.promotionService = promotionService;
        this.catalogService = catalogService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("promotions", promotionService.findAll());
        model.addAttribute("recentUsages", promotionService.findRecentUsages());
        return "promotion/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("promo", new Promotion());
        model.addAttribute("items", catalogService.findActiveItems());
        model.addAttribute("scopes", PromotionScope.values());
        model.addAttribute("discountTypes", DiscountType.values());
        return "promotion/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable("id") Long id, Model model) {
        Promotion promo = promotionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi!"));
        model.addAttribute("promo", promo);
        model.addAttribute("items", catalogService.findActiveItems());
        model.addAttribute("scopes", PromotionScope.values());
        model.addAttribute("discountTypes", DiscountType.values());
        return "promotion/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(value = "id", required = false) Long id,
                       @RequestParam("code") String code,
                       @RequestParam("scope") PromotionScope scope,
                       @RequestParam(value = "targetItemId", required = false) Long targetItemId,
                       @RequestParam("discountType") DiscountType discountType,
                       @RequestParam("discountValue") BigDecimal discountValue,
                       @RequestParam(value = "minOrderAmount", defaultValue = "0") BigDecimal minOrderAmount,
                       @RequestParam(value = "maxUsage", defaultValue = "100") Integer maxUsage,
                       @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                       @RequestParam(value = "active", defaultValue = "false") boolean active,
                       @RequestParam(value = "description", required = false) String description,
                       HttpSession session,
                       RedirectAttributes ra) {
        try {
            String user = (String) session.getAttribute("username");
            promotionService.savePromotion(id, code, scope, targetItemId, discountType,
                    discountValue, minOrderAmount, maxUsage, startDate, endDate, active, description, user);
            ra.addFlashAttribute("success", "Đã lưu mã khuyến mãi: " + code.toUpperCase());
            return "redirect:/promotions";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return id != null ? "redirect:/promotions/edit/" + id : "redirect:/promotions/add";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id, HttpSession session, RedirectAttributes ra) {
        try {
            String user = (String) session.getAttribute("username");
            promotionService.deletePromotion(id, user);
            ra.addFlashAttribute("success", "Đã xóa / ngừng kích hoạt mã khuyến mãi!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/promotions";
    }
}
