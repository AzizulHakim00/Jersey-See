package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.cart.ShoppingCart;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.JerseyEdition;
import bd.edu.seu.jerseysee.model.enums.KitType;
import bd.edu.seu.jerseysee.model.enums.OrderStatus;
import bd.edu.seu.jerseysee.model.enums.PaymentMethod;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.model.enums.SleeveType;
import bd.edu.seu.jerseysee.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserService userService;

    public GlobalModelAttributes(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute
    public void commonAttributes(Authentication authentication, HttpServletRequest request, Model model) {
        User currentUser = currentUser(authentication);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentRole", currentUser == null ? null : currentUser.getRole());
        model.addAttribute("cartCount", cartCount(request.getSession(false)));
        model.addAttribute("productTypes", ProductType.values());
        model.addAttribute("jerseyEditions", JerseyEdition.values());
        model.addAttribute("kitTypes", KitType.values());
        model.addAttribute("sleeveTypes", SleeveType.values());
        model.addAttribute("sizeOptions", SizeOption.values());
        model.addAttribute("printingTypes", PrintingType.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("orderStatuses", OrderStatus.values());
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.getRequiredByEmail(authentication.getName());
    }

    private int cartCount(HttpSession session) {
        if (session == null || !(session.getAttribute("shoppingCart") instanceof ShoppingCart cart)) {
            return 0;
        }
        return cart.getTotalQuantity();
    }
}
