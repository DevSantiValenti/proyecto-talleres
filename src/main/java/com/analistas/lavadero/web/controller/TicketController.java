package com.analistas.lavadero.web.controller;

import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.service.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TicketController {
    private final ITicketService tickets;

    @GetMapping(value = "/admin/ordenes/{id}/ticket.pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> ticket(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=orden-" + id + ".pdf")
                .body(tickets.orden(id));
    }

    @GetMapping(value = "/admin/vehiculos/{id}/qr.pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> tarjeta(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=vehiculo-" + id + ".pdf")
                .body(tickets.vehiculo(id));
    }
}
