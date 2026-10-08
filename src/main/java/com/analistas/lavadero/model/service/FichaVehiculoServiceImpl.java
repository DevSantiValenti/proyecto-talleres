package com.analistas.lavadero.model.service;

import static com.analistas.lavadero.model.domain.Reglas.*;

import com.analistas.lavadero.model.domain.*;
import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class FichaVehiculoServiceImpl implements IFichaVehiculoService {
    private final IOrdenTrabajoRepository ordenes;
    private final IVehiculoRepository vehiculos;
    private final IMantenimientoProgramadoRepository mantenimientos;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Value("${app.nombre}")
    private String negocio;

    private Vehiculo validarVehiculo(String token) {
        return vehiculos
                .findByTokenPublico(token)
                .orElseThrow(() -> new ReglaNegocioException("Ficha no disponible"));
    }

    @Transactional(readOnly = true)
    public Long vehiculoId(String token) {
        return validarVehiculo(token).getId();
    }

    @Transactional(readOnly = true)
    public FichaPublicaDto ficha(String token) {
        var v = validarVehiculo(token);
        var trabajos =
                ordenes.findByVehiculoIdOrderByIngresoDesc(v.getId()).stream()
                        .filter(
                                o ->
                                        o.getEstado() != EstadoTrabajo.CANCELADO
                                                && o.getServicios().stream()
                                                        .anyMatch(
                                                                s ->
                                                                        s.getEstado()
                                                                                != EstadoTrabajo
                                                                                        .CANCELADO))
                        .map(
                                o ->
                                        new TrabajoPublicoDto(
                                                o.getIngreso(),
                                                o.getFinalizacion(),
                                                o.getKilometrajeIngreso(),
                                                o.getEstado(),
                                                o.getServicios().stream()
                                                        .filter(
                                                                s ->
                                                                        s.getEstado()
                                                                                != EstadoTrabajo
                                                                                        .CANCELADO)
                                                        .map(this::publico)
                                                        .toList(),
                                                o.getRepuestos().stream()
                                                        .map(
                                                                r ->
                                                                        r.getCantidad()
                                                                                + " × "
                                                                                + r.getNombre())
                                                        .toList()))
                        .toList();
        var proximos =
                mantenimientos.findByVehiculoIdOrderByIdDesc(v.getId()).stream()
                        .filter(m -> m.getEstado() == EstadoMantenimiento.PENDIENTE)
                        .map(
                                m ->
                                        new MantenimientoPublicoDto(
                                                m.getDescripcion(),
                                                m.getFechaObjetivo(),
                                                m.getKilometrajeObjetivo(),
                                                ReglasMantenimiento.estado(m, LocalDate.now(clock)),
                                                v.getKilometraje() != null))
                        .toList();
        return new FichaPublicaDto(
                negocio,
                nombre(v),
                ocultarPatente(v.getPatente()),
                v.getKilometraje(),
                v.getKilometrajeFecha(),
                trabajos,
                proximos,
                clock.instant());
    }

    private ServicioPublicoDto publico(OrdenServicio s) {
        return new ServicioPublicoDto(
                s.getNombre(),
                s.getEstado() == EstadoTrabajo.CANCELADO
                        ? "Cancelado"
                        : s.getEtapas().get(s.getEtapaActual()).getNombre(),
                s.getEstado(),
                s.getEstado() == EstadoTrabajo.TERMINADO ? s.getResumenPublico() : "",
                s.getFinalizacion());
    }
}
