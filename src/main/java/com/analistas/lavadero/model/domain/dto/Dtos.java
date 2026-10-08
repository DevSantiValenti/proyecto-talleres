package com.analistas.lavadero.model.domain.dto;

import com.analistas.lavadero.model.domain.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Contratos explícitos: nunca se serializan entidades persistentes. */
public final class Dtos {
    private Dtos() {}

    public record ClienteEntrada(
            @NotBlank @Size(max = 150) String nombre,
            @Pattern(regexp = "^$|\\+?[1-9][0-9]{7,14}$") String telefono,
            boolean consentimientoWhatsapp) {}

    public record ClienteDto(
            Long id, String nombre, String telefono, boolean consentimientoWhatsapp) {}

    public record EmpleadoEntrada(@NotBlank @Size(max = 150) String nombre, boolean activo) {}

    public record EmpleadoDto(Long id, String nombre, boolean activo) {}

    public record UsuarioEntrada(
            @NotBlank @Size(max = 100) String nombre,
            @Size(min = 12, max = 72) String password,
            @NotNull Rol rol,
            boolean activo) {}

    public record UsuarioDto(Long id, String nombre, Rol rol, boolean activo) {}

    public record VehiculoEntrada(
            @NotBlank @Size(max = 20) String patente,
            @NotNull TipoVehiculo tipo,
            @NotBlank @Size(max = 100) String marca,
            @NotBlank @Size(max = 100) String modelo,
            Long clienteId) {}

    public record VehiculoDto(
            Long id,
            String patente,
            TipoVehiculo tipo,
            String marca,
            String modelo,
            Long clienteId,
            Long kilometraje,
            Instant kilometrajeFecha,
            String enlace) {}

    public record LecturaEntrada(
            @PositiveOrZero long kilometraje,
            @NotBlank @Size(max = 500) String motivo,
            boolean correccion) {}

    public record LecturaDto(
            Long id, long kilometraje, Instant fecha, String actor, String motivo) {}

    public record EtapaDto(
            @NotBlank @Size(max = 100) String nombre, @NotNull EstadoTrabajo estado) {}

    public record CatalogoEntrada(
            @NotBlank @Size(max = 150) String nombre,
            @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal precio,
            @NotNull TipoComision tipoComision,
            @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal comision,
            boolean activo,
            @NotEmpty @Size(max = 20) List<@Valid EtapaDto> etapas) {}

    public record CatalogoDto(
            Long id,
            String nombre,
            BigDecimal precio,
            TipoComision tipoComision,
            BigDecimal comision,
            boolean activo,
            List<EtapaDto> etapas) {}

    public record LineaEntrada(
            @NotNull Long catalogoId,
            @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal precio) {}

    public record OrdenEntrada(
            @NotNull Long vehiculoId,
            @NotEmpty @Size(max = 30) List<@Valid LineaEntrada> servicios,
            @Size(max = 2000) String notasInternas,
            MedioPago medioPrevisto) {}

    public record VersionEntrada(long version, @Size(max = 500) String motivo) {}

    public record AsignacionEntrada(long version, @NotEmpty Set<Long> empleados) {}

    public record AvanceEntrada(long version, @Size(max = 2000) String resumenPublico) {}

    public record ResumenTrabajoEntrada(
            long version, @NotBlank @Size(max = 2000) String resumenPublico) {}

    public record PrecioEntrada(
            long version,
            @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal precio,
            @NotBlank @Size(max = 500) String motivo) {}

    public record RepuestoEntrada(
            long version,
            @NotBlank @Size(max = 150) String nombre,
            @Min(1) @Max(10000) int cantidad,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal costo,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal precio) {}

    public record CobroEntrada(long version, @NotNull MedioPago medio) {}

    public record ServicioOperacionDto(
            Long id,
            String nombre,
            EstadoTrabajo estado,
            String etapa,
            int etapaActual,
            int etapasTotal,
            List<EmpleadoDto> empleados,
            String resumenPublico,
            boolean habilitado) {}

    public record OrdenOperacionDto(
            Long id,
            long version,
            Long vehiculoId,
            String patente,
            String vehiculo,
            EstadoTrabajo estado,
            Instant ingreso,
            Instant actualizacion,
            List<ServicioOperacionDto> servicios) {}

    public record ServicioAdminDto(
            Long id,
            String nombre,
            EstadoTrabajo estado,
            BigDecimal precio,
            TipoComision tipoComision,
            BigDecimal comision,
            List<EmpleadoDto> empleados,
            String etapa,
            String resumenPublico) {}

