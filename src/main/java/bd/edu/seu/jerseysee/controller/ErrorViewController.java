package bd.edu.seu.jerseysee.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class ErrorViewController {

    @RequestMapping("/access-denied")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accessDenied(HttpServletRequest request, Model model) {
        // Security forwards the rejected request with its original HTTP method.
        Object failure = request.getAttribute(WebAttributes.ACCESS_DENIED_403);
        model.addAttribute("formExpired", failure instanceof CsrfException);
        return "error/403";
    }
}
