package com.ucop.service.impl;

import com.ucop.entity.Item;
import com.ucop.entity.StockItem;
import com.ucop.entity.Warehouse;
import com.ucop.enums.ItemType;
import com.ucop.repository.ItemRepository;
import com.ucop.repository.StockItemRepository;
import com.ucop.repository.WarehouseRepository;
import com.ucop.service.AuditService;
import com.ucop.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StockServiceImpl implements StockService {

    private final WarehouseRepository warehouseRepository;
    private final StockItemRepository stockItemRepository;
    private final ItemRepository itemRepository;
    private final AuditService auditService;

    public StockServiceImpl(WarehouseRepository warehouseRepository,
                            StockItemRepository stockItemRepository,
                            ItemRepository itemRepository,
                            AuditService auditService) {
        this.warehouseRepository = warehouseRepository;
        this.stockItemRepository = stockItemRepository;
        this.itemRepository = itemRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> findAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @Override
    public Warehouse saveWarehouse(Long id, String code, String name, String location, String performedBy) {
        Warehouse wh = (id != null && id > 0)
                ? warehouseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho!"))
                : new Warehouse();
        wh.setCode(code.trim().toUpperCase());
        wh.setName(name.trim());
        wh.setLocation(location);
        wh.setActive(true);
        Warehouse saved = warehouseRepository.save(wh);
        auditService.log("SAVE_WAREHOUSE", "Warehouse", saved.getId(), performedBy, null, saved.getCode(), "Lưu kho: " + saved.getName());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockItem> findAllStockItems() {
        return stockItemRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockItem> findLowStockItems() {
        return stockItemRepository.findLowStockItems();
    }

    @Override
    public StockItem adjustStock(Long warehouseId, Long itemId, int deltaOnHand, int lowStockThreshold, String performedBy) {
        Warehouse wh = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho hàng!"));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm!"));

        if (item.getItemType() == ItemType.SERVICE) {
            throw new IllegalArgumentException("Gói dịch vụ không quản lý tồn kho vật lý!");
        }

        StockItem stock = stockItemRepository.findByWarehouseIdAndItemId(warehouseId, itemId)
                .orElseGet(() -> {
                    StockItem s = new StockItem();
                    s.setWarehouse(wh);
                    s.setItem(item);
                    s.setOnHand(0);
                    s.setReserved(0);
                    return s;
                });

        int oldOnHand = stock.getOnHand();
        int newOnHand = Math.max(0, oldOnHand + deltaOnHand);
        stock.setOnHand(newOnHand);
        if (lowStockThreshold >= 0) {
            stock.setLowStockThreshold(lowStockThreshold);
        }
        stock.setUpdatedBy(performedBy);
        StockItem saved = stockItemRepository.save(stock);

        auditService.log("STOCK_CHANGE", "StockItem", saved.getId(), performedBy,
                String.valueOf(oldOnHand), String.valueOf(newOnHand),
                "Điều chỉnh tồn kho SP " + item.getSku() + " tại kho " + wh.getCode());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public int getAvailableStock(Long itemId) {
        Item item = itemRepository.findById(itemId).orElse(null);
        if (item != null && item.getItemType() == ItemType.SERVICE) {
            return 9999; // Dịch vụ luôn sẵn sàng
        }
        return stockItemRepository.getTotalAvailableByItemId(itemId);
    }

    @Override
    public void reserveStock(Long itemId, int quantity, String performedBy) {
        Item item = itemRepository.findById(itemId).orElseThrow();
        if (item.getItemType() == ItemType.SERVICE) return;

        List<StockItem> stocks = stockItemRepository.findByItemIdOrderByOnHandDesc(itemId);
        int remaining = quantity;
        for (StockItem st : stocks) {
            int avail = st.getAvailable();
            if (avail > 0) {
                int take = Math.min(avail, remaining);
                st.setReserved(st.getReserved() + take);
                st.setUpdatedBy(performedBy);
                stockItemRepository.save(st);
                remaining -= take;
                if (remaining == 0) break;
            }
        }
        if (remaining > 0) {
            throw new IllegalStateException("Sản phẩm '" + item.getName() + "' không đủ tồn kho khả dụng (thiếu " + remaining + ")!");
        }
    }

    @Override
    public void confirmDeductReservedStock(Long itemId, int quantity, String performedBy) {
        Item item = itemRepository.findById(itemId).orElseThrow();
        if (item.getItemType() == ItemType.SERVICE) return;

        List<StockItem> stocks = stockItemRepository.findByItemIdOrderByOnHandDesc(itemId);
        int remaining = quantity;
        for (StockItem st : stocks) {
            if (st.getReserved() > 0 && remaining > 0) {
                int deduct = Math.min(st.getReserved(), remaining);
                st.setReserved(st.getReserved() - deduct);
                st.setOnHand(Math.max(0, st.getOnHand() - deduct));
                st.setUpdatedBy(performedBy);
                stockItemRepository.save(st);
                remaining -= deduct;
            }
        }
    }

    @Override
    public void releaseReservedStock(Long itemId, int quantity, String performedBy) {
        Item item = itemRepository.findById(itemId).orElseThrow();
        if (item.getItemType() == ItemType.SERVICE) return;

        List<StockItem> stocks = stockItemRepository.findByItemIdOrderByOnHandDesc(itemId);
        int remaining = quantity;
        for (StockItem st : stocks) {
            if (st.getReserved() > 0 && remaining > 0) {
                int release = Math.min(st.getReserved(), remaining);
                st.setReserved(st.getReserved() - release);
                st.setUpdatedBy(performedBy);
                stockItemRepository.save(st);
                remaining -= release;
            }
        }
    }
}
