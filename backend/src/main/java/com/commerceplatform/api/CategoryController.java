package com.commerceplatform.api;

import com.commerceplatform.domain.CatalogCategory;
import com.commerceplatform.repository.CategoryRepository;
import com.commerceplatform.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CategoryController {
    private final CategoryRepository categories;
    private final CategoryService service;

    public CategoryController(CategoryRepository categories, CategoryService service) {
        this.categories = categories;
        this.service = service;
    }
    public record CategoryInput(@NotBlank @Size(max = 80) String name, @Size(max = 500) String description) {}

    @GetMapping({"/categories", "/admin/categories"})
    public List<CatalogCategory> list() { return categories.findAllByOrderByNameAsc(); }
    @PostMapping("/admin/categories") @ResponseStatus(HttpStatus.CREATED)
    public CatalogCategory create(@Valid @RequestBody CategoryInput input) { return service.create(input.name(), input.description()); }
    @PutMapping("/admin/categories/{id}")
    public CatalogCategory edit(@PathVariable long id, @Valid @RequestBody CategoryInput input) { return service.edit(id, input.name(), input.description()); }
    @DeleteMapping("/admin/categories/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }
}
