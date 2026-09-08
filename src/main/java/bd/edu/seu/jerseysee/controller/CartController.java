package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.dto.AddToCartDTO;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.service.CartService;
import bd.edu.seu.jerseysee.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    public CartController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    @GetMapping("/cart")
    public String cart(Authentication authentication, Model model) {
        loadCart(authentication, model);
        model.addAttribute("addToCart", new AddToCartDTO());
        return "cart/view";
    }

    @PostMapping("/cart/items")
    public String add(Authentication authentication,
            @Valid @ModelAttribute("addToCart") AddToCartDTO addToCart, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return cartWithForm(authentication, model);
        }
        try {
            cartService.add(currentUser(authentication), addToCart);
            return "redirect:/cart?added";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("cart", exception.getMessage());
            return cartWithForm(authentication, model);
        }
    }

    @PostMapping("/cart/items/{lineId}")
    public String updateQuantity(@PathVariable String lineId,
            @RequestParam @Min(value = 1, message = "Quantity must be at least 1.")
            @Max(value = 10, message = "Quantity cannot exceed 10 per cart line.") int quantity,
            Authentication authentication, Model model) {
        try {
            cartService.updateQuantity(currentUser(authentication), lineId, quantity);
            return "redirect:/cart?updated";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("cartError", exception.getMessage());
            return cartWithForm(authentication, model);
        }
    }

    @PostMapping("/cart/items/{lineId}/remove")
    public String remove(@PathVariable String lineId, Authentication authentication, Model model) {
        try {
            cartService.remove(currentUser(authentication), lineId);
            return "redirect:/cart?removed";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("cartError", exception.getMessage());
            return cartWithForm(authentication, model);
        }
    }

    private String cartWithForm(Authentication authentication, Model model) {
        loadCart(authentication, model);
        if (!model.containsAttribute("addToCart")) {
            model.addAttribute("addToCart", new AddToCartDTO());
        }
        return "cart/view";
    }

    private void loadCart(Authentication authentication, Model model) {
        model.addAttribute("shoppingCart", cartService.getCart(currentUser(authentication)));
    }

    private User currentUser(Authentication authentication) {
        return userService.getRequiredByEmail(authentication.getName());
    }
}
