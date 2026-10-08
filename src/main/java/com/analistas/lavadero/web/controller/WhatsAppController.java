package com.analistas.lavadero.web.controller;

import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.service.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/webhooks/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {
    private final IWhatsAppService whatsapp;
    private final IRecordatorioService recordatorios;

    @GetMapping
    public ResponseEntity<String> verificar(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        return "subscribe".equals(mode) && whatsapp.verificarToken(token)
                ? ResponseEntity.ok(challenge)
                : ResponseEntity.status(403).build();
    }

    @PostMapping
    public void recibir(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String firma,
            @RequestBody byte[] cuerpo) {
        recordatorios.webhook(cuerpo, firma);
    }
}
