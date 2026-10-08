package com.analistas.lavadero.model.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.analistas.lavadero.model.domain.*;
import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.repository.*;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.*;
import java.time.*;
import java.util.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
@Import(FlujoIntegracionTest.Config.class)
public class FlujoIntegracionTest {
    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RelojPrueba relojPrueba() {
            return new RelojPrueba();
        }
    }

    @Autowired protected RelojPrueba clock;
    @Autowired protected IClienteService clientes;
    @Autowired protected IVehiculoService vehiculos;
    @Autowired protected IEmpleadoService empleados;
    @Autowired protected IServicioCatalogoService catalogo;
    @Autowired protected IOrdenTrabajoService ordenes;
    @Autowired protected IGananciaService ganancias;
    @Autowired protected IMantenimientoProgramadoService mantenimientos;
    @Autowired protected IRecordatorioService recordatorios;
    @Autowired protected IFichaVehiculoService fichas;
    @Autowired protected ITicketService tickets;
    @Autowired protected IRecordatorioRepository recordatorioRepo;
    @Autowired protected IOrdenTrabajoRepository ordenRepo;
    @Autowired protected IMantenimientoProgramadoRepository mantenimientoRepo;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected IWhatsAppService whatsapp;

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected IUsuarioService usuarios;
    @Autowired protected org.springframework.security.core.session.SessionRegistry sesiones;

    @BeforeEach
    void limpiar() {
        jdbc.update("delete from usuario where nombre <> 'admin'");
        for (String t :
                List.of(
                        "intento_notificacion",
                        "recordatorio",
                        "mantenimiento_programado",
                        "ganancia",
                        "liquidacion",
                        "historial",
                        "pago",
                        "presupuesto",
                        "servicio_empleado",
                        "orden_servicio_etapa",
                        "repuesto",
                        "orden_servicio",
                        "orden_trabajo",
                        "servicio_catalogo_etapa",
                        "servicio_catalogo",
                        "lectura_kilometraje",
                        "vehiculo",
                        "cliente",
                        "empleado")) jdbc.update("delete from " + t);
        clock.fijar(Instant.parse("2026-10-07T15:00:00Z"));
        reset(whatsapp);
    }

    protected VehiculoDto vehiculo() {
        var c =
                clientes.guardar(
                        null, new ClienteEntrada("Cliente privado", "+5493515551234", true));
        return vehiculos.guardar(
                null, new VehiculoEntrada("ab 123 cd", TipoVehiculo.MOTO, "Honda", "Wave", c.id()));
    }

    protected CatalogoDto servicio(TipoComision comision, BigDecimal valor) {
        return catalogo.guardar(
                null,
                new CatalogoEntrada(
                        "Service ágil",
                        new BigDecimal("100.00"),
                        comision,
                        valor,
                        true,
                        List.of(
                                new EtapaDto("En espera", EstadoTrabajo.EN_ESPERA),
                                new EtapaDto("Trabajando", EstadoTrabajo.EN_PROCESO),
                                new EtapaDto("Terminado", EstadoTrabajo.TERMINADO))));
    }

    protected OrdenAdminDto orden(VehiculoDto v, CatalogoDto s) {
        return ordenes.crear(
                new OrdenEntrada(
                        v.id(),
                        List.of(new LineaEntrada(s.id(), new BigDecimal("100.00"))),
                        "NOTA INTERNA SECRETA",
                        null));
    }

    protected OrdenAdminDto ordenAprobada(VehiculoDto v, CatalogoDto s) {
        var o = orden(v, s);
        ordenes.aprobar(o.id(), new VersionEntrada(o.version(), "Cliente aceptó"));
        return ordenes.obtener(o.id());
    }

    protected long version(Long id) {
        return ordenes.obtener(id).version();
    }

    protected void asignar(OrdenAdminDto o, int cantidad) {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < cantidad; i++)
            ids.add(empleados.guardar(null, new EmpleadoEntrada("Empleado " + i, true)).id());
        ordenes.asignar(
                o.id(), o.servicios().getFirst().id(), new AsignacionEntrada(version(o.id()), ids));
    }

    protected void terminar(OrdenAdminDto o) {
        var sid = o.servicios().getFirst().id();
        ordenes.avanzar(
                o.id(), sid, new AvanceEntrada(version(o.id()), "Cambio de aceite y control"));
        ordenes.avanzar(
                o.id(), sid, new AvanceEntrada(version(o.id()), "Cambio de aceite y control"));
    }

    protected String token(String enlace) {
        return enlace.substring(enlace.lastIndexOf('/') + 1);
    }

    @Test
    void tallerCompletoComisionExactaYLiquidacionUnica() {
        var o = ordenAprobada(vehiculo(), servicio(TipoComision.FIJA, new BigDecimal("10.00")));
        asignar(o, 3);
        terminar(o);
        assertThat(ordenes.obtener(o.id()).estado()).isEqualTo(EstadoTrabajo.TERMINADO);
        assertThat(ganancias.listar())
                .extracting(GananciaDto::importe)
                .containsExactlyInAnyOrder(
                        new BigDecimal("3.34"), new BigDecimal("3.33"), new BigDecimal("3.33"));
        assertThatThrownBy(() -> ordenes.entregar(o.id(), new VersionEntrada(version(o.id()), "")))
                .hasMessageContaining("pago");
        ordenes.cobrar(o.id(), new CobroEntrada(version(o.id()), MedioPago.EFECTIVO));
        ordenes.cobrar(o.id(), new CobroEntrada(0, MedioPago.EFECTIVO));
        ordenes.entregar(o.id(), new VersionEntrada(version(o.id()), "Retirado"));
        var g = ganancias.listar().getFirst();
        var d =
                new LiquidacionEntrada(
                        g.empleadoId(),
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31),
                        MedioPago.EFECTIVO);
        ganancias.liquidar(d);
        assertThatThrownBy(() -> ganancias.liquidar(d)).hasMessageContaining("No hay ganancias");
        assertThat(ordenes.cola()).isEmpty();
    }

    @Test
    void tallerExigeAprobacionYRepuestosNoGeneranComision() {
        var o = orden(vehiculo(), servicio(TipoComision.PORCENTAJE, new BigDecimal("30.00")));
        asignar(o, 1);
        assertThatThrownBy(() -> terminar(o)).hasMessageContaining("aprobación");
        ordenes.repuesto(
                o.id(),
                new RepuestoEntrada(
                        version(o.id()), "Aceite", 2, new BigDecimal("40"), new BigDecimal("80")));
        var nuevo = ordenes.obtener(o.id());
        assertThat(nuevo.presupuestoVersion()).isEqualTo(2);
        assertThat(nuevo.total()).isEqualByComparingTo("260");
        assertThat(nuevo.presupuestos()).hasSize(2);
        ordenes.aprobar(o.id(), new VersionEntrada(version(o.id()), "Aceptó por teléfono"));
        terminar(o);
        assertThat(ganancias.listar().getFirst().importe()).isEqualByComparingTo("30");
    }

    @Test
    void editaRepuestosVersionaPresupuestoYConservaCobro() throws Exception {
        var o = orden(vehiculo(), servicio(TipoComision.FIJA, BigDecimal.ONE));
        ordenes.repuesto(
                o.id(),
                new RepuestoEntrada(
                        version(o.id()), "Aceite", 1, new BigDecimal("40"), new BigDecimal("80")));
        ordenes.aprobar(o.id(), new VersionEntrada(version(o.id()), "Aceptado"));
        var rid = ordenes.obtener(o.id()).repuestos().getFirst().id();
        long anterior = version(o.id());
        mvc.perform(
                        put("/api/admin/ordenes/" + o.id() + "/repuestos/" + rid)
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        """
                                        {"version":%d,"nombre":"Aceite y filtro","cantidad":2,"costo":45,"precio":90}
                                        """
                                                .formatted(anterior)))
                .andExpect(status().isOk());
        org.springframework.security.core.context.SecurityContextHolder.setContext(
                org.springframework.security.test.context.TestSecurityContextHolder.getContext());
        var editada = ordenes.obtener(o.id());
        assertThat(editada.total()).isEqualByComparingTo("280");
        assertThat(editada.presupuestoVersion()).isEqualTo(3);
        assertThat(editada.versionAprobada()).isNull();
        assertThat(editada.presupuestos()).hasSize(3);
        assertThat(editada.historial()).anyMatch(h -> h.accion().contains("Repuesto modificado"));
        assertThatThrownBy(
                        () ->
                                ordenes.editarRepuesto(
                                        o.id(),
                                        rid,
                                        new RepuestoEntrada(
                                                anterior,
                                                "Viejo",
                                                1,
                                                BigDecimal.ONE,
                                                BigDecimal.ONE)))
                .isInstanceOf(ReglaNegocioException.class);
        mvc.perform(
                        put("/api/admin/ordenes/" + o.id() + "/repuestos/" + rid)
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        """
                                        {"version":%d,"nombre":"Inválido","cantidad":0,"costo":-1,"precio":90}
                                        """
                                                .formatted(version(o.id()))))
                .andExpect(status().isBadRequest());
        org.springframework.security.core.context.SecurityContextHolder.setContext(
                org.springframework.security.test.context.TestSecurityContextHolder.getContext());
        ordenes.aprobar(o.id(), new VersionEntrada(version(o.id()), "Aceptado"));
        ordenes.cobrar(o.id(), new CobroEntrada(version(o.id()), MedioPago.EFECTIVO));
        assertThatThrownBy(
                        () ->
                                ordenes.editarRepuesto(
                                        o.id(),
                                        rid,
                                        new RepuestoEntrada(
                                                version(o.id()),
                                                "Cambio",
                                                1,
                                                BigDecimal.ONE,
                                                BigDecimal.ONE)))
                .hasMessageContaining("pagada");
        assertThat(ordenes.obtener(o.id()).pago().importe()).isEqualByComparingTo("280");
    }

    @Test
    void repuestoNoPuedeEditarOtraOrdenYRespetaTerminada() {
        var c = servicio(TipoComision.FIJA, BigDecimal.ONE);
        var o = orden(vehiculo(), c);
        var v2 =
                vehiculos.guardar(
                        null,
                        new VehiculoEntrada("CD456EF", TipoVehiculo.AUTO, "Fiat", "Uno", null));
        var otra = orden(v2, c);
        ordenes.repuesto(
                otra.id(),
                new RepuestoEntrada(
                        version(otra.id()), "Filtro", 1, BigDecimal.ONE, BigDecimal.TEN));
        var rid = ordenes.obtener(otra.id()).repuestos().getFirst().id();
        assertThatThrownBy(
                        () ->
                                ordenes.editarRepuesto(
                                        o.id(),
                                        rid,
                                        new RepuestoEntrada(
                                                version(o.id()),
                                                "Ajeno",
                                                1,
                                                BigDecimal.ONE,
                                                BigDecimal.ONE)))
                .isInstanceOf(NoSuchElementException.class);
        asignar(otra, 1);
        ordenes.aprobar(otra.id(), new VersionEntrada(version(otra.id()), "Aceptado"));
        terminar(otra);
        assertThatThrownBy(
                        () ->
                                ordenes.editarRepuesto(
                                        otra.id(),
                                        rid,
                                        new RepuestoEntrada(
                                                version(otra.id()),
                                                "Cambio",
                                                1,
                                                BigDecimal.ONE,
                                                BigDecimal.ONE)))
                .hasMessageContaining("Reabrí");
    }

    @Test
    void detallePublicoSePuedeCompletarDespuesDeEntregarSinCambiarEconomia() throws Exception {
        var v = vehiculo();
        var o = orden(v, servicio(TipoComision.FIJA, BigDecimal.ONE));
        asignar(o, 1);
        ordenes.aprobar(o.id(), new VersionEntrada(version(o.id()), "Aceptado"));
        terminar(o);
        ordenes.cobrar(o.id(), new CobroEntrada(version(o.id()), MedioPago.EFECTIVO));
        ordenes.entregar(o.id(), new VersionEntrada(version(o.id()), "Entregado"));
        var antes = ordenes.obtener(o.id());
        var sid = o.servicios().getFirst().id();
        mvc.perform(
                        put("/api/admin/ordenes/" + o.id() + "/servicios/" + sid + "/resumen")
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        """
                                        {"version":%d,"resumenPublico":"Cambio de aceite y filtro. Control de frenos."}
                                        """
                                                .formatted(antes.version())))
                .andExpect(status().isOk());
        org.springframework.security.core.context.SecurityContextHolder.setContext(
                org.springframework.security.test.context.TestSecurityContextHolder.getContext());
        var despues = ordenes.obtener(o.id());
        assertThat(despues.estado()).isEqualTo(EstadoTrabajo.ENTREGADO);
        assertThat(despues.total()).isEqualTo(antes.total());
        assertThat(despues.pago()).isEqualTo(antes.pago());
        assertThat(despues.presupuestoVersion()).isEqualTo(antes.presupuestoVersion());
        assertThat(ganancias.listar()).hasSize(1);
        assertThat(despues.historial())
                .anyMatch(h -> h.accion().contains("Detalle público actualizado"));
        var ficha = fichas.ficha(token(v.enlace()));
        assertThat(ficha.trabajos().getFirst().servicios().getFirst().resumen())
                .isEqualTo("Cambio de aceite y filtro. Control de frenos.");
        mvc.perform(
                        put("/api/admin/ordenes/" + o.id() + "/servicios/" + sid + "/resumen")
                                .with(user("operador").roles("OPERACION"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"version\":0,\"resumenPublico\":\"No permitido\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void evitaSegundaOrdenActivaYConflictoDeVersion() {
        var v = vehiculo();
        var s = servicio(TipoComision.FIJA, BigDecimal.ONE);
        var o = ordenAprobada(v, s);
        assertThatThrownBy(() -> ordenAprobada(v, s)).hasMessageContaining("activa");
        asignar(o, 1);
        assertThatThrownBy(
                        () ->
                                ordenes.avanzar(
                                        o.id(),
                                        o.servicios().getFirst().id(),
                                        new AvanceEntrada(o.version(), "")))
                .hasMessageContaining("otra pantalla");
    }

    @Test
    void fichaMantieneMismoQrAlEntregarYVolverTrasVariosAnios() {
        var v = vehiculo();
        var c = servicio(TipoComision.FIJA, BigDecimal.ONE);
        var o = ordenAprobada(v, c);
        asignar(o, 1);
        terminar(o);
        ordenes.cobrar(o.id(), new CobroEntrada(version(o.id()), MedioPago.EFECTIVO));
        ordenes.entregar(o.id(), new VersionEntrada(version(o.id()), "Entregado"));
        clock.fijar(clock.instant().plus(Duration.ofDays(3650)));
        assertThat(fichas.ficha(token(v.enlace())).trabajos()).hasSize(1);
        var siguiente = ordenAprobada(v, c);
        asignar(siguiente, 1);
        terminar(siguiente);
        assertThat(vehiculos.obtener(v.id()).enlace()).isEqualTo(v.enlace());
        assertThat(fichas.ficha(token(v.enlace())).trabajos()).hasSize(2);
    }

    @Test
    void endpointsTemporalesYRotacionYaNoExisten() throws Exception {
        mvc.perform(get("/api/seguimiento/antiguo")).andExpect(status().isNotFound());
        mvc.perform(get("/seguimiento/antiguo")).andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/vehiculos/1/rotar").with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/ordenes/1/rotar").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void fichaPermanenteMantieneHistorialSinDatosPrivados() throws Exception {
        var v = vehiculo();
        var o = orden(v, servicio(TipoComision.FIJA, BigDecimal.ONE));
        asignar(o, 1);
        ordenes.aprobar(o.id(), new VersionEntrada(version(o.id()), "Aceptado"));
        terminar(o);
        clock.fijar(clock.instant().plus(Duration.ofDays(90)));
        assertThat(fichas.ficha(token(v.enlace())).trabajos()).hasSize(1);
        mvc.perform(get("/api/vehiculo/" + token(v.enlace())))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "NOTA INTERNA"))))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "Cliente privado"))))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString("comision"))))
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"));
    }

    @Test
    void reabrirGeneraAjustesSinModificarLiquidacionPagada() {
        var o = ordenAprobada(vehiculo(), servicio(TipoComision.FIJA, new BigDecimal("20")));
        asignar(o, 1);
        terminar(o);
        var g = ganancias.listar().getFirst();
        ganancias.liquidar(
                new LiquidacionEntrada(
                        g.empleadoId(),
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31),
                        MedioPago.EFECTIVO));
        ordenes.reabrir(
                o.id(),
                o.servicios().getFirst().id(),
                new VersionEntrada(version(o.id()), "Retrabajo"));
        assertThat(ganancias.liquidaciones().getFirst().importe()).isEqualByComparingTo("20");
        assertThat(ganancias.listar()).hasSize(2);
        terminar(o);
        assertThat(ganancias.listar()).hasSize(3);
        assertThat(
                        ganancias.listar().stream()
                                .map(GananciaDto::importe)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("20");
    }

    @Test
    void mantenimientoSeEvaluaPorPrimerLimiteYLecturaConocida() {
        var v = vehiculo();
        var m =
                mantenimientos.guardar(
                        null,
                        new MantenimientoEntrada(
                                v.id(), null, "Aceite", LocalDate.of(2026, 10, 14), 2000L, 7, true),
                        null);
        assertThat(m.estado()).isEqualTo("PROXIMO");
        vehiculos.kilometraje(v.id(), new LecturaEntrada(2000, "Ingreso", false));
        assertThat(mantenimientos.listar().getFirst().estado()).isEqualTo("VENCIDO");
        assertThatThrownBy(
                        () ->
                                vehiculos.kilometraje(
                                        v.id(), new LecturaEntrada(1500, "Error", false)))
                .hasMessageContaining("disminuir");
        vehiculos.kilometraje(v.id(), new LecturaEntrada(1500, "Corrección del odómetro", true));
        assertThat(mantenimientos.listar().getFirst().estado()).isEqualTo("PROXIMO");
    }

    @Test
    void sinLecturaNoInventaVigenciaPorKilometros() {
        var v = vehiculo();
        var m =
                mantenimientos.guardar(
                        null,
                        new MantenimientoEntrada(v.id(), null, "Aceite", null, 2000L, 7, false),
                        null);
        assertThat(m.estado()).isEqualTo("KILOMETRAJE_SIN_VERIFICAR");
    }

    @Test
    void recordatoriosPersistentesSinDuplicadosYReprogramacion() {
        var v = vehiculo();
        var m =
                mantenimientos.guardar(
                        null,
                        new MantenimientoEntrada(
                                v.id(), null, "Aceite", LocalDate.of(2026, 10, 14), null, 7, true),
                        null);
        recordatorios.programar();
        recordatorios.programar();
        assertThat(recordatorioRepo.findAll()).hasSize(1);
        assertThat(recordatorios.listar().getFirst().programado())
                .isEqualTo(Instant.parse("2026-10-07T12:00:00Z"));
        mantenimientos.guardar(
                m.id(),
                new MantenimientoEntrada(
                        v.id(), null, "Aceite", LocalDate.of(2026, 10, 20), null, 7, true),
                m.version());
        assertThat(recordatorios.listar().getFirst().estado())
                .isEqualTo(EstadoRecordatorio.CANCELADO);
    }

    @Test
    void fechaYKilometrosNoDuplicanAvisoDeVencimiento() {
        var v = vehiculo();
        vehiculos.kilometraje(v.id(), new LecturaEntrada(2000, "Ingreso", false));
        mantenimientos.guardar(
                null,
                new MantenimientoEntrada(
                        v.id(), null, "Aceite", LocalDate.of(2026, 10, 7), 2000L, 7, true),
                null);
        recordatorios.programar();
        recordatorios.programar();
        assertThat(recordatorioRepo.findAll()).hasSize(1);
        assertThat(recordatorios.listar().getFirst().tipo())
                .isEqualTo(TipoRecordatorio.VENCIMIENTO);
    }

    @Test
    void alReiniciarSoloSeProgramaAvisoRelevante() {
        var v = vehiculo();
        mantenimientos.guardar(
                null,
                new MantenimientoEntrada(
                        v.id(), null, "Aceite", LocalDate.of(2026, 10, 1), null, 7, true),
                null);
        recordatorios.programar();
        assertThat(recordatorios.listar()).hasSize(1);
        assertThat(recordatorios.listar().getFirst().tipo())
                .isEqualTo(TipoRecordatorio.VENCIMIENTO);
    }

    @Test
    void cambioPropietarioConservaQrYCancelaDestinatario() {
        var v = vehiculo();
        mantenimientos.guardar(
                null,
                new MantenimientoEntrada(
                        v.id(), null, "Aceite", LocalDate.of(2026, 10, 14), null, 7, true),
                null);
        recordatorios.programar();
        var c = clientes.guardar(null, new ClienteEntrada("Nuevo cliente", "", false));
        vehiculos.guardar(
                v.id(), new VehiculoEntrada(v.patente(), v.tipo(), v.marca(), v.modelo(), c.id()));
        assertThat(vehiculos.obtener(v.id()).enlace()).isEqualTo(v.enlace());
        assertThat(fichas.ficha(token(v.enlace()))).isNotNull();
        assertThat(recordatorios.listar().getFirst().estado())
                .isEqualTo(EstadoRecordatorio.CANCELADO);
        assertThat(mantenimientos.listar().getFirst().avisos()).isFalse();
    }

    @Test
    void bajaConsentimientoCancelaPendientes() {
        var v = vehiculo();
        mantenimientos.guardar(
                null,
                new MantenimientoEntrada(
                        v.id(), null, "Aceite", LocalDate.of(2026, 10, 14), null, 7, true),
                null);
        recordatorios.programar();
        clientes.guardar(v.clienteId(), new ClienteEntrada("Cliente", "+5493515551234", false));
        recordatorios.programar();
        assertThat(recordatorios.listar())
                .allMatch(r -> r.estado() == EstadoRecordatorio.CANCELADO);
    }

    @Test
    void cumplimientoRequiereServicioDelMismoVehiculo() {
        var v = vehiculo();
        var o = orden(v, servicio(TipoComision.FIJA, BigDecimal.ONE));
        asignar(o, 1);
        ordenes.aprobar(o.id(), new VersionEntrada(version(o.id()), "Aceptado"));
        terminar(o);
        var m =
                mantenimientos.guardar(
                        null,
                        new MantenimientoEntrada(
                                v.id(),
                                o.servicios().getFirst().id(),
                                "Próximo service",
                                LocalDate.of(2026, 10, 14),
                                null,
                                7,
                                true),
                        null);
        recordatorios.programar();
        mantenimientos.realizar(
                m.id(), new CumplimientoEntrada(m.version(), o.servicios().getFirst().id()));
        assertThat(mantenimientos.listar().getFirst().estado()).isEqualTo("REALIZADO");
        assertThat(recordatorios.listar())
                .allMatch(r -> r.estado() == EstadoRecordatorio.CANCELADO);
    }

    @Test
    void pdfTieneTextoCorrectoYQrEscaneable() throws Exception {
        var v = vehiculo();
        var o = ordenAprobada(v, servicio(TipoComision.FIJA, BigDecimal.ONE));
        byte[] pdf = tickets.orden(o.id());
        try (var doc = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(doc))
                    .contains("AB123CD", "Service ágil", "PENDIENTE", "100.00");
            assertThat(doc.getPage(0).getMediaBox().getWidth()).isBetween(226f, 227f);
            var image = new PDFRenderer(doc).renderImageWithDPI(0, 180);
            var qr =
                    new MultiFormatReader()
                            .decode(
                                    new BinaryBitmap(
                                            new HybridBinarizer(
                                                    new BufferedImageLuminanceSource(image))));
            assertThat(qr.getText()).isEqualTo(v.enlace());
        }
    }

    @Test
    void vistasThymeleafRenderizan() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "data-modo=\"ADMIN\"")));
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OPERACION")
    void operacionNoPuedeAdministrarNiVerGanancias() throws Exception {
        mvc.perform(get("/api/admin/datos")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/ganancias")).andExpect(status().isForbidden());
        mvc.perform(get("/api/ordenes")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PANTALLA")
    void televisionSoloLectura() throws Exception {
        mvc.perform(
                        post("/api/vehiculos/1/kilometraje")
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"kilometraje\":2000,\"motivo\":\"Ingreso\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/empleados")).andExpect(status().isForbidden());
        mvc.perform(get("/pantalla")).andExpect(status().isOk());
    }

    @Test
    void csrfObligatorioYWebhookRechazaFirmaFalsa() throws Exception {
        mvc.perform(
                        post("/api/admin/empleados")
                                .contentType("application/json")
                                .content("{\"nombre\":\"Ana\",\"activo\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/webhooks/whatsapp").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        assertThat(whatsapp.habilitado()).isFalse();
    }

    private void aviso() {
        var v = vehiculo();
        mantenimientos.guardar(
                null,
                new MantenimientoEntrada(
                        v.id(), null, "Aceite", LocalDate.of(2026, 10, 14), null, 7, true),
                null);
        recordatorios.programar();
        when(whatsapp.habilitado()).thenReturn(true);
    }

    @Test
    void whatsappSimuladoEntregaUnaSolaVez() {
        aviso();
        when(whatsapp.enviar(any()))
                .thenReturn(new ResultadoEnvioDto("ACEPTADO", "wamid.prueba", "Aceptado"));
        recordatorios.despachar();
        recordatorios.despachar();
        verify(whatsapp, times(1)).enviar(any());
        assertThat(recordatorios.listar().getFirst().estado())
                .isEqualTo(EstadoRecordatorio.ACEPTADO);
        assertThat(recordatorios.listar().getFirst().intentos()).isEqualTo(1);
    }

    @Test
    void whatsappInciertoNoSeReintenta() {
        aviso();
        when(whatsapp.enviar(any())).thenReturn(new ResultadoEnvioDto("INCIERTO", null, "Timeout"));
        recordatorios.despachar();
        clock.fijar(clock.instant().plusSeconds(600));
        recordatorios.programar();
        recordatorios.despachar();
        verify(whatsapp, times(1)).enviar(any());
        assertThat(recordatorios.listar().getFirst().estado())
                .isEqualTo(EstadoRecordatorio.INCIERTO);
    }

    @Test
    void whatsappLimitaReintentosRecuperables() {
        aviso();
        when(whatsapp.enviar(any())).thenReturn(new ResultadoEnvioDto("REINTENTAR", null, "429"));
        for (int i = 0; i < 4; i++) {
            recordatorios.despachar();
            clock.fijar(clock.instant().plusSeconds(200));
        }
        verify(whatsapp, times(3)).enviar(any());
        assertThat(recordatorios.listar().getFirst().estado())
                .isEqualTo(EstadoRecordatorio.FALLIDO);
    }

    @Test
    void webhookDuplicadoNoRetrocedeEntrega() {
        aviso();
        when(whatsapp.enviar(any()))
                .thenReturn(new ResultadoEnvioDto("ACEPTADO", "wamid.prueba", "Aceptado"));
        recordatorios.despachar();
        when(whatsapp.verificarFirma(any(), any())).thenReturn(true);
        String body =
                "{\"entry\":[{\"changes\":[{\"value\":{\"statuses\":[{\"id\":\"wamid.prueba\",\"status\":\"delivered\"}]}}]}]}";
        recordatorios.webhook(body.getBytes(), "firma");
        recordatorios.webhook(body.getBytes(), "firma");
        recordatorios.webhook(body.replace("delivered", "failed").getBytes(), "firma");
        assertThat(recordatorios.listar().getFirst().estado())
                .isEqualTo(EstadoRecordatorio.ENTREGADO);
    }

    @Test
    void reinicioMarcaEnvioInterrumpidoComoIncierto() {
        aviso();
        var r = recordatorioRepo.findAll().getFirst();
        r.setEstado(EstadoRecordatorio.ENVIANDO);
        r.setActualizado(clock.instant().minusSeconds(600));
        recordatorioRepo.save(r);
        recordatorios.programar();
        recordatorios.despachar();
        verify(whatsapp, never()).enviar(any());
        assertThat(recordatorios.listar().getFirst().estado())
                .isEqualTo(EstadoRecordatorio.INCIERTO);
    }

    @Test
    void dtoOperativoNoFiltraInformacionFinanciera() throws Exception {
        var o = ordenAprobada(vehiculo(), servicio(TipoComision.FIJA, BigDecimal.ONE));
        asignar(o, 1);
        mvc.perform(get("/api/ordenes").with(user("operador").roles("OPERACION")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString("precio"))))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString("comision"))))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "NOTA INTERNA"))));
    }

    @Test
    void cancelarOrdenFinalizadaRevierteGananciaYPermiteNuevoIngreso() {
        var v = vehiculo();
        var s = servicio(TipoComision.FIJA, new BigDecimal("5"));
        var o = ordenAprobada(v, s);
        asignar(o, 1);
        terminar(o);
        ordenes.cancelar(o.id(), new VersionEntrada(version(o.id()), "Ingreso por error"));
        assertThat(
                        ganancias.listar().stream()
                                .map(GananciaDto::importe)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("0");
        assertThat(ordenAprobada(v, s).estado()).isEqualTo(EstadoTrabajo.RECIBIDO);
    }

    @Test
    void dobleAvanceConcurrenteNoSaltaEtapas() throws Exception {
        var o = ordenAprobada(vehiculo(), servicio(TipoComision.FIJA, BigDecimal.ONE));
        asignar(o, 1);
        long version = version(o.id());
        var auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "admin",
                        "",
                        List.of(
                                new org.springframework.security.core.authority
                                        .SimpleGrantedAuthority("ROLE_ADMIN")));
        java.util.concurrent.Callable<Boolean> tarea =
                () -> {
                    var context =
                            org.springframework.security.core.context.SecurityContextHolder
                                    .createEmptyContext();
                    context.setAuthentication(auth);
                    org.springframework.security.core.context.SecurityContextHolder.setContext(
                            context);
                    try {
                        ordenes.avanzar(
                                o.id(),
                                o.servicios().getFirst().id(),
                                new AvanceEntrada(version, ""));
                        return true;
                    } catch (ReglaNegocioException e) {
                        return false;
                    } finally {
                        org.springframework.security.core.context.SecurityContextHolder
                                .clearContext();
                    }
                };
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var resultados = pool.invokeAll(List.of(tarea, tarea));
            assertThat(
                            resultados.stream()
                                    .filter(
                                            f -> {
                                                try {
                                                    return f.get();
                                                } catch (Exception e) {
                                                    throw new RuntimeException(e);
                                                }
                                            })
                                    .count())
                    .isEqualTo(1);
        }
        assertThat(ordenes.obtener(o.id()).estado()).isEqualTo(EstadoTrabajo.EN_PROCESO);
        assertThat(ganancias.listar()).isEmpty();
    }

    @Test
    void cambiosDeCuentaRevocanSesionesExistentes() {
        usuarios.guardar(
                null,
                new UsuarioEntrada("operador-prueba", "Prueba-Sesion-2026", Rol.OPERACION, true));
        var cuenta =
                usuarios.listar().stream()
                        .filter(u -> u.nombre().equals("operador-prueba"))
                        .findFirst()
                        .orElseThrow();
        var principal =
                org.springframework.security.core.userdetails.User.withUsername("operador-prueba")
                        .password("hash-prueba")
                        .roles("OPERACION")
                        .build();
        sesiones.registerNewSession("sesion-de-prueba", principal);
        usuarios.guardar(
                cuenta.id(), new UsuarioEntrada(cuenta.nombre(), null, Rol.OPERACION, false));
        assertThat(sesiones.getSessionInformation("sesion-de-prueba").isExpired()).isTrue();
        sesiones.removeSessionInformation("sesion-de-prueba");
    }

    @Test
    void ultimoAdministradorNoPuedeDesactivarse() {
        var cuenta =
                usuarios.listar().stream()
                        .filter(u -> u.nombre().equals("admin"))
                        .findFirst()
                        .orElseThrow();
        assertThatThrownBy(
                        () ->
                                usuarios.guardar(
                                        cuenta.id(),
                                        new UsuarioEntrada("admin", null, Rol.ADMIN, false)))
                .hasMessageContaining("administrador activo");
    }

    @Test
    void lecturaDuranteTrabajoQuedaEnHistorialDelService() {
        var v = vehiculo();
        var o = orden(v, servicio(TipoComision.FIJA, BigDecimal.ONE));
        vehiculos.kilometraje(v.id(), new LecturaEntrada(3200, "Lectura al recibir", false));
        asignar(o, 1);
        ordenes.aprobar(o.id(), new VersionEntrada(version(o.id()), "Cliente conforme"));
        terminar(o);
        assertThat(fichas.ficha(token(v.enlace())).trabajos().getFirst().kilometraje())
                .isEqualTo(3200L);
        vehiculos.kilometraje(v.id(), new LecturaEntrada(4000, "Lectura posterior", false));
        assertThat(fichas.ficha(token(v.enlace())).trabajos().getFirst().kilometraje())
                .isEqualTo(3200L);
    }

    @Test
    void webhookAdelantadoSeCorrelacionaYNoRetrocede() {
        aviso();
        when(whatsapp.verificarFirma(any(), any())).thenReturn(true);
        when(whatsapp.enviar(any()))
                .thenAnswer(
                        invocacion -> {
                            MensajeWhatsAppDto m = invocacion.getArgument(0);
                            String body =
                                    "{\"entry\":[{\"changes\":[{\"value\":{\"statuses\":[{\"id\":\"wamid.adelantado\",\"biz_opaque_callback_data\":\""
                                            + m.referencia()
                                            + "\",\"status\":\"delivered\"}]}}]}]}";
                            recordatorios.webhook(body.getBytes(), "firma");
                            return new ResultadoEnvioDto(
                                    "ACEPTADO", "wamid.adelantado", "Aceptado");
                        });
        recordatorios.despachar();
        recordatorios.despachar();
        assertThat(recordatorios.listar().getFirst().estado())
                .isEqualTo(EstadoRecordatorio.ENTREGADO);
        verify(whatsapp, times(1)).enviar(any());
    }
}
