package com.analistas.lavadero.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping({"/", "/home"})
    public String inicio(Authentication auth) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
            return "redirect:/admin";
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_OPERACION")))
            return "redirect:/operacion";
        return "redirect:/pantalla";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/admin", "/operacion", "/pantalla"})
    public String app(jakarta.servlet.http.HttpServletRequest request, Model model) {
        model.addAttribute(
                "modo",
                request.getRequestURI().equals("/pantalla")
                        ? "PANTALLA"
                        : request.getRequestURI().equals("/operacion") ? "OPERACION" : "ADMIN");
        return "app";
    }
}
