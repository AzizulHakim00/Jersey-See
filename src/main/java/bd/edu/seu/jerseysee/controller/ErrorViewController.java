package bd.edu.seu.jerseysee.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class ErrorViewController {

    @GetMapping("/access-denied")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accessDenied() {
        return "error/403";
    }
}
