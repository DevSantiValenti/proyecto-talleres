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
public class VehiculoServiceImpl implements IVehiculoService {
    private final IVehiculoRepository repository;
    private final IOrdenTrabajoRepository ordenes;
    private final IHistorialRepository historial;
    private final IClienteRepository clientes;
    private final ILecturaKilometrajeRepository lecturas;
    private final IMantenimientoProgramadoRepository mantenimientos;
    private final IRecordatorioRepository recordatorios;
    private final IEventoService eventos;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Value("${app.base-url}")
    private String baseUrl;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<VehiculoDto> listar() {
        return repository.findAll().stream().map(this::dto).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public VehiculoDto obtener(Long id) {
        return dto(repository.findById(id).orElseThrow());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public VehiculoDto guardar(Long id, VehiculoEntrada d) {
        String p = patente(d.patente());
        exigir(p.length() >= 3, "Patente inválida");
        repository
                .findByPatente(p)
                .ifPresent(
                        v ->
                                exigir(
                                        Objects.equals(v.getId(), id),
                                        "La patente ya está registrada"));
        Vehiculo v = id == null ? new Vehiculo() : repository.bloquear(id).orElseThrow();
        Long anterior = v.getCliente() == null ? null : v.getCliente().getId();
        if (id != null && !Objects.equals(anterior, d.clienteId())) {
            mantenimientos.findByVehiculoIdOrderByIdDesc(id).stream()
                    .filter(m -> m.getEstado() == EstadoMantenimiento.PENDIENTE)
                    .forEach(
                            m -> {
                                m.setAvisos(false);
                                m.setDestinatario(null);
                                m.setRevision(m.getRevision() + 1);
                                recordatorios.findByMantenimientoId(m.getId()).stream()
                                        .filter(r -> r.getEstado() == EstadoRecordatorio.PENDIENTE)
                                        .forEach(
                                                r -> {
                                                    r.setEstado(EstadoRecordatorio.CANCELADO);
                                                    r.setDetalle("Cambio de propietario");
                                                });
                            });
        }
        v.setPatente(p);
        v.setTipo(d.tipo());
        v.setMarca(d.marca().trim());
        v.setModelo(d.modelo().trim());
        v.setCliente(d.clienteId() == null ? null : clientes.findById(d.clienteId()).orElseThrow());
        if (v.getTokenPublico() == null) v.setTokenPublico(token());
        repository.save(v);
        eventos.publicar(v.getId(), null);
        return dto(v);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERACION')")
    public void kilometraje(Long id, LecturaEntrada d) {
        Vehiculo v = repository.bloquear(id).orElseThrow();
        exigir(!d.correccion() || admin(), "Solo el administrador puede corregir lecturas");
        exigir(
                d.correccion()
                        || v.getKilometraje() == null
                        || d.kilometraje() >= v.getKilometraje(),
                "El kilometraje no puede disminuir sin una corrección administrativa");
        v.setKilometraje(d.kilometraje());
        v.setKilometrajeFecha(clock.instant());
        LecturaKilometraje l = new LecturaKilometraje();
        l.setVehiculo(v);
        l.setKilometraje(d.kilometraje());
        l.setFecha(clock.instant());
        l.setActor(actor());
        l.setMotivo((d.correccion() ? "CORRECCIÓN: " : "") + d.motivo());
        lecturas.save(l);
        for (var orden : ordenes.findByVehiculoIdOrderByIngresoDesc(id))
            if (!cerrado(orden) && orden.getEstado() != EstadoTrabajo.TERMINADO) {
                var o = ordenes.bloquear(orden.getId()).orElseThrow();
                o.setKilometrajeIngreso(d.kilometraje());
                o.setActualizacion(clock.instant());
                Historial h = new Historial();
                h.setOrden(o);
                h.setFecha(clock.instant());
                h.setActor(actor());
                h.setAccion("Kilometraje registrado: " + d.kilometraje() + " km. " + d.motivo());
                historial.save(h);
            }
        eventos.publicar(id, null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<LecturaDto> lecturas(Long id) {
        return lecturas.findByVehiculoIdOrderByFechaDesc(id).stream()
                .map(
                        l ->
                                new LecturaDto(
                                        l.getId(),
                                        l.getKilometraje(),
                                        l.getFecha(),
                                        l.getActor(),
                                        l.getMotivo()))
                .toList();
    }

    private VehiculoDto dto(Vehiculo v) {
        return new VehiculoDto(
                v.getId(),
                v.getPatente(),
                v.getTipo(),
                v.getMarca(),
                v.getModelo(),
                v.getCliente() == null ? null : v.getCliente().getId(),
                v.getKilometraje(),
                v.getKilometrajeFecha(),
                baseUrl + "/vehiculo/" + v.getTokenPublico());
    }
}
