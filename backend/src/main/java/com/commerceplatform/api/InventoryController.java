package com.commerceplatform.api;

import com.commerceplatform.domain.Product;
import com.commerceplatform.domain.StockMovement;
import com.commerceplatform.repository.StockMovementRepository;
import com.commerceplatform.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
public class InventoryController {
    private final InventoryService inventory;
    private final StockMovementRepository movements;
    public InventoryController(InventoryService inventory, StockMovementRepository movements) {
        this.inventory = inventory;
        this.movements = movements;
    }
    public record Adjustment(@NotNull Integer change, @NotNull @Min(0) Integer expectedStock, @NotBlank @Size(max = 160) String reason) {}
    @PostMapping("/{id}/adjustments")
    public Product adjust(@PathVariable long id, @Valid @RequestBody Adjustment input) {
        if (input.change() == 0) throw new StoreProblem(HttpStatus.BAD_REQUEST, "enterANonZeroStockAdjustment");
        return inventory.adjust(id, input.change(), input.expectedStock(), input.reason().strip());
    }
    @GetMapping("/{id}/movements")
    public List<StockMovement> history(@PathVariable long id) { return movements.findTop30ByProductIdOrderByIdDesc(id); }
}
