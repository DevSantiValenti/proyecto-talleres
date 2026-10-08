package com.analistas.lavadero.model.service;

import static com.analistas.lavadero.model.domain.Reglas.*;

import com.analistas.lavadero.model.domain.*;
import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ClienteServiceImpl implements IClienteService {
    private final IClienteRepository repository;
    private final IRecordatorioRepository recordatorios;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ClienteDto> listar() {
        return repository.findAll().stream()
                .map(
                        e ->
                                new ClienteDto(
                                        e.getId(),
                                        e.getNombre(),
                                        e.getTelefono(),
                                        e.isConsentimientoWhatsapp()))
                .toList();
    }

    public ClienteDto guardar(Long id, ClienteEntrada d) {
        exigir(
                !d.consentimientoWhatsapp() || (d.telefono() != null && !d.telefono().isBlank()),
                "El consentimiento requiere un teléfono internacional");
        Cliente e = id == null ? new Cliente() : repository.bloquear(id).orElseThrow();
        boolean cambioTelefono = id != null && !Objects.equals(e.getTelefono(), d.telefono());
        if (id != null && (cambioTelefono || !d.consentimientoWhatsapp()))
            recordatorios.findAll().stream()
                    .filter(
                            r ->
                                    r.getDestinatario().getId().equals(id)
                                            && r.getEstado() == EstadoRecordatorio.PENDIENTE)
                    .forEach(
                            r -> {
                                r.setEstado(EstadoRecordatorio.CANCELADO);
                                r.setDetalle("Cambio de contacto o baja de consentimiento");
                            });
        if (d.consentimientoWhatsapp() && (!e.isConsentimientoWhatsapp() || cambioTelefono))
            e.setConsentimientoFecha(clock.instant());
        e.setNombre(d.nombre().trim());
        e.setTelefono(d.telefono());
        e.setConsentimientoWhatsapp(d.consentimientoWhatsapp());
        repository.save(e);
        return new ClienteDto(
                e.getId(), e.getNombre(), e.getTelefono(), e.isConsentimientoWhatsapp());
    }
}
