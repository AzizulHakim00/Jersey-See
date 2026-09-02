package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ErrorHandlingControllerAdvice {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(ResourceNotFoundException exception, Model model) {
        model.addAttribute("errorMessage", exception.getMessage());
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accessDenied(Model model) {
        model.addAttribute("errorMessage", "You do not have permission to use this area.");
        return "error/403";
    }
}
