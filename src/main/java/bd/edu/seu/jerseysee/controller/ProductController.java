package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.dto.CatalogFilter;
import bd.edu.seu.jerseysee.dto.AddToCartDTO;
import bd.edu.seu.jerseysee.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/catalog")
    public String catalog(@Valid @ModelAttribute("filter") CatalogFilter filter, BindingResult bindingResult,
            @PageableDefault(size = 12) Pageable pageable, Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                model.addAttribute("products", productService.search(filter, pageable));
            } catch (IllegalArgumentException exception) {
                bindingResult.reject("filter", exception.getMessage());
            }
        }
        model.addAttribute("categories", productService.categories());
        return "catalog/list";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getActive(id));
        model.addAttribute("addToCart", new AddToCartDTO());
        return "catalog/detail";
    }
}
