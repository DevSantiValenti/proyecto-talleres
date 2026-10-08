package com.analistas.lavadero.web.controller;

import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.service.*;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdministracionController {
    private final IClienteService clientes;
    private final IVehiculoService vehiculos;
    private final IEmpleadoService empleados;
    private final IUsuarioService usuarios;
    private final IServicioCatalogoService catalogo;
    private final IOrdenTrabajoService ordenes;
    private final IGananciaService ganancias;
    private final IMantenimientoProgramadoService mantenimientos;
    private final IRecordatorioService recordatorios;
    private final IWhatsAppService whatsapp;

    @GetMapping("/datos")
    public Map<String, Object> datos() {
        return Map.of(
                "clientes",
                clientes.listar(),
                "vehiculos",
                vehiculos.listar(),
                "empleados",
                empleados.listar(),
                "catalogo",
                catalogo.listar(),
                "usuarios",
                usuarios.listar(),
                "whatsappHabilitado",
                whatsapp.habilitado());
    }

    @PostMapping("/clientes")
    public ClienteDto cliente(@Valid @RequestBody ClienteEntrada d) {
        return clientes.guardar(null, d);
    }

    @PutMapping("/clientes/{id}")
    public ClienteDto cliente(@PathVariable Long id, @Valid @RequestBody ClienteEntrada d) {
        return clientes.guardar(id, d);
    }

    @PostMapping("/vehiculos")
    public VehiculoDto vehiculo(@Valid @RequestBody VehiculoEntrada d) {
        return vehiculos.guardar(null, d);
    }

    @PutMapping("/vehiculos/{id}")
    public VehiculoDto vehiculo(@PathVariable Long id, @Valid @RequestBody VehiculoEntrada d) {
        return vehiculos.guardar(id, d);
    }

    @GetMapping("/vehiculos/{id}/lecturas")
    public List<LecturaDto> lecturas(@PathVariable Long id) {
        return vehiculos.lecturas(id);
    }

    @PostMapping("/empleados")
    public EmpleadoDto empleado(@Valid @RequestBody EmpleadoEntrada d) {
        return empleados.guardar(null, d);
    }

    @PutMapping("/empleados/{id}")
    public EmpleadoDto empleado(@PathVariable Long id, @Valid @RequestBody EmpleadoEntrada d) {
        return empleados.guardar(id, d);
    }

    @PostMapping("/usuarios")
    public void usuario(@Valid @RequestBody UsuarioEntrada d) {
        usuarios.guardar(null, d);
    }

    @PutMapping("/usuarios/{id}")
    public void usuario(@PathVariable Long id, @Valid @RequestBody UsuarioEntrada d) {
        usuarios.guardar(id, d);
    }

    @PostMapping("/catalogo")
    public CatalogoDto catalogo(@Valid @RequestBody CatalogoEntrada d) {
        return catalogo.guardar(null, d);
    }

    @PutMapping("/catalogo/{id}")
    public CatalogoDto catalogo(@PathVariable Long id, @Valid @RequestBody CatalogoEntrada d) {
        return catalogo.guardar(id, d);
    }

    @GetMapping("/ordenes")
    public List<OrdenAdminDto> ordenes() {
        return ordenes.listar();
    }

    @GetMapping("/ordenes/{id}")
    public OrdenAdminDto orden(@PathVariable Long id) {
        return ordenes.obtener(id);
    }

    @PostMapping("/ordenes")
    public OrdenAdminDto orden(@Valid @RequestBody OrdenEntrada d) {
        return ordenes.crear(d);
    }

    @PostMapping("/ordenes/{id}/aprobar")
    public void aprobar(@PathVariable Long id, @Valid @RequestBody VersionEntrada d) {
        ordenes.aprobar(id, d);
    }

    @PostMapping("/ordenes/{id}/servicios")
    public void agregar(
            @PathVariable Long id, @RequestParam long version, @Valid @RequestBody LineaEntrada d) {
        ordenes.agregarServicio(id, version, d);
    }

    @PutMapping("/ordenes/{id}/servicios/{sid}/precio")
    public void precio(
            @PathVariable Long id, @PathVariable Long sid, @Valid @RequestBody PrecioEntrada d) {
        ordenes.precio(id, sid, d);
    }

    @PostMapping("/ordenes/{id}/repuestos")
    public void repuesto(@PathVariable Long id, @Valid @RequestBody RepuestoEntrada d) {
        ordenes.repuesto(id, d);
    }

    @PutMapping("/ordenes/{id}/repuestos/{rid}")
    public void editarRepuesto(
            @PathVariable Long id, @PathVariable Long rid, @Valid @RequestBody RepuestoEntrada d) {
        ordenes.editarRepuesto(id, rid, d);
    }

    @PutMapping("/ordenes/{id}/servicios/{sid}/resumen")
    public void resumen(
            @PathVariable Long id,
            @PathVariable Long sid,
            @Valid @RequestBody ResumenTrabajoEntrada d) {
        ordenes.resumen(id, sid, d);
    }

    @PostMapping("/ordenes/{id}/repuestos/{rid}/quitar")
    public void quitar(
            @PathVariable Long id, @PathVariable Long rid, @Valid @RequestBody VersionEntrada d) {
        ordenes.quitarRepuesto(id, rid, d);
    }

    @PostMapping("/ordenes/{id}/cobrar")
    public void cobrar(@PathVariable Long id, @Valid @RequestBody CobroEntrada d) {
        ordenes.cobrar(id, d);
    }

    @PostMapping("/ordenes/{id}/entregar")
    public void entregar(@PathVariable Long id, @Valid @RequestBody VersionEntrada d) {
        ordenes.entregar(id, d);
    }

    @PostMapping("/ordenes/{id}/cancelar")
    public void cancelar(@PathVariable Long id, @Valid @RequestBody VersionEntrada d) {
        ordenes.cancelar(id, d);
    }

    @PostMapping("/ordenes/{id}/servicios/{sid}/reabrir")
    public void reabrir(
            @PathVariable Long id, @PathVariable Long sid, @Valid @RequestBody VersionEntrada d) {
        ordenes.reabrir(id, sid, d);
    }

    @PostMapping("/ordenes/{id}/servicios/{sid}/cancelar")
    public void cancelarServicio(
            @PathVariable Long id, @PathVariable Long sid, @Valid @RequestBody VersionEntrada d) {
        ordenes.cancelarServicio(id, sid, d);
    }

    @GetMapping("/ganancias")
    public List<GananciaDto> ganancias() {
        return ganancias.listar();
    }

    @GetMapping("/liquidaciones")
    public List<LiquidacionDto> liquidaciones() {
        return ganancias.liquidaciones();
    }

    @PostMapping("/liquidaciones")
    public LiquidacionDto liquidar(@Valid @RequestBody LiquidacionEntrada d) {
        return ganancias.liquidar(d);
    }

    @PostMapping("/ganancias/ajustes")
    public void ajustar(@Valid @RequestBody AjusteEntrada d) {
        ganancias.ajustar(d);
    }

    @GetMapping("/mantenimientos")
    public List<MantenimientoDto> mantenimientos() {
        return mantenimientos.listar();
    }

    @PostMapping("/mantenimientos")
    public MantenimientoDto mantenimiento(@Valid @RequestBody MantenimientoEntrada d) {
        return mantenimientos.guardar(null, d, null);
    }

    @PutMapping("/mantenimientos/{id}")
    public MantenimientoDto mantenimiento(
            @PathVariable Long id, @Valid @RequestBody MantenimientoCambio d) {
        return mantenimientos.guardar(id, d.datos(), d.version());
    }

    @PostMapping("/mantenimientos/{id}/realizar")
    public void realizar(@PathVariable Long id, @Valid @RequestBody CumplimientoEntrada d) {
        mantenimientos.realizar(id, d);
    }

    @PostMapping("/mantenimientos/{id}/cancelar")
    public void cancelarMantenimiento(@PathVariable Long id, @Valid @RequestBody VersionEntrada d) {
        mantenimientos.cancelar(id, d);
    }

    @GetMapping("/recordatorios")
    public List<RecordatorioDto> recordatorios() {
        return recordatorios.listar();
    }
}
