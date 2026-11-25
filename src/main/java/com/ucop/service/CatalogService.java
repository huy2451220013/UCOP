package com.ucop.service;

import com.ucop.dto.ItemFormDTO;
import com.ucop.entity.Category;
import com.ucop.entity.Item;
import com.ucop.enums.ItemType;
import org.springframework.web.multipart.MultipartFile;

import java.io.Writer;
import java.util.List;
import java.util.Optional;

public interface CatalogService {
    // Categories (Multi-level)
    List<Category> findAllCategories();
    Optional<Category> findCategoryById(Long id);
    Category saveCategory(Long id, String name, String description, Long parentId, boolean active, String performedBy);
    void deleteCategory(Long id, String performedBy);
    void exportCategoriesCsv(Writer writer) throws Exception;
    int importCategoriesCsv(MultipartFile file, String performedBy) throws Exception;

    // Items
    List<Item> searchItems(Long categoryId, ItemType itemType, String keyword);
    List<Item> findActiveItems();
    Optional<Item> findItemById(Long id);
    Item saveItem(ItemFormDTO dto, String performedBy);
    void deleteItem(Long id, String performedBy);
    void exportItemsCsv(Writer writer) throws Exception;
    int importItemsCsv(MultipartFile file, String performedBy) throws Exception;
}
