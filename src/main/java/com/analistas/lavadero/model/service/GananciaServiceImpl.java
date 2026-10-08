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
public class GananciaServiceImpl implements IGananciaService {
    private final IGananciaRepository repository;
    private final ILiquidacionRepository liquidaciones;
    private final IEmpleadoRepository empleados;
    private final IOrdenServicioRepository servicios;
    private final Clock clock;

    public void generar(OrdenServicio s) {
        exigir(!s.getEmpleados().isEmpty(), "Asigná al menos un empleado");
        BigDecimal total =
                s.getTipoComision() == TipoComision.FIJA
                        ? s.getComision()
                        : s.getPrecio()
                                .multiply(s.getComision())
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        var equipo =
                s.getEmpleados().stream().sorted(Comparator.comparing(Empleado::getId)).toList();
        long centavos = total.movePointRight(2).longValueExact();
        long parte = centavos / equipo.size(), resto = centavos % equipo.size();
        for (int i = 0; i < equipo.size(); i++) {
            String clave =
                    "servicio:" + s.getId() + ":" + s.getCiclo() + ":" + equipo.get(i).getId();
            if (!repository.existsByClave(clave))
                guardar(
                        s,
                        equipo.get(i),
                        BigDecimal.valueOf(parte + (i < resto ? 1 : 0), 2),
                        "Servicio terminado: " + s.getNombre(),
                        clave);
        }
    }

    public void revertir(OrdenServicio s, String motivo) {
        String prefijo = "servicio:" + s.getId() + ":" + s.getCiclo() + ":";
        for (Ganancia g : repository.findByServicioId(s.getId()))
            if (g.getClave().startsWith(prefijo)
                    && !repository.existsByClave("reversa:" + g.getId()))
                guardar(
                        s,
                        g.getEmpleado(),
                        g.getImporte().negate(),
                        "Reversión: " + motivo,
                        "reversa:" + g.getId());
    }

    private void guardar(
            OrdenServicio s, Empleado e, BigDecimal importe, String concepto, String clave) {
        Ganancia g = new Ganancia();
        g.setServicio(s);
        g.setEmpleado(e);
        g.setImporte(importe);
        g.setConcepto(concepto);
        g.setClave(clave);
        g.setFecha(clock.instant());
        repository.save(g);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<GananciaDto> listar() {
        return repository.findAll().stream()
                .map(
                        g ->
                                new GananciaDto(
                                        g.getId(),
                                        g.getServicio().getId(),
                                        g.getEmpleado().getId(),
                                        g.getEmpleado().getNombre(),
                                        g.getImporte(),
                                        g.getFecha(),
                                        g.getLiquidacion() == null
                                                ? null
                                                : g.getLiquidacion().getId(),
                                        g.getConcepto()))
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<LiquidacionDto> liquidaciones() {
        return liquidaciones.findAll().stream().map(this::dto).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public LiquidacionDto liquidar(LiquidacionEntrada d) {
        exigir(!d.hasta().isBefore(d.desde()), "Período inválido");
        Empleado e = empleados.bloquear(d.empleadoId()).orElseThrow();
        var zona = clock.getZone();
        var desde = d.desde().atStartOfDay(zona).toInstant();
        var hasta = d.hasta().plusDays(1).atStartOfDay(zona).toInstant();
        var pendientes =
                repository.findByEmpleadoIdOrderByFechaDesc(e.getId()).stream()
                        .filter(
                                g ->
                                        g.getLiquidacion() == null
                                                && !g.getFecha().isBefore(desde)
                                                && g.getFecha().isBefore(hasta))
                        .toList();
        exigir(!pendientes.isEmpty(), "No hay ganancias pendientes en ese período");
        BigDecimal total =
                pendientes.stream()
                        .map(Ganancia::getImporte)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        exigir(
                total.signum() > 0,
                "El saldo a liquidar debe ser positivo; los ajustes permanecen pendientes");
        Liquidacion l = new Liquidacion();
        l.setEmpleado(e);
        l.setDesde(d.desde());
        l.setHasta(d.hasta());
        l.setImporte(total);
        l.setMedio(d.medio());
        l.setFecha(clock.instant());
        l.setActor(actor());
        liquidaciones.save(l);
        pendientes.forEach(g -> g.setLiquidacion(l));
        return dto(l);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void ajustar(AjusteEntrada d) {
        Empleado e = empleados.bloquear(d.empleadoId()).orElseThrow();
        exigir(d.importe().signum() != 0, "El ajuste no puede ser cero");
        if (repository.existsByClave("ajuste:" + d.clave())) return;
        OrdenServicio s = servicios.findById(d.servicioId()).orElseThrow();
        exigir(
                s.getEmpleados().stream().anyMatch(x -> x.getId().equals(e.getId())),
                "El empleado no participó en el servicio");
        guardar(
                s,
                e,
                d.importe().setScale(2, RoundingMode.UNNECESSARY),
                "Ajuste por " + actor() + ": " + d.motivo(),
                "ajuste:" + d.clave());
    }

    private LiquidacionDto dto(Liquidacion l) {
        return new LiquidacionDto(
                l.getId(),
                l.getEmpleado().getNombre(),
                l.getDesde(),
                l.getHasta(),
                l.getImporte(),
                l.getMedio(),
                l.getFecha());
    }
}
