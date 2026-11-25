package com.ucop.controller;

import com.ucop.enums.ItemType;
import com.ucop.service.CatalogService;
import com.ucop.service.StockService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;
    private final CatalogService catalogService;

    public StockController(StockService stockService, CatalogService catalogService) {
        this.stockService = stockService;
        this.catalogService = catalogService;
    }

    @GetMapping
    public String viewStock(Model model) {
        model.addAttribute("warehouses", stockService.findAllWarehouses());
        model.addAttribute("stockItems", stockService.findAllStockItems());
        model.addAttribute("lowStockItems", stockService.findLowStockItems());
        model.addAttribute("physicalItems", catalogService.searchItems(null, ItemType.PHYSICAL, null));
        return "stock/list";
    }

    @PostMapping("/warehouse/save")
    public String saveWarehouse(@RequestParam(value = "id", required = false) Long id,
                                @RequestParam("code") String code,
                                @RequestParam("name") String name,
                                @RequestParam(value = "location", required = false) String location,
                                HttpSession session,
                                RedirectAttributes ra) {
        try {
            String user = (String) session.getAttribute("username");
            stockService.saveWarehouse(id, code, name, location, user);
            ra.addFlashAttribute("success", "Lưu thông tin kho hàng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock";
    }

    @PostMapping("/adjust")
    public String adjustStock(@RequestParam("warehouseId") Long warehouseId,
                              @RequestParam("itemId") Long itemId,
                              @RequestParam("deltaOnHand") int deltaOnHand,
                              @RequestParam(value = "lowStockThreshold", defaultValue = "5") int lowStockThreshold,
                              HttpSession session,
                              RedirectAttributes ra) {
        try {
            String user = (String) session.getAttribute("username");
            stockService.adjustStock(warehouseId, itemId, deltaOnHand, lowStockThreshold, user);
            ra.addFlashAttribute("success", "Cập nhật số lượng tồn kho thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock";
    }
}
