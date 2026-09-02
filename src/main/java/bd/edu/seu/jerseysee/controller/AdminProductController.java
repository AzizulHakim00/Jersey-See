package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.dto.ProductDTO;
import bd.edu.seu.jerseysee.dto.ProductVariantDTO;
import bd.edu.seu.jerseysee.exception.InvalidFileException;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/staff/products")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String products(@PageableDefault(size = 20) Pageable pageable, Model model) {
        model.addAttribute("products", productService.listForManagement(pageable));
        return "staff/products/list";
    }

    @GetMapping("/new")
    public String newProduct(Model model) {
        model.addAttribute("product", new ProductDTO());
        addFormOptions(model);
        return "staff/products/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("product") ProductDTO product, BindingResult bindingResult,
            @RequestParam(name = "image", required = false) MultipartFile image, Model model) {
        if (bindingResult.hasErrors()) {
            addFormOptions(model);
            return "staff/products/form";
        }
        try {
            Product created = productService.create(product, image);
            return "redirect:/staff/products/" + created.getId() + "/edit?created";
        } catch (InvalidFileException exception) {
            bindingResult.reject("image", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("product", exception.getMessage());
        }
        addFormOptions(model);
        return "staff/products/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Product product = productService.getForManagement(id);
        model.addAttribute("product", productService.toDTO(product));
        model.addAttribute("persistedProduct", product);
        model.addAttribute("variant", new ProductVariantDTO());
        addFormOptions(model);
        return "staff/products/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("product") ProductDTO product,
            BindingResult bindingResult,
            @RequestParam(name = "image", required = false) MultipartFile image, Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                productService.update(id, product, image);
                return "redirect:/staff/products/" + id + "/edit?updated";
            } catch (InvalidFileException exception) {
                bindingResult.reject("image", exception.getMessage());
            } catch (IllegalArgumentException exception) {
                bindingResult.reject("product", exception.getMessage());
            }
        }
        model.addAttribute("persistedProduct", productService.getForManagement(id));
        model.addAttribute("variant", new ProductVariantDTO());
        addFormOptions(model);
        return "staff/products/form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/staff/products?deleted";
    }

    @PostMapping("/{id}/variants")
    public String addVariant(@PathVariable Long id,
            @Valid @ModelAttribute("variant") ProductVariantDTO variant, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return variantErrorForm(id, model);
        }
        try {
            productService.addVariant(id, variant);
            return "redirect:/staff/products/" + id + "/edit?variantCreated";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("variant", exception.getMessage());
            return variantErrorForm(id, model);
        }
    }

    @PostMapping("/{productId}/variants/{variantId}")
    public String updateVariant(@PathVariable Long productId, @PathVariable Long variantId,
            @Valid @ModelAttribute("variant") ProductVariantDTO variant, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return variantErrorForm(productId, model);
        }
        try {
            productService.updateVariant(productId, variantId, variant);
            return "redirect:/staff/products/" + productId + "/edit?variantUpdated";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("variant", exception.getMessage());
            return variantErrorForm(productId, model);
        }
    }

    @PostMapping("/{productId}/variants/{variantId}/delete")
    public String deleteVariant(@PathVariable Long productId, @PathVariable Long variantId) {
        productService.deleteVariant(productId, variantId);
        return "redirect:/staff/products/" + productId + "/edit?variantDeleted";
    }

    private String variantErrorForm(Long productId, Model model) {
        Product persisted = productService.getForManagement(productId);
        model.addAttribute("product", productService.toDTO(persisted));
        model.addAttribute("persistedProduct", persisted);
        addFormOptions(model);
        return "staff/products/form";
    }

    private void addFormOptions(Model model) {
        model.addAttribute("categories", productService.categories());
    }
}
