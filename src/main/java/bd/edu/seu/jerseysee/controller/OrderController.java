package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.cart.ShoppingCart;
import bd.edu.seu.jerseysee.dto.CheckoutDTO;
import bd.edu.seu.jerseysee.model.CustomerOrder;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.OrderStatus;
import bd.edu.seu.jerseysee.service.InvoiceService;
import bd.edu.seu.jerseysee.service.OrderService;
import bd.edu.seu.jerseysee.service.UserService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@SessionAttributes("shoppingCart")
public class OrderController {

    private static final MediaType UTF8_TEXT = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final UserService userService;

    public OrderController(OrderService orderService, InvoiceService invoiceService, UserService userService) {
        this.orderService = orderService;
        this.invoiceService = invoiceService;
        this.userService = userService;
    }

    @ModelAttribute("shoppingCart")
    public ShoppingCart shoppingCart() {
        return new ShoppingCart();
    }

    @GetMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String checkoutForm(@ModelAttribute("shoppingCart") ShoppingCart shoppingCart, Model model) {
        model.addAttribute("checkout", new CheckoutDTO());
        return "orders/checkout";
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String checkout(Authentication authentication, @Valid @ModelAttribute("checkout") CheckoutDTO checkout,
            BindingResult bindingResult, @ModelAttribute("shoppingCart") ShoppingCart shoppingCart, Model model) {
        if (bindingResult.hasErrors()) {
            return checkoutFormWithCart(model);
        }
        try {
            User customer = userService.getRequiredByEmail(authentication.getName());
            CustomerOrder order = orderService.checkout(customer, shoppingCart, checkout);
            shoppingCart.clear();
            return "redirect:/orders/" + order.getId() + "?created";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("checkout", exception.getMessage());
            return checkoutFormWithCart(model);
        }
    }

    @GetMapping("/orders")
    public String orders(Authentication authentication, Model model) {
        model.addAttribute("orders", orderService.listFor(currentUser(authentication)));
        return "orders/list";
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("order", orderService.getAccessible(id, currentUser(authentication)));
        return "orders/detail";
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String cancel(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User customer = currentUser(authentication);
        try {
            orderService.cancel(id, customer);
            return "redirect:/orders/" + id + "?cancelled";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/orders/" + id + "?error";
        }
    }

    @GetMapping("/orders/{id}/invoice")
    public ResponseEntity<byte[]> invoice(@PathVariable Long id, Authentication authentication) {
        CustomerOrder order = orderService.getAccessible(id, currentUser(authentication));
        String filename = "jerseysee-invoice-" + id + ".txt";
        return ResponseEntity.ok()
                .contentType(UTF8_TEXT)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(invoiceService.createTextInvoice(order));
    }

    @GetMapping("/staff/orders")
    @PreAuthorize("hasAnyRole('SALESMAN', 'CASHIER', 'MANAGER', 'ADMIN')")
    public String staffOrders(Authentication authentication, Model model) {
        model.addAttribute("orders", orderService.listFor(currentUser(authentication)));
        return "staff/orders/list";
    }

    @GetMapping("/staff/orders/{id}")
    @PreAuthorize("hasAnyRole('SALESMAN', 'CASHIER', 'MANAGER', 'ADMIN')")
    public String staffOrderDetail(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("order", orderService.getAccessible(id, currentUser(authentication)));
        return "staff/orders/detail";
    }

    @PostMapping("/staff/orders/{id}/status")
    @PreAuthorize("hasAnyRole('SALESMAN', 'MANAGER', 'ADMIN')")
    public String updateStatus(@PathVariable Long id, @RequestParam OrderStatus status, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User actor = currentUser(authentication);
        try {
            orderService.updateStatus(id, status, actor);
            return "redirect:/staff/orders/" + id + "?statusUpdated";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/staff/orders/" + id + "?error";
        }
    }

    private User currentUser(Authentication authentication) {
        return userService.getRequiredByEmail(authentication.getName());
    }

    private String checkoutFormWithCart(Model model) {
        return "orders/checkout";
    }
}
