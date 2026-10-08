package com.analistas.lavadero.web.controller;

import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.service.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class FichaVehiculoController {
    private final IFichaVehiculoService fichas;
    private final IEventoService eventos;

    @GetMapping("/vehiculo/{token}")
    public String ficha(@PathVariable String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("modo", "vehiculo");
        return "publico";
    }

    @GetMapping("/api/vehiculo/{token}")
    @ResponseBody
    public FichaPublicaDto historial(@PathVariable String token) {
        return fichas.ficha(token);
    }

    @GetMapping(value = "/api/vehiculo/{token}/eventos", produces = "text/event-stream")
    @ResponseBody
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter fichaEventos(
            @PathVariable String token) {
        return eventos.suscribir("vehiculo:" + fichas.vehiculoId(token));
    }
}
