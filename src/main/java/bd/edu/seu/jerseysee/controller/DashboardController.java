package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.service.DashboardService;
import bd.edu.seu.jerseysee.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardController(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User currentUser = userService.getRequiredByEmail(authentication.getName());
        model.addAttribute("dashboard", dashboardService.summary(currentUser));
        return "dashboard/index";
    }
}
