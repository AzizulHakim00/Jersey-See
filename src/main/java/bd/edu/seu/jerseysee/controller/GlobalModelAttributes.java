package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.JerseyEdition;
import bd.edu.seu.jerseysee.model.enums.KitType;
import bd.edu.seu.jerseysee.model.enums.OrderStatus;
import bd.edu.seu.jerseysee.model.enums.PaymentMethod;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.model.enums.SleeveType;
import bd.edu.seu.jerseysee.service.CartService;
import bd.edu.seu.jerseysee.service.OrderService;
import bd.edu.seu.jerseysee.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserService userService;
    private final CartService cartService;

    public GlobalModelAttributes(UserService userService, CartService cartService) {
        this.userService = userService;
        this.cartService = cartService;
    }

    @ModelAttribute
    public void commonAttributes(Authentication authentication, Model model) {
        User currentUser = currentUser(authentication);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentRole", currentUser == null ? null : currentUser.getRole());
        model.addAttribute("cartCount", cartCount(currentUser));
        model.addAttribute("productTypes", ProductType.values());
        model.addAttribute("jerseyEditions", JerseyEdition.values());
        model.addAttribute("kitTypes", KitType.values());
        model.addAttribute("sleeveTypes", SleeveType.values());
        model.addAttribute("sizeOptions", SizeOption.values());
        model.addAttribute("printingTypes", PrintingType.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("deliveryFee", OrderService.DELIVERY_FEE);
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.getRequiredByEmail(authentication.getName());
    }

    private int cartCount(User currentUser) {
        if (currentUser == null || !currentUser.isEnabled() || currentUser.getRole() != Role.CUSTOMER) {
            return 0;
        }
        return cartService.getTotalQuantity(currentUser);
    }
}
