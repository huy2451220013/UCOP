package com.ucop.service.impl;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.ucop.dto.ItemFormDTO;
import com.ucop.entity.Category;
import com.ucop.entity.Item;
import com.ucop.enums.ItemType;
import com.ucop.repository.CategoryRepository;
import com.ucop.repository.ItemRepository;
import com.ucop.service.AuditService;
import com.ucop.service.CatalogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CatalogServiceImpl implements CatalogService {

    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final AuditService auditService;

    public CatalogServiceImpl(CategoryRepository categoryRepository,
                              ItemRepository itemRepository,
                              AuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllCategories() {
        return categoryRepository.findAllByOrderByIdAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    public Category saveCategory(Long id, String name, String description, Long parentId, boolean active, String performedBy) {
        Category cat = (id != null && id > 0)
                ? categoryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"))
                : new Category();

        cat.setName(name.trim());
        cat.setDescription(description);
        cat.setActive(active);

        if (parentId != null && parentId > 0) {
            if (id != null && id.equals(parentId)) {
                throw new IllegalArgumentException("Danh mục không thể nhận chính nó làm danh mục cha!");
            }
            Category parent = categoryRepository.findById(parentId).orElse(null);
            cat.setParent(parent);
        } else {
            cat.setParent(null);
        }

        Category saved = categoryRepository.save(cat);
        auditService.log(id == null ? "CREATE_CATEGORY" : "UPDATE_CATEGORY",
                "Category", saved.getId(), performedBy, null, saved.getName(),
                "Lưu danh mục: " + saved.getFullPathName());
        return saved;
    }

    @Override
    public void deleteCategory(Long id, String performedBy) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));
        if (!cat.getItems().isEmpty() || !cat.getChildren().isEmpty()) {
            throw new IllegalStateException("Không thể xóa danh mục đang chứa danh mục con hoặc sản phẩm!");
        }
        categoryRepository.delete(cat);
        auditService.log("DELETE_CATEGORY", "Category", id, performedBy, cat.getName(), null, "Xóa danh mục");
    }

    @Override
    @Transactional(readOnly = true)
    public void exportCategoriesCsv(Writer writer) throws Exception {
        try (CSVWriter csv = new CSVWriter(writer)) {
            csv.writeNext(new String[]{"ID", "Name", "ParentName", "Description", "Active"});
            for (Category c : categoryRepository.findAllByOrderByIdAsc()) {
                csv.writeNext(new String[]{
                        String.valueOf(c.getId()),
                        c.getName(),
                        c.getParent() != null ? c.getParent().getName() : "",
                        c.getDescription() != null ? c.getDescription() : "",
                        String.valueOf(c.isActive())
                });
            }
        }
    }

    @Override
    public int importCategoriesCsv(MultipartFile file, String performedBy) throws Exception {
        int count = 0;
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> rows = reader.readAll();
            for (int i = 1; i < rows.size(); i++) {
                String[] r = rows.get(i);
                if (r.length < 2 || r[1].isBlank()) continue;
                String name = r[1].trim().replace("\uFEFF", "");
                String parentName = r.length > 2 ? r[2].trim() : "";
                String desc = r.length > 3 ? r[3].trim() : "";
                boolean active = r.length <= 4 || Boolean.parseBoolean(r[4].trim());

                Category cat = categoryRepository.findFirstByNameIgnoreCase(name).orElse(new Category());
                cat.setName(name);
                cat.setDescription(desc);
                cat.setActive(active);
                if (!parentName.isEmpty()) {
                    categoryRepository.findFirstByNameIgnoreCase(parentName).ifPresent(cat::setParent);
                }
                categoryRepository.save(cat);
                count++;
            }
        }
        auditService.log("IMPORT_CSV", "Category", null, performedBy, null, String.valueOf(count), "Import CSV Danh mục");
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> searchItems(Long categoryId, ItemType itemType, String keyword) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Long catId = (categoryId != null && categoryId > 0) ? categoryId : null;
        return itemRepository.searchItems(catId, itemType, kw);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> findActiveItems() {
        return itemRepository.findByStatusOrderByIdDesc("ACTIVE");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Item> findItemById(Long id) {
        return itemRepository.findById(id);
    }

    @Override
    public Item saveItem(ItemFormDTO dto, String performedBy) {
        boolean isNew = (dto.getId() == null || dto.getId() <= 0);
        Item item = isNew
                ? new Item()
                : itemRepository.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm!"));

        BigDecimal oldPrice = item.getPrice();

        String sku = (dto.getSku() == null || dto.getSku().isBlank())
                ? "UCOP-" + (System.currentTimeMillis() % 1000000)
                : dto.getSku().trim().toUpperCase();

        if (isNew && itemRepository.existsBySkuIgnoreCase(sku)) {
            throw new IllegalArgumentException("Mã SKU '" + sku + "' đã tồn tại!");
        }

        Category cat = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không hợp lệ!"));

        item.setSku(sku);
        item.setName(dto.getName().trim());
        item.setCategory(cat);
        item.setItemType(dto.getItemType());
        item.setUnit(dto.getUnit().trim());
        item.setPrice(dto.getPrice());
        item.setWeight(dto.getItemType() == ItemType.SERVICE ? BigDecimal.ZERO : dto.getWeight());
        item.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        item.setDescription(dto.getDescription());
        if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            item.setImageUrl(dto.getImageUrl().trim());
        } else if (isNew) {
            item.setImageUrl(null);
        }

        if (isNew) {
            item.setCreatedBy(performedBy);
        }
        item.setUpdatedBy(performedBy);

        Item saved = itemRepository.save(item);

        if (isNew) {
            auditService.log("CREATE_ITEM", "Item", saved.getId(), performedBy,
                    null, saved.getPrice().toPlainString(), "Thêm sản phẩm/dịch vụ: " + saved.getSku() + " - " + saved.getName());
        } else if (oldPrice != null && oldPrice.compareTo(saved.getPrice()) != 0) {
            auditService.log("PRICE_CHANGE", "Item", saved.getId(), performedBy,
                    oldPrice.toPlainString(), saved.getPrice().toPlainString(),
                    "Thay đổi giá SP " + saved.getSku());
        } else {
            auditService.log("UPDATE_ITEM", "Item", saved.getId(), performedBy,
                    null, saved.getName(), "Cập nhật thông tin SP " + saved.getSku());
        }
        return saved;
    }

    @Override
    public void deleteItem(Long id, String performedBy) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm!"));
        try {
            itemRepository.delete(item);
            itemRepository.flush();
            auditService.log("DELETE_ITEM", "Item", id, performedBy, item.getSku(), null, "Xóa SP: " + item.getName());
        } catch (Exception e) {
            // Soft-deactivate if referenced by existing orders
            item.setStatus("INACTIVE");
            itemRepository.save(item);
            auditService.log("DEACTIVATE_ITEM", "Item", id, performedBy, "ACTIVE", "INACTIVE",
                    "Chuyển trạng thái ngừng kinh doanh do SP đã phát sinh đơn hàng: " + item.getSku());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void exportItemsCsv(Writer writer) throws Exception {
        try (CSVWriter csv = new CSVWriter(writer)) {
            csv.writeNext(new String[]{"SKU", "Name", "Category", "ItemType", "Unit", "Price", "WeightKg", "Status", "Description"});
            for (Item it : itemRepository.findAll()) {
                csv.writeNext(new String[]{
                        it.getSku(),
                        it.getName(),
                        it.getCategory() != null ? it.getCategory().getName() : "",
                        it.getItemType().name(),
                        it.getUnit(),
                        it.getPrice().toPlainString(),
                        it.getWeight() != null ? it.getWeight().toPlainString() : "0",
                        it.getStatus(),
                        it.getDescription() != null ? it.getDescription() : ""
                });
            }
        }
    }

    @Override
    public int importItemsCsv(MultipartFile file, String performedBy) throws Exception {
        int count = 0;
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String[]> rows = reader.readAll();
            for (int i = 1; i < rows.size(); i++) {
                String[] r = rows.get(i);
                if (r.length < 6 || r[0].isBlank()) continue;
                String sku = r[0].trim().replace("\uFEFF", "").toUpperCase();
                String name = r[1].trim();
                String catName = r[2].trim();
                ItemType type = "SERVICE".equalsIgnoreCase(r[3].trim()) ? ItemType.SERVICE : ItemType.PHYSICAL;
                String unit = r[4].trim().isEmpty() ? "Chiếc" : r[4].trim();
                BigDecimal price = new BigDecimal(r[5].trim());
                BigDecimal weight = (r.length > 6 && !r[6].trim().isEmpty()) ? new BigDecimal(r[6].trim()) : BigDecimal.valueOf(0.5);
                String status = (r.length > 7 && !r[7].trim().isEmpty()) ? r[7].trim().toUpperCase() : "ACTIVE";
                String desc = (r.length > 8) ? r[8].trim() : "";

                Item item = itemRepository.findBySkuIgnoreCase(sku).orElse(new Item());
                item.setSku(sku);
                item.setName(name);
                if (!catName.isEmpty()) {
                    categoryRepository.findFirstByNameIgnoreCase(catName).ifPresent(item::setCategory);
                }
                item.setItemType(type);
                item.setUnit(unit);
                item.setPrice(price);
                item.setWeight(weight);
                item.setStatus(status);
                item.setDescription(desc);
                item.setUpdatedBy(performedBy);
                if (item.getCreatedBy() == null) item.setCreatedBy(performedBy);
                itemRepository.save(item);
                count++;
            }
        }
        auditService.log("IMPORT_CSV", "Item", null, performedBy, null, String.valueOf(count), "Import CSV Sản phẩm");
        return count;
    }
}
