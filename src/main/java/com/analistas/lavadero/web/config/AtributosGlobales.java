package com.analistas.lavadero.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class AtributosGlobales {
    @Value("${app.nombre}")
    private String negocio;

    @ModelAttribute("negocio")
    public String negocio() {
        return negocio;
    }

    @ModelAttribute("usuarioActual")
    public String usuarioActual(Authentication a) {
        return a == null ? "Invitado" : a.getName();
    }

    @ModelAttribute("rol")
    public String rol(Authentication a) {
        return a == null
                ? "PUBLICO"
                : a.getAuthorities().stream()
                        .map(x -> x.getAuthority().replace("ROLE_", ""))
                        .findFirst()
                        .orElse("PUBLICO");
    }
}
