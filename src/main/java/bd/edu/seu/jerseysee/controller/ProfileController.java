package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.dto.ProfileDTO;
import bd.edu.seu.jerseysee.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        model.addAttribute("profile", userService.getProfile(authentication.getName()));
        return "profile/edit";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication authentication, @Valid @ModelAttribute("profile") ProfileDTO profile,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "profile/edit";
        }
        userService.updateProfile(authentication.getName(), profile);
        return "redirect:/profile?updated";
    }
}
