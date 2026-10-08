package com.analistas.lavadero.web.controller;

import com.analistas.lavadero.model.domain.ReglaNegocioException;
import com.analistas.lavadero.model.domain.dto.Dtos.ErrorDto;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class ErroresController {
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorDto> negocio(ReglaNegocioException e, HttpServletRequest req) {
        int status =
                req.getRequestURI().startsWith("/api/vehiculo/")
                        ? 410
                        : req.getRequestURI().equals("/webhooks/whatsapp") ? 403 : 409;
        return ResponseEntity.status(status).body(new ErrorDto(e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorDto> falta() {
        return ResponseEntity.status(404).body(new ErrorDto("Registro no encontrado"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> validacion(MethodArgumentNotValidException e) {
        String mensaje =
                e.getBindingResult().getFieldErrors().stream()
                        .map(x -> x.getField() + ": " + x.getDefaultMessage())
                        .findFirst()
                        .orElse("Datos inválidos");
        return ResponseEntity.badRequest().body(new ErrorDto(mensaje));
    }

    @ExceptionHandler({
        DataIntegrityViolationException.class,
        OptimisticLockingFailureException.class,
        PessimisticLockingFailureException.class
    })
    public ResponseEntity<ErrorDto> conflicto() {
        return ResponseEntity.status(409)
                .body(
                        new ErrorDto(
                                "El registro ya existe o cambió en otra pantalla. Actualizá los"
                                        + " datos."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> prohibido() {
        return ResponseEntity.status(403).body(new ErrorDto("No tenés permisos para esta acción"));
    }
}
