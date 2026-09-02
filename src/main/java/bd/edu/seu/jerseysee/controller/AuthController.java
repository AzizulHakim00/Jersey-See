package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.dto.RegistrationDTO;
import bd.edu.seu.jerseysee.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registrationForm(@ModelAttribute("registration") RegistrationDTO registration) {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registration") RegistrationDTO registration, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(registration);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("registration", exception.getMessage());
            return "auth/register";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
}
