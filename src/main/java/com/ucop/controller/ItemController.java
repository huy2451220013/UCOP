package com.ucop.controller;

import com.ucop.dto.ItemFormDTO;
import com.ucop.entity.Item;
import com.ucop.enums.ItemType;
import com.ucop.service.CatalogService;
import com.ucop.service.StockService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/items")
public class ItemController {

    private final CatalogService catalogService;
    private final StockService stockService;

    public ItemController(CatalogService catalogService, StockService stockService) {
        this.catalogService = catalogService;
        this.stockService = stockService;
    }

    @GetMapping
    public String list(@RequestParam(value = "categoryId", required = false) Long categoryId,
                       @RequestParam(value = "itemType", required = false) ItemType itemType,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       Model model) {
        model.addAttribute("items", catalogService.searchItems(categoryId, itemType, keyword));
        model.addAttribute("categories", catalogService.findAllCategories());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedItemType", itemType);
        model.addAttribute("keyword", keyword);
        return "catalog/items";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Item item = catalogService.findItemById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm!"));
        model.addAttribute("item", item);
        model.addAttribute("availableStock", stockService.getAvailableStock(id));
        return "catalog/detail";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("itemForm", new ItemFormDTO());
        model.addAttribute("categories", catalogService.findAllCategories());
        return "catalog/item-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable("id") Long id, Model model) {
        Item it = catalogService.findItemById(id).orElseThrow();
        ItemFormDTO dto = new ItemFormDTO();
        dto.setId(it.getId());
        dto.setSku(it.getSku());
        dto.setName(it.getName());
        dto.setCategoryId(it.getCategory() != null ? it.getCategory().getId() : null);
        dto.setItemType(it.getItemType());
        dto.setUnit(it.getUnit());
        dto.setPrice(it.getPrice());
        dto.setWeight(it.getWeight());
        dto.setStatus(it.getStatus());
        dto.setDescription(it.getDescription());
        dto.setImageUrl(it.getImageUrl());

        model.addAttribute("itemForm", dto);
        model.addAttribute("categories", catalogService.findAllCategories());
        return "catalog/item-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("itemForm") ItemFormDTO form,
                       BindingResult br,
                       HttpSession session,
                       Model model,
                       RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("categories", catalogService.findAllCategories());
            return "catalog/item-form";
        }
        try {
            String user = (String) session.getAttribute("username");
            catalogService.saveItem(form, user);
            ra.addFlashAttribute("success", "Lưu sản phẩm / dịch vụ thành công!");
            return "redirect:/items";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", catalogService.findAllCategories());
            return "catalog/item-form";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id, HttpSession session, RedirectAttributes ra) {
        try {
            String user = (String) session.getAttribute("username");
            catalogService.deleteItem(id, user);
            ra.addFlashAttribute("success", "Đã xử lý xóa / ngừng kinh doanh sản phẩm!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/items";
    }

    @GetMapping("/export-csv")
    public void exportCsv(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=items.csv");
        OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        writer.write('\ufeff');
        catalogService.exportItemsCsv(writer);
    }

    @PostMapping("/import-csv")
    public String importCsv(@RequestParam("file") MultipartFile file, HttpSession session, RedirectAttributes ra) {
        try {
            String user = (String) session.getAttribute("username");
            int count = catalogService.importItemsCsv(file, user);
            ra.addFlashAttribute("success", "Đã import thành công " + count + " sản phẩm từ file CSV!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi import CSV: " + e.getMessage());
        }
        return "redirect:/items";
    }
}
