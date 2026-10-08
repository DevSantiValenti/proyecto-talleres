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
public class OrdenTrabajoServiceImpl implements IOrdenTrabajoService {

    private final IOrdenTrabajoRepository repository;
    private final IVehiculoRepository vehiculos;
    private final IServicioCatalogoRepository catalogos;
    private final IEmpleadoRepository empleados;
    private final IOrdenServicioRepository servicios;
    private final IRepuestoRepository repuestos;
    private final IPagoRepository pagos;
    private final IHistorialRepository historial;
    private final IPresupuestoRepository presupuestos;
    private final IGananciaService ganancias;
    private final IEventoService eventos;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Value("${app.base-url}")
    private String baseUrl;

    @PreAuthorize("hasAnyRole('ADMIN','OPERACION','PANTALLA')")
    @Transactional(readOnly = true)
    public List<OrdenOperacionDto> cola() {
        return repository.findAllByOrderByIngresoDesc().stream()
                .filter(o -> !cerrado(o))
                .sorted(Comparator.comparing(OrdenTrabajo::getIngreso))
                .map(this::operacion)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<OrdenAdminDto> listar() {
        return repository.findAllByOrderByIngresoDesc().stream().map(this::dto).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public OrdenAdminDto obtener(Long id) {
        return dto(repository.findById(id).orElseThrow());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public OrdenAdminDto crear(OrdenEntrada d) {
        Vehiculo v = vehiculos.bloquear(d.vehiculoId()).orElseThrow();
        exigir(
                repository.findByVehiculoIdOrderByIngresoDesc(v.getId()).stream()
                        .noneMatch(o -> !cerrado(o)),
                "El vehículo ya tiene una orden activa");
        OrdenTrabajo o = new OrdenTrabajo();
        o.setVehiculo(v);
        o.setCliente(v.getCliente());
        o.setVehiculoActivo(v.getId());
        o.setIngreso(clock.instant());
        o.setActualizacion(clock.instant());
        o.setKilometrajeIngreso(v.getKilometraje());
        o.setMedioPrevisto(d.medioPrevisto());
        o.setNotasInternas(d.notasInternas() == null ? "" : d.notasInternas());
        repository.save(o);
        d.servicios().forEach(x -> nuevaLinea(o, x));
        guardarPresupuesto(o);
        registrar(o, "Ingreso del vehículo");
        repository.flush();
        return dto(o);
    }

    private OrdenTrabajo editar(Long id, long version) {
        var o = repository.bloquear(id).orElseThrow();
        version(o, version);
        exigir(!cerrado(o), "La orden está cerrada");
        return o;
    }

    private OrdenServicio linea(OrdenTrabajo o, Long id) {
        return o.getServicios().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private void modificable(OrdenTrabajo o) {
        exigir(
                pagos.findByOrdenId(o.getId()).isEmpty(),
                "La orden ya está pagada: conservá el cobro original y registrá la corrección"
                        + " administrativa sin modificar sus importes");
        exigir(
                o.getEstado() != EstadoTrabajo.TERMINADO,
                "Reabrí el trabajo antes de modificar su presupuesto");
    }

    private OrdenServicio nuevaLinea(OrdenTrabajo o, LineaEntrada d) {
        var c = catalogos.findById(d.catalogoId()).orElseThrow();
        exigir(c.isActivo(), "Servicio desactivado");
        OrdenServicio s = new OrdenServicio();
        s.setOrden(o);
        s.setCatalogo(c);
        s.setNombre(c.getNombre());
        s.setPrecio(dinero(d.precio()));
        s.setTipoComision(c.getTipoComision());
        s.setComision(c.getComision());
        c.getEtapas().forEach(e -> s.getEtapas().add(new Etapa(e.getNombre(), e.getEstado())));
        s.setEstado(s.getEtapas().getFirst().getEstado());
        o.getServicios().add(s);
        servicios.save(s);
        return s;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void agregarServicio(Long id, long version, LineaEntrada d) {
        var o = editar(id, version);
        modificable(o);
        nuevaLinea(o, d);
        cambiarPresupuesto(o);
        registrar(o, "Servicio agregado");
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void precio(Long id, Long sid, PrecioEntrada d) {
        var o = editar(id, d.version());
        modificable(o);
        var s = linea(o, sid);
        exigir(
                s.getEstado() != EstadoTrabajo.TERMINADO
                        && s.getEstado() != EstadoTrabajo.CANCELADO,
                "No se modifica el precio de un servicio finalizado");
        s.setPrecio(dinero(d.precio()));
        cambiarPresupuesto(o);
        registrar(o, "Precio modificado: " + d.motivo());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void repuesto(Long id, RepuestoEntrada d) {
        var o = editar(id, d.version());
        modificable(o);
        Repuesto r = new Repuesto();
        r.setOrden(o);
        r.setNombre(d.nombre());
        r.setCantidad(d.cantidad());
        r.setCosto(dinero(d.costo()));
        r.setPrecio(dinero(d.precio()));
        repuestos.save(r);
        o.getRepuestos().add(r);
        cambiarPresupuesto(o);
        registrar(o, "Repuesto agregado: " + r.getNombre());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void editarRepuesto(Long id, Long rid, RepuestoEntrada d) {
        var o = editar(id, d.version());
        modificable(o);
        var r =
                o.getRepuestos().stream()
                        .filter(x -> x.getId().equals(rid))
                        .findFirst()
                        .orElseThrow();
        String anterior =
                r.getCantidad()
                        + " × "
                        + r.getNombre()
                        + " (costo "
                        + r.getCosto()
                        + ", venta "
                        + r.getPrecio()
                        + ")";
        r.setNombre(d.nombre().trim());
        r.setCantidad(d.cantidad());
        r.setCosto(dinero(d.costo()));
        r.setPrecio(dinero(d.precio()));
        cambiarPresupuesto(o);
        registrar(
                o,
                "Repuesto modificado: "
                        + anterior
                        + " → "
                        + r.getCantidad()
                        + " × "
                        + r.getNombre()
                        + " (costo "
                        + r.getCosto()
                        + ", venta "
                        + r.getPrecio()
                        + ")");
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void resumen(Long id, Long sid, ResumenTrabajoEntrada d) {
        var o = repository.bloquear(id).orElseThrow();
        version(o, d.version());
        var s = linea(o, sid);
        exigir(
                o.getEstado() != EstadoTrabajo.CANCELADO
                        && s.getEstado() != EstadoTrabajo.CANCELADO,
                "El trabajo está cancelado");
        s.setResumenPublico(d.resumenPublico().trim());
        registrar(o, "Detalle público actualizado: " + s.getNombre());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void quitarRepuesto(Long id, Long rid, VersionEntrada d) {
        var o = editar(id, d.version());
        modificable(o);
        var r =
                o.getRepuestos().stream()
                        .filter(x -> x.getId().equals(rid))
                        .findFirst()
                        .orElseThrow();
        o.getRepuestos().remove(r);
        repuestos.delete(r);
        cambiarPresupuesto(o);
        registrar(o, "Repuesto retirado: " + r.getNombre() + ". " + d.motivo());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void aprobar(Long id, VersionEntrada d) {
        var o = editar(id, d.version());
        exigir(d.motivo() != null && !d.motivo().isBlank(), "Registrá cómo aprobó el cliente");
        o.setVersionAprobada(o.getPresupuestoVersion());
        o.setAprobacionNota(d.motivo());
        o.setAprobacionFecha(clock.instant());
        registrar(o, "Presupuesto v" + o.getPresupuestoVersion() + " aprobado: " + d.motivo());
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERACION')")
    public void asignar(Long id, Long sid, AsignacionEntrada d) {
        var o = editar(id, d.version());
        var s = linea(o, sid);
        exigir(
                s.getEstado() != EstadoTrabajo.TERMINADO
                        && s.getEstado() != EstadoTrabajo.CANCELADO,
                "No se puede cambiar personal de un servicio finalizado");
        var equipo = empleados.findAllById(d.empleados());
        exigir(
                equipo.size() == d.empleados().size()
                        && equipo.stream().allMatch(Empleado::isActivo),
                "Empleados inexistentes o desactivados");
        s.getEmpleados().clear();
        s.getEmpleados().addAll(equipo);
        if (o.getEstado() == EstadoTrabajo.RECIBIDO) o.setEstado(EstadoTrabajo.EN_ESPERA);
        registrar(
                o,
                "Personal de "
                        + s.getNombre()
                        + ": "
                        + String.join(", ", equipo.stream().map(Empleado::getNombre).toList()));
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPERACION')")
    public void avanzar(Long id, Long sid, AvanceEntrada d) {
        var o = editar(id, d.version());
        var s = linea(o, sid);
        exigir(
                s.getEstado() != EstadoTrabajo.CANCELADO
                        && s.getEstado() != EstadoTrabajo.TERMINADO,
                "El servicio ya finalizó");
        exigir(
                Objects.equals(o.getVersionAprobada(), o.getPresupuestoVersion()),
                "El presupuesto actual requiere aprobación del administrador");
        exigir(!s.getEmpleados().isEmpty(), "Asigná los empleados antes de iniciar");
        s.setEtapaActual(s.getEtapaActual() + 1);
        s.setEstado(s.getEtapas().get(s.getEtapaActual()).getEstado());
        if (d.resumenPublico() != null && !d.resumenPublico().isBlank())
            s.setResumenPublico(d.resumenPublico());
        if (s.getEstado() == EstadoTrabajo.TERMINADO) {
            s.setFinalizacion(clock.instant());
            if (s.getResumenPublico().isBlank()) s.setResumenPublico(s.getNombre() + " realizado");
            ganancias.generar(s);
        }
        recalcular(o);
        registrar(
                o,
                "Etapa de "
                        + s.getNombre()
                        + ": "
                        + s.getEtapas().get(s.getEtapaActual()).getNombre());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void cobrar(Long id, CobroEntrada d) {
        var o = repository.bloquear(id).orElseThrow();
        if (pagos.findByOrdenId(id).isPresent()) return;
        version(o, d.version());
        exigir(!cerrado(o), "La orden está cerrada");
        exigir(
                Objects.equals(o.getVersionAprobada(), o.getPresupuestoVersion()),
                "Aprobá el presupuesto antes de cobrar");
        Pago p = new Pago();
        p.setOrden(o);
        p.setImporte(total(o));
        p.setMedio(d.medio());
        p.setFecha(clock.instant());
        p.setActor(actor());
        pagos.save(p);
        o.setMedioPrevisto(d.medio());
        registrar(o, "Pago completo registrado por " + d.medio());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void entregar(Long id, VersionEntrada d) {
        var o = editar(id, d.version());
        exigir(o.getEstado() == EstadoTrabajo.TERMINADO, "El trabajo todavía no está terminado");
        exigir(pagos.findByOrdenId(id).isPresent(), "Falta registrar el pago completo");
        o.setEstado(EstadoTrabajo.ENTREGADO);
        o.setEntrega(clock.instant());
        o.setVehiculoActivo(null);
        registrar(o, "Vehículo entregado");
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void cancelar(Long id, VersionEntrada d) {
        var o = editar(id, d.version());
        exigir(
                pagos.findByOrdenId(id).isEmpty(),
                "No se puede cancelar una orden cobrada; requiere conciliación administrativa");
        exigir(d.motivo() != null && !d.motivo().isBlank(), "Indicá el motivo");
        for (var s : o.getServicios()) {
            if (s.getEstado() == EstadoTrabajo.TERMINADO) ganancias.revertir(s, d.motivo());
            s.setEstado(EstadoTrabajo.CANCELADO);
        }
        o.setEstado(EstadoTrabajo.CANCELADO);
        o.setFinalizacion(clock.instant());
        o.setVehiculoActivo(null);
        registrar(o, "Orden cancelada: " + d.motivo());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void cancelarServicio(Long id, Long sid, VersionEntrada d) {
        var o = editar(id, d.version());
        modificable(o);
        var s = linea(o, sid);
        exigir(s.getEstado() != EstadoTrabajo.CANCELADO, "Servicio ya cancelado");
        exigir(
                o.getServicios().stream()
                                .filter(x -> x.getEstado() != EstadoTrabajo.CANCELADO)
                                .count()
                        > 1,
                "Para cancelar el último servicio, cancelá la orden");
        exigir(d.motivo() != null && !d.motivo().isBlank(), "Indicá el motivo");
        if (s.getEstado() == EstadoTrabajo.TERMINADO) ganancias.revertir(s, d.motivo());
        s.setEstado(EstadoTrabajo.CANCELADO);
        cambiarPresupuesto(o);
        recalcular(o);
        registrar(o, "Servicio cancelado: " + s.getNombre() + ". " + d.motivo());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void reabrir(Long id, Long sid, VersionEntrada d) {
        var o = editar(id, d.version());
        var s = linea(o, sid);
        exigir(
                s.getEstado() == EstadoTrabajo.TERMINADO,
                "Solo se reabre un servicio terminado y no entregado");
        exigir(d.motivo() != null && !d.motivo().isBlank(), "Indicá el motivo");
        ganancias.revertir(s, d.motivo());
        s.setCiclo(s.getCiclo() + 1);
        s.setEtapaActual(0);
        s.setEstado(EstadoTrabajo.EN_ESPERA);
        s.setFinalizacion(null);
        o.setFinalizacion(null);
        recalcular(o);
        registrar(o, "Servicio reabierto: " + d.motivo());
    }

    private void recalcular(OrdenTrabajo o) {
        var activos =
                o.getServicios().stream()
                        .filter(s -> s.getEstado() != EstadoTrabajo.CANCELADO)
                        .toList();
        if (!activos.isEmpty()
                && activos.stream().allMatch(s -> s.getEstado() == EstadoTrabajo.TERMINADO)) {
            o.setEstado(EstadoTrabajo.TERMINADO);
            o.setFinalizacion(clock.instant());
        } else if (activos.stream()
                .anyMatch(
                        s ->
                                s.getEstado() == EstadoTrabajo.EN_PROCESO
                                        || s.getEstado() == EstadoTrabajo.TERMINADO))
            o.setEstado(EstadoTrabajo.EN_PROCESO);
        else o.setEstado(EstadoTrabajo.EN_ESPERA);
    }

    private BigDecimal total(OrdenTrabajo o) {
        return o.getServicios().stream()
                .filter(s -> s.getEstado() != EstadoTrabajo.CANCELADO)
                .map(OrdenServicio::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(
                        o.getRepuestos().stream()
                                .map(
                                        r ->
                                                r.getPrecio()
                                                        .multiply(
                                                                BigDecimal.valueOf(
                                                                        r.getCantidad())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void cambiarPresupuesto(OrdenTrabajo o) {
        o.setPresupuestoVersion(o.getPresupuestoVersion() + 1);
        o.setVersionAprobada(null);
        o.setAprobacionFecha(null);
        o.setAprobacionNota(null);
        guardarPresupuesto(o);
    }

    private void guardarPresupuesto(OrdenTrabajo o) {
        Presupuesto p = new Presupuesto();
        p.setOrden(o);
        p.setNumero(o.getPresupuestoVersion());
        p.setTotal(total(o));
        p.setFecha(clock.instant());
        p.setActor(actor());
        p.setDetalle(
                String.join(
                                "\n",
                                o.getServicios().stream()
                                        .map(
                                                s ->
                                                        s.getNombre()
                                                                + " | "
                                                                + s.getPrecio()
                                                                + " | "
                                                                + s.getEstado())
                                        .toList())
                        + "\n"
                        + String.join(
                                "\n",
                                o.getRepuestos().stream()
                                        .map(
                                                r ->
                                                        r.getCantidad()
                                                                + " x "
                                                                + r.getNombre()
                                                                + " | "
                                                                + r.getPrecio())
                                        .toList()));
        presupuestos.save(p);
    }

    private void registrar(OrdenTrabajo o, String accion) {
        var ahora = clock.instant();
        o.setActualizacion(
                o.getActualizacion() != null && !ahora.isAfter(o.getActualizacion())
                        ? o.getActualizacion().plusNanos(1000)
                        : ahora);
        Historial h = new Historial();
        h.setOrden(o);
        h.setFecha(clock.instant());
        h.setActor(actor());
        h.setAccion(accion);
        historial.save(h);
        eventos.publicar(o.getVehiculo().getId(), o.getId());
    }

    private EmpleadoDto emp(Empleado e) {
        return new EmpleadoDto(e.getId(), e.getNombre(), e.isActivo());
    }

    private OrdenOperacionDto operacion(OrdenTrabajo o) {
        return new OrdenOperacionDto(
                o.getId(),
                o.getVersion(),
                o.getVehiculo().getId(),
                o.getVehiculo().getPatente(),
                nombre(o.getVehiculo()),
                o.getEstado(),
                o.getIngreso(),
                o.getActualizacion(),
                o.getServicios().stream()
                        .map(
                                s ->
                                        new ServicioOperacionDto(
                                                s.getId(),
                                                s.getNombre(),
                                                s.getEstado(),
                                                s.getEstado() == EstadoTrabajo.CANCELADO
                                                        ? "Cancelado"
                                                        : s.getEtapas()
                                                                .get(s.getEtapaActual())
                                                                .getNombre(),
                                                s.getEtapaActual(),
                                                s.getEtapas().size(),
                                                s.getEmpleados().stream().map(this::emp).toList(),
                                                s.getResumenPublico(),
                                                Objects.equals(
                                                        o.getVersionAprobada(),
                                                        o.getPresupuestoVersion())))
                        .toList());
    }

    private OrdenAdminDto dto(OrdenTrabajo o) {
        Vehiculo v = o.getVehiculo();
        Cliente c = o.getCliente();
        var pago =
                pagos.findByOrdenId(o.getId())
                        .map(p -> new PagoDto(p.getImporte(), p.getMedio(), p.getFecha()))
                        .orElse(null);
        return new OrdenAdminDto(
                o.getId(),
                o.getVersion(),
                new VehiculoDto(
                        v.getId(),
                        v.getPatente(),
                        v.getTipo(),
                        v.getMarca(),
                        v.getModelo(),
                        v.getCliente() == null ? null : v.getCliente().getId(),
                        v.getKilometraje(),
                        v.getKilometrajeFecha(),
                        baseUrl + "/vehiculo/" + v.getTokenPublico()),
                c == null
                        ? null
                        : new ClienteDto(
                                c.getId(),
                                c.getNombre(),
                                c.getTelefono(),
                                c.isConsentimientoWhatsapp()),
                o.getEstado(),
                o.getIngreso(),
                o.getFinalizacion(),
                o.getNotasInternas(),
                o.getPresupuestoVersion(),
                o.getVersionAprobada(),
                o.getAprobacionNota(),
                o.getMedioPrevisto(),
                o.getServicios().stream()
                        .map(
                                s ->
                                        new ServicioAdminDto(
                                                s.getId(),
                                                s.getNombre(),
                                                s.getEstado(),
                                                s.getPrecio(),
                                                s.getTipoComision(),
                                                s.getComision(),
                                                s.getEmpleados().stream().map(this::emp).toList(),
                                                s.getEtapas().get(s.getEtapaActual()).getNombre(),
                                                s.getResumenPublico()))
                        .toList(),
                o.getRepuestos().stream()
                        .map(
                                r ->
                                        new RepuestoDto(
                                                r.getId(),
                                                r.getNombre(),
                                                r.getCantidad(),
                                                r.getCosto(),
                                                r.getPrecio()))
                        .toList(),
                total(o),
                pago,
                historial.findByOrdenIdOrderByFechaAsc(o.getId()).stream()
                        .map(h -> new HistorialDto(h.getFecha(), h.getActor(), h.getAccion()))
                        .toList(),
                presupuestos.findByOrdenIdOrderByNumeroDesc(o.getId()).stream()
                        .map(
                                p ->
                                        new PresupuestoDto(
                                                p.getNumero(),
                                                p.getDetalle(),
                                                p.getTotal(),
                                                p.getFecha(),
                                                p.getActor()))
                        .toList());
    }
}
