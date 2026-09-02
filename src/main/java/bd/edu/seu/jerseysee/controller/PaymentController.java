package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.dto.PaymentConfirmationDTO;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.service.PaymentService;
import bd.edu.seu.jerseysee.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff/payments")
@PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'ADMIN')")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    public PaymentController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @GetMapping
    public String payments(Authentication authentication, Model model) {
        model.addAttribute("payments", paymentService.listFor(currentUser(authentication)));
        model.addAttribute("confirmation", new PaymentConfirmationDTO());
        return "staff/payments/list";
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('CASHIER', 'ADMIN')")
    public String confirm(@PathVariable Long id, @Valid @ModelAttribute("confirmation") PaymentConfirmationDTO confirmation,
            BindingResult bindingResult, Authentication authentication, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("payments", paymentService.listFor(currentUser(authentication)));
            return "staff/payments/list";
        }
        try {
            paymentService.confirm(id, confirmation, currentUser(authentication));
            return "redirect:/staff/payments?confirmed";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("payment", exception.getMessage());
            model.addAttribute("payments", paymentService.listFor(currentUser(authentication)));
            return "staff/payments/list";
        }
    }

    private User currentUser(Authentication authentication) {
        return userService.getRequiredByEmail(authentication.getName());
    }
}
