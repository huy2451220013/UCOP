package com.ucop.controller;

import com.ucop.service.CatalogService;
import com.ucop.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CatalogService catalogService;
    private final PromotionService promotionService;

    public HomeController(CatalogService catalogService, PromotionService promotionService) {
        this.catalogService = catalogService;
        this.promotionService = promotionService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("items", catalogService.findActiveItems());
        model.addAttribute("categories", catalogService.findAllCategories());
        model.addAttribute("promotions", promotionService.findActivePromotions());
        return "index";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
