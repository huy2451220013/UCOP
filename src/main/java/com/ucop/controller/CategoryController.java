package com.ucop.controller;

import com.ucop.entity.Category;
import com.ucop.service.CatalogService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CatalogService catalogService;

    public CategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public String list(@RequestParam(value = "editId", required = false) Long editId, Model model) {
        model.addAttribute("categories", catalogService.findAllCategories());
        if (editId != null) {
            catalogService.findCategoryById(editId).ifPresent(c -> model.addAttribute("editCategory", c));
        }
        return "catalog/categories";
    }

    @PostMapping("/save")
    public String save(@RequestParam(value = "id", required = false) Long id,
                       @RequestParam("name") String name,
                       @RequestParam(value = "description", required = false) String description,
                       @RequestParam(value = "parentId", required = false) Long parentId,
                       @RequestParam(value = "active", defaultValue = "true") boolean active,
                       HttpSession session,
                       RedirectAttributes ra) {
        try {
            String user = (String) session.getAttribute("username");
            Category saved = catalogService.saveCategory(id, name, description, parentId, active, user);
            ra.addFlashAttribute("success", "Đã lưu danh mục: " + saved.getName());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id, HttpSession session, RedirectAttributes ra) {
        try {
            String user = (String) session.getAttribute("username");
            catalogService.deleteCategory(id, user);
            ra.addFlashAttribute("success", "Đã xóa danh mục thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    @GetMapping("/export-csv")
    public void exportCsv(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=categories.csv");
        OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        writer.write('\ufeff');
        catalogService.exportCategoriesCsv(writer);
    }

    @PostMapping("/import-csv")
    public String importCsv(@RequestParam("file") MultipartFile file, HttpSession session, RedirectAttributes ra) {
        try {
            String user = (String) session.getAttribute("username");
            int count = catalogService.importCategoriesCsv(file, user);
            ra.addFlashAttribute("success", "Đã import thành công " + count + " danh mục từ CSV!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi import CSV: " + e.getMessage());
        }
        return "redirect:/categories";
    }
}
