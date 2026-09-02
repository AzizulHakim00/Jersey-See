package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.dto.EmployeeDTO;
import bd.edu.seu.jerseysee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/staff/employees")
    public String employees(Model model) {
        model.addAttribute("employees", employeeService.listAll());
        return "staff/employees/list";
    }

    @GetMapping("/staff/employees/new")
    public String employeeForm(Model model) {
        model.addAttribute("employee", new EmployeeDTO());
        model.addAttribute("allowedRoles", employeeService.rolesAvailableToCurrentUser());
        return "staff/employees/form";
    }

    @PostMapping("/staff/employees")
    public String createEmployee(@Valid @ModelAttribute("employee") EmployeeDTO employee, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allowedRoles", employeeService.rolesAvailableToCurrentUser());
            return "staff/employees/form";
        }
        try {
            employeeService.create(employee);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("employee", exception.getMessage());
            model.addAttribute("allowedRoles", employeeService.rolesAvailableToCurrentUser());
            return "staff/employees/form";
        }
        return "redirect:/staff/employees?created";
    }
}