    public record RepuestoDto(
            Long id, String nombre, int cantidad, BigDecimal costo, BigDecimal precio) {}

    public record PagoDto(BigDecimal importe, MedioPago medio, Instant fecha) {}

    public record HistorialDto(Instant fecha, String actor, String accion) {}

    public record PresupuestoDto(
            int numero, String detalle, BigDecimal total, Instant fecha, String actor) {}

    public record OrdenAdminDto(
            Long id,
            long version,
            VehiculoDto vehiculo,
            ClienteDto cliente,
            EstadoTrabajo estado,
            Instant ingreso,
            Instant finalizacion,
            String notasInternas,
            int presupuestoVersion,
            Integer versionAprobada,
            String aprobacionNota,
            MedioPago medioPrevisto,
            List<ServicioAdminDto> servicios,
            List<RepuestoDto> repuestos,
            BigDecimal total,
            PagoDto pago,
            List<HistorialDto> historial,
            List<PresupuestoDto> presupuestos) {}

    public record GananciaDto(
            Long id,
            Long servicioId,
            Long empleadoId,
            String empleado,
            BigDecimal importe,
            Instant fecha,
            Long liquidacionId,
            String concepto) {}

    public record LiquidacionEntrada(
            @NotNull Long empleadoId,
            @NotNull LocalDate desde,
            @NotNull LocalDate hasta,
            @NotNull MedioPago medio) {}

    public record LiquidacionDto(
            Long id,
            String empleado,
            LocalDate desde,
            LocalDate hasta,
            BigDecimal importe,
            MedioPago medio,
            Instant fecha) {}

    public record AjusteEntrada(
            @NotNull Long servicioId,
            @NotNull Long empleadoId,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal importe,
            @NotBlank @Size(max = 500) String motivo,
            @NotBlank @Size(max = 80) String clave) {}

    public record MantenimientoEntrada(
            @NotNull Long vehiculoId,
            Long origenId,
            @NotBlank @Size(max = 300) String descripcion,
            LocalDate fechaObjetivo,
            @Positive Long kilometrajeObjetivo,
            @Min(0) @Max(365) int anticipacionDias,
            boolean avisos) {}

    public record MantenimientoCambio(long version, @Valid @NotNull MantenimientoEntrada datos) {}

    public record CumplimientoEntrada(long version, @NotNull Long servicioId) {}

    public record MantenimientoDto(
            Long id,
            long version,
            Long vehiculoId,
            String vehiculo,
            String descripcion,
            LocalDate fechaObjetivo,
            Long kilometrajeObjetivo,
            String estado,
            EstadoMantenimiento estadoAdministrativo,
            boolean kilometrajeVerificado,
            int anticipacionDias,
            boolean avisos,
            Long origenId) {}

    public record RecordatorioDto(
            Long id,
            String vehiculo,
            String descripcion,
            TipoRecordatorio tipo,
            Instant programado,
            EstadoRecordatorio estado,
            int intentos,
            String detalle) {}

    public record ServicioPublicoDto(
            String nombre,
            String etapa,
            EstadoTrabajo estado,
            String resumen,
            Instant finalizacion) {}

    public record TrabajoPublicoDto(
            Instant ingreso,
            Instant finalizacion,
            Long kilometraje,
            EstadoTrabajo estado,
            List<ServicioPublicoDto> servicios,
            List<String> repuestos) {}

    public record MantenimientoPublicoDto(
            String descripcion,
            LocalDate fechaObjetivo,
            Long kilometrajeObjetivo,
            String estado,
            boolean kilometrajeVerificado) {}

    public record FichaPublicaDto(
            String negocio,
            String vehiculo,
            String patente,
            Long kilometraje,
            Instant kilometrajeFecha,
            List<TrabajoPublicoDto> trabajos,
            List<MantenimientoPublicoDto> mantenimientos,
            Instant actualizacion) {}

    public record ResultadoEnvioDto(String estado, String proveedorId, String detalle) {}

    public record MensajeWhatsAppDto(
            String telefono,
            String vehiculo,
            String mantenimiento,
            String objetivo,
            String enlace,
            String referencia) {}

    public record TicketDto(
            String negocio,
            Long orden,
            String vehiculo,
            String patente,
            Instant ingreso,
            List<String> lineas,
            BigDecimal total,
            String pago,
            String medio,
            String enlaceFicha) {}

    public record ErrorDto(String mensaje) {}
}
