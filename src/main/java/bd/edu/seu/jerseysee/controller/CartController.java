package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.cart.ShoppingCart;
import bd.edu.seu.jerseysee.dto.AddToCartDTO;
import bd.edu.seu.jerseysee.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@SessionAttributes("shoppingCart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("shoppingCart")
    public ShoppingCart shoppingCart() {
        return new ShoppingCart();
    }

    @GetMapping("/cart")
    public String cart(@ModelAttribute("shoppingCart") ShoppingCart shoppingCart, Model model) {
        model.addAttribute("addToCart", new AddToCartDTO());
        return "cart/view";
    }

    @PostMapping("/cart/items")
    public String add(@Valid @ModelAttribute("addToCart") AddToCartDTO addToCart, BindingResult bindingResult,
            @ModelAttribute("shoppingCart") ShoppingCart shoppingCart, Model model) {
        if (bindingResult.hasErrors()) {
            return cartWithForm(model);
        }
        try {
            cartService.add(shoppingCart, addToCart);
            return "redirect:/cart?added";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("cart", exception.getMessage());
            return cartWithForm(model);
        }
    }

    @PostMapping("/cart/items/{lineId}")
    public String updateQuantity(@PathVariable String lineId,
            @RequestParam @Min(value = 1, message = "Quantity must be at least 1.")
            @Max(value = 10, message = "Quantity cannot exceed 10 per cart line.") int quantity,
            @ModelAttribute("shoppingCart") ShoppingCart shoppingCart, Model model) {
        try {
            cartService.updateQuantity(shoppingCart, lineId, quantity);
            return "redirect:/cart?updated";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("cartError", exception.getMessage());
            return cartWithForm(model);
        }
    }

    @PostMapping("/cart/items/{lineId}/remove")
    public String remove(@PathVariable String lineId, @ModelAttribute("shoppingCart") ShoppingCart shoppingCart,
            Model model) {
        try {
            cartService.remove(shoppingCart, lineId);
            return "redirect:/cart?removed";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("cartError", exception.getMessage());
            return cartWithForm(model);
        }
    }

    private String cartWithForm(Model model) {
        if (!model.containsAttribute("addToCart")) {
            model.addAttribute("addToCart", new AddToCartDTO());
        }
        return "cart/view";
    }
}
