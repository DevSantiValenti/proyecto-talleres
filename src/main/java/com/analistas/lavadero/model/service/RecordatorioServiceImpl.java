package com.analistas.lavadero.model.service;

import static com.analistas.lavadero.model.domain.Reglas.*;

import com.analistas.lavadero.model.domain.*;
import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.json.JsonMapper;

import java.math.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RecordatorioServiceImpl implements IRecordatorioService {
    private final IMantenimientoProgramadoRepository mantenimientos;
    private final IRecordatorioRepository repository;
    private final IIntentoNotificacionRepository intentos;
    private final IClienteRepository clientes;
    private final IWhatsAppService whatsapp;
    private final PlatformTransactionManager txManager;
    private final Clock clock;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public void programar() {
        Instant ahora = clock.instant();
        LocalDate hoy = LocalDate.now(clock);
        // Un envío que quedó a medias tras un reinicio nunca se reintenta a ciegas.
        repository.findAll().stream()
                .filter(
                        r ->
                                r.getEstado() == EstadoRecordatorio.ENVIANDO
                                        && r.getActualizado().isBefore(ahora.minusSeconds(300)))
                .forEach(
                        r -> {
                            r.setEstado(EstadoRecordatorio.INCIERTO);
                            r.setDetalle("Envío interrumpido; verificar en Meta");
                        });
        for (var original : mantenimientos.findAll()) {
            var m = mantenimientos.bloquear(original.getId()).orElseThrow();
            if (!elegible(m)) continue;
            String estado = ReglasMantenimiento.estado(m, hoy);
            boolean vencido = estado.equals("VENCIDO") || estado.equals("VENCE_HOY");
            if (vencido) {
                repository.findByMantenimientoId(m.getId()).stream()
                        .filter(
                                r ->
                                        r.getTipo() == TipoRecordatorio.ANTICIPACION
                                                && r.getEstado() == EstadoRecordatorio.PENDIENTE)
                        .forEach(
                                r -> {
                                    r.setEstado(EstadoRecordatorio.CANCELADO);
                                    r.setDetalle("Aviso antiguo reemplazado por vencimiento");
                                });
                boolean porKm =
                        m.getKilometrajeObjetivo() != null
                                && m.getVehiculo().getKilometraje() != null
                                && m.getVehiculo().getKilometraje() >= m.getKilometrajeObjetivo();
                crear(
                        m,
                        TipoRecordatorio.VENCIMIENTO,
                        porKm
                                ? ahora
                                : m.getFechaObjetivo()
                                        .atTime(9, 0)
                                        .atZone(clock.getZone())
                                        .toInstant());
            } else if (m.getFechaObjetivo() != null
                    && !hoy.isBefore(m.getFechaObjetivo().minusDays(m.getAnticipacionDias())))
                crear(
                        m,
                        TipoRecordatorio.ANTICIPACION,
                        m.getFechaObjetivo()
                                .minusDays(m.getAnticipacionDias())
                                .atTime(9, 0)
                                .atZone(clock.getZone())
                                .toInstant());
        }
    }

    private boolean elegible(MantenimientoProgramado m) {
        return m.getEstado() == EstadoMantenimiento.PENDIENTE
                && m.isAvisos()
                && m.getDestinatario() != null
                && m.getDestinatario().isConsentimientoWhatsapp()
                && m.getDestinatario().getTelefono() != null
                && !m.getDestinatario().getTelefono().isBlank()
                && m.getVehiculo().getCliente() != null
                && m.getVehiculo().getCliente().getId().equals(m.getDestinatario().getId());
    }

    private void crear(MantenimientoProgramado m, TipoRecordatorio tipo, Instant fecha) {
        String clave = m.getId() + ":" + m.getRevision() + ":" + tipo;
        if (repository.existsByClave(clave)) return;
        Recordatorio r = new Recordatorio();
        r.setMantenimiento(m);
        r.setDestinatario(m.getDestinatario());
        r.setTipo(tipo);
        r.setRevision(m.getRevision());
        r.setProgramado(fecha);
        r.setActualizado(clock.instant());
        r.setClave(clave);
        repository.save(r);
    }

    public void despachar() {
        if (!whatsapp.habilitado()) return;
        var tx = new TransactionTemplate(txManager);
        var ids =
                tx.execute(
                        s ->
                                repository
                                        .findByEstadoAndProgramadoLessThanEqual(
                                                EstadoRecordatorio.PENDIENTE, clock.instant())
                                        .stream()
                                        .map(Recordatorio::getId)
                                        .toList());
        for (Long id : ids) {
            MensajeWhatsAppDto mensaje =
                    tx.execute(
                            status -> {
                                var r = repository.bloquear(id).orElseThrow();
                                var m = r.getMantenimiento();
                                if (r.getEstado() != EstadoRecordatorio.PENDIENTE) return null;
                                if (!elegible(m)
                                        || r.getRevision() != m.getRevision()
                                        || !r.getDestinatario()
                                                .getId()
                                                .equals(m.getDestinatario().getId())) {
                                    r.setEstado(EstadoRecordatorio.CANCELADO);
                                    return null;
                                }
                                r.setEstado(EstadoRecordatorio.ENVIANDO);
                                r.setIntentos(r.getIntentos() + 1);
                                r.setActualizado(clock.instant());
                                String objetivo =
                                        (m.getFechaObjetivo() == null
                                                        ? ""
                                                        : m.getFechaObjetivo().toString())
                                                + (m.getKilometrajeObjetivo() == null
                                                        ? ""
                                                        : " / "
                                                                + m.getKilometrajeObjetivo()
                                                                + " km");
                                return new MensajeWhatsAppDto(
                                        r.getDestinatario().getTelefono(),
                                        m.getVehiculo().getMarca()
                                                + " "
                                                + m.getVehiculo().getModelo(),
                                        m.getDescripcion(),
                                        objetivo,
                                        baseUrl + "/vehiculo/" + m.getVehiculo().getTokenPublico(),
                                        r.getClave());
                            });
            if (mensaje == null) continue;
            var resultado = whatsapp.enviar(mensaje);
            tx.executeWithoutResult(
                    status -> {
                        var r = repository.bloquear(id).orElseThrow();
                        if (resultado.proveedorId() != null)
                            r.setProveedorId(resultado.proveedorId());
                        boolean confirmado =
                                r.getEstado() == EstadoRecordatorio.ENTREGADO
                                        || r.getEstado() == EstadoRecordatorio.ACEPTADO
                                        || r.getEstado() == EstadoRecordatorio.FALLIDO;
                        if (!confirmado) {
                            r.setDetalle(resultado.detalle());
                            r.setActualizado(clock.instant());
                            switch (resultado.estado()) {
                                case "ACEPTADO" -> r.setEstado(EstadoRecordatorio.ACEPTADO);
                                case "REINTENTAR" -> {
                                    if (r.getIntentos() < 3) {
                                        r.setEstado(EstadoRecordatorio.PENDIENTE);
                                        r.setProgramado(
                                                clock.instant().plusSeconds(60L * r.getIntentos()));
                                    } else r.setEstado(EstadoRecordatorio.FALLIDO);
                                }
                                case "FALLIDO" -> r.setEstado(EstadoRecordatorio.FALLIDO);
                                default -> r.setEstado(EstadoRecordatorio.INCIERTO);
                            }
                        }
                        IntentoNotificacion i = new IntentoNotificacion();
                        i.setRecordatorio(r);
                        i.setFecha(clock.instant());
                        i.setResultado(resultado.estado() + ": " + resultado.detalle());
                        intentos.save(i);
                    });
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<RecordatorioDto> listar() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Recordatorio::getId).reversed())
                .map(
                        r ->
                                new RecordatorioDto(
                                        r.getId(),
                                        r.getMantenimiento().getVehiculo().getPatente(),
                                        r.getMantenimiento().getDescripcion(),
                                        r.getTipo(),
                                        r.getProgramado(),
                                        r.getEstado(),
                                        r.getIntentos(),
                                        r.getDetalle()))
                .toList();
    }

    @Transactional
    public void webhook(byte[] cuerpo, String firma) {
        exigir(
                cuerpo.length <= 262144 && whatsapp.verificarFirma(cuerpo, firma),
                "Firma de webhook inválida");
        var json = JsonMapper.builder().build().readTree(cuerpo);
        for (var entry : json.path("entry"))
            for (var cambio : entry.path("changes")) {
                var valor = cambio.path("value");
                for (var evento : valor.path("statuses")) {
                    String proveedorId = evento.path("id").asText("");
                    String referencia = evento.path("biz_opaque_callback_data").asText("");
                    if (proveedorId.isBlank() || proveedorId.length() > 255) continue;
                    var registro = repository.findByProveedorId(proveedorId);
                    if (registro.isEmpty() && !referencia.isBlank())
                        registro = repository.findByClave(referencia);
                    registro.ifPresent(
                            r -> {
                                if (r.getProveedorId() != null
                                        && !r.getProveedorId().equals(proveedorId)) return;
                                r.setProveedorId(proveedorId);
                                r.setActualizado(clock.instant());
                                String estado = evento.path("status").asText();
                                if (estado.equals("read") || estado.equals("delivered")) {
                                    r.setEstado(EstadoRecordatorio.ENTREGADO);
                                    r.setDetalle("Entrega confirmada por Meta");
                                } else if (estado.equals("failed")
                                        && r.getEstado() != EstadoRecordatorio.ENTREGADO) {
                                    r.setEstado(EstadoRecordatorio.FALLIDO);
                                    r.setDetalle("Meta informó un fallo de entrega");
                                } else if (estado.equals("sent")
                                        && (r.getEstado() == EstadoRecordatorio.ENVIANDO
                                                || r.getEstado() == EstadoRecordatorio.INCIERTO)) {
                                    r.setEstado(EstadoRecordatorio.ACEPTADO);
                                    r.setDetalle("Envío confirmado por webhook");
                                }
                            });
                }
                for (var mensaje : valor.path("messages")) {
                    String texto =
                            mensaje.path("text")
                                    .path("body")
                                    .asText("")
                                    .trim()
                                    .toUpperCase(Locale.ROOT);
                    if (Set.of("BAJA", "STOP", "CANCELAR").contains(texto)) {
                        String telefono = mensaje.path("from").asText("");
                        for (var c : clientes.findAll())
                            if (c.getTelefono() != null
                                    && c.getTelefono().replace("+", "").equals(telefono)) {
                                c.setConsentimientoWhatsapp(false);
                                repository.findAll().stream()
                                        .filter(
                                                r ->
                                                        r.getDestinatario()
                                                                        .getId()
                                                                        .equals(c.getId())
                                                                && r.getEstado()
                                                                        == EstadoRecordatorio
                                                                                .PENDIENTE)
                                        .forEach(
                                                r -> {
                                                    r.setEstado(EstadoRecordatorio.CANCELADO);
                                                    r.setDetalle("Baja solicitada por WhatsApp");
                                                });
                            }
                    }
                }
            }
    }
}
