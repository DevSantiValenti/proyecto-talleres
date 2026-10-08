package com.analistas.lavadero.web.controller;

import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.service.*;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
public class OrdenTrabajoController {
    private final IOrdenTrabajoService ordenes;
    private final IEmpleadoService empleados;
    private final IVehiculoService vehiculos;
    private final IEventoService eventos;

    @GetMapping("/api/ordenes")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACION','PANTALLA')")
    public List<OrdenOperacionDto> cola() {
        return ordenes.cola();
    }

    @GetMapping("/api/empleados")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACION')")
    public List<EmpleadoDto> empleados() {
        return empleados.listar().stream().filter(EmpleadoDto::activo).toList();
    }

    @PostMapping("/api/ordenes/{id}/servicios/{sid}/asignar")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACION')")
    public void asignar(
            @PathVariable Long id,
            @PathVariable Long sid,
            @Valid @RequestBody AsignacionEntrada d) {
        ordenes.asignar(id, sid, d);
    }

    @PostMapping("/api/ordenes/{id}/servicios/{sid}/avanzar")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACION')")
    public void avanzar(
            @PathVariable Long id, @PathVariable Long sid, @Valid @RequestBody AvanceEntrada d) {
        ordenes.avanzar(id, sid, d);
    }

    @PostMapping("/api/vehiculos/{id}/kilometraje")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACION')")
    public void kilometraje(@PathVariable Long id, @Valid @RequestBody LecturaEntrada d) {
        vehiculos.kilometraje(id, d);
    }

    @GetMapping(value = "/api/eventos/cola", produces = "text/event-stream")
    @PreAuthorize("hasAnyRole('ADMIN','OPERACION','PANTALLA')")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter eventos() {
        return eventos.suscribir("cola");
    }
}
