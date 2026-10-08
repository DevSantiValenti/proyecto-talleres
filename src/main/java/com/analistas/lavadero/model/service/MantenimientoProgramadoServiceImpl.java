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
public class MantenimientoProgramadoServiceImpl implements IMantenimientoProgramadoService {
    private final IMantenimientoProgramadoRepository repository;
    private final IVehiculoRepository vehiculos;
    private final IOrdenServicioRepository servicios;
    private final IRecordatorioRepository recordatorios;
    private final IEventoService eventos;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<MantenimientoDto> listar() {
        return repository.findAll().stream().map(this::dto).toList();
    }

    public MantenimientoDto guardar(Long id, MantenimientoEntrada d, Long version) {
        exigir(
                d.fechaObjetivo() != null || d.kilometrajeObjetivo() != null,
                "Indicá fecha o kilometraje objetivo");
        var v = vehiculos.bloquear(d.vehiculoId()).orElseThrow();
        MantenimientoProgramado m =
                id == null ? new MantenimientoProgramado() : repository.bloquear(id).orElseThrow();
        if (id != null) {
            version(m, version);
            exigir(
                    m.getEstado() == EstadoMantenimiento.PENDIENTE,
                    "El mantenimiento ya está cerrado");
            exigir(m.getVehiculo().getId().equals(v.getId()), "No se puede cambiar de vehículo");
            invalidar(m);
            m.setRevision(m.getRevision() + 1);
        }
        if (d.origenId() != null) {
            var s = servicios.findById(d.origenId()).orElseThrow();
            exigir(
                    s.getOrden().getVehiculo().getId().equals(v.getId())
                            && s.getEstado() == EstadoTrabajo.TERMINADO,
                    "El origen debe ser un trabajo de taller terminado del mismo vehículo");
            m.setOrigen(s);
        } else m.setOrigen(null);
        if (d.avisos())
            exigir(
                    v.getCliente() != null
                            && v.getCliente().isConsentimientoWhatsapp()
                            && v.getCliente().getTelefono() != null
                            && !v.getCliente().getTelefono().isBlank(),
                    "Para activar avisos, asociá un cliente con teléfono y consentimiento");
        m.setVehiculo(v);
        m.setDestinatario(v.getCliente());
        m.setDescripcion(d.descripcion());
        m.setFechaObjetivo(d.fechaObjetivo());
        m.setKilometrajeObjetivo(d.kilometrajeObjetivo());
        m.setAnticipacionDias(d.anticipacionDias());
        m.setAvisos(d.avisos());
        repository.saveAndFlush(m);
        eventos.publicar(v.getId(), null);
        return dto(m);
    }

    public void realizar(Long id, CumplimientoEntrada d) {
        var m = repository.bloquear(id).orElseThrow();
        version(m, d.version());
        exigir(m.getEstado() == EstadoMantenimiento.PENDIENTE, "El mantenimiento ya está cerrado");
        var s = servicios.findById(d.servicioId()).orElseThrow();
        exigir(
                s.getEstado() == EstadoTrabajo.TERMINADO
                        && s.getOrden().getVehiculo().getId().equals(m.getVehiculo().getId()),
                "Seleccioná un servicio de taller terminado del mismo vehículo");
        m.setRealizadoCon(s);
        m.setEstado(EstadoMantenimiento.REALIZADO);
        invalidar(m);
        eventos.publicar(m.getVehiculo().getId(), null);
    }

    public void cancelar(Long id, VersionEntrada d) {
        var m = repository.bloquear(id).orElseThrow();
        version(m, d.version());
        exigir(m.getEstado() == EstadoMantenimiento.PENDIENTE, "El mantenimiento ya está cerrado");
        m.setEstado(EstadoMantenimiento.CANCELADO);
        invalidar(m);
        eventos.publicar(m.getVehiculo().getId(), null);
    }

    private void invalidar(MantenimientoProgramado m) {
        recordatorios.findByMantenimientoId(m.getId()).stream()
                .filter(r -> r.getEstado() == EstadoRecordatorio.PENDIENTE)
                .forEach(
                        r -> {
                            r.setEstado(EstadoRecordatorio.CANCELADO);
                            r.setDetalle("Programación reemplazada o cerrada");
                        });
    }

    private MantenimientoDto dto(MantenimientoProgramado m) {
        return new MantenimientoDto(
                m.getId(),
                m.getVersion(),
                m.getVehiculo().getId(),
                m.getVehiculo().getPatente() + " · " + nombre(m.getVehiculo()),
                m.getDescripcion(),
                m.getFechaObjetivo(),
                m.getKilometrajeObjetivo(),
                ReglasMantenimiento.estado(m, LocalDate.now(clock)),
                m.getEstado(),
                m.getVehiculo().getKilometraje() != null,
                m.getAnticipacionDias(),
                m.isAvisos(),
                m.getOrigen() == null ? null : m.getOrigen().getId());
    }
}
