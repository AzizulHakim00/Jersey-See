package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.dto.EmployeeDTO;
import bd.edu.seu.jerseysee.dto.EmployeeUpdateDTO;
import bd.edu.seu.jerseysee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping("/staff/employees/{id}/edit")
    public String editEmployee(@PathVariable Long id, Model model) {
        model.addAttribute("employee", employeeService.getUpdateForm(id));
        model.addAttribute("employeeId", id);
        model.addAttribute("allowedRoles", employeeService.rolesAvailableToCurrentUser());
        return "staff/employees/edit";
    }

    @PostMapping("/staff/employees/{id}")
    public String updateEmployee(@PathVariable Long id,
            @Valid @ModelAttribute("employee") EmployeeUpdateDTO employee,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return editFormWithOptions(id, model);
        }
        try {
            employeeService.update(id, employee);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("employee", exception.getMessage());
            return editFormWithOptions(id, model);
        }
        return "redirect:/staff/employees?updated";
    }

    @PostMapping("/staff/employees/{id}/status")
    public String setEmployeeStatus(@PathVariable Long id, @RequestParam boolean active,
            RedirectAttributes redirectAttributes) {
        try {
            employeeService.setActive(id, active);
            return active
                    ? "redirect:/staff/employees?enabled"
                    : "redirect:/staff/employees?disabled";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/staff/employees?error";
        }
    }

    @PostMapping("/staff/employees/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            employeeService.delete(id);
            return "redirect:/staff/employees?deleted";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/staff/employees?error";
        }
    }

    private String editFormWithOptions(Long id, Model model) {
        model.addAttribute("employeeId", id);
        model.addAttribute("allowedRoles", employeeService.rolesAvailableToCurrentUser());
        return "staff/employees/edit";
    }
}
