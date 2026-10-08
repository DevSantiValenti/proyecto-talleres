package com.analistas.lavadero.model.service;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.util.UUID;

class MigracionTallerTest {
    @Test
    void migracionPreservaFichaEHistorialYDesactivaCatalogoAnterior() throws Exception {
        String url =
                "jdbc:h2:mem:migracion_"
                        + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("2").load().migrate();
        try (var c = DriverManager.getConnection(url, "sa", "");
                var q = c.createStatement()) {
            q.executeUpdate(
                    """
                    INSERT INTO vehiculo(id,patente,tipo,marca,modelo,token_publico)
                    VALUES (1,'ABC123','MOTO','Honda','Wave','token-permanente-previo')
                    """);
            q.executeUpdate(
                    """
                    INSERT INTO servicio_catalogo(id,nombre,categoria,precio,tipo_comision,comision,activo)
                    VALUES (1,'Servicio anterior','LAVADO',100,'FIJA',10,true),
                           (2,'Service de moto','TALLER',200,'FIJA',20,true)
                    """);
            q.executeUpdate(
                    """
                    INSERT INTO orden_trabajo(id,vehiculo_id,estado,ingreso,actualizacion,notas_internas,
                        token_temporal,presupuesto_version)
                    VALUES (1,1,'ENTREGADO',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'Nota conservada','temporal-antiguo',1)
                    """);
            q.executeUpdate(
                    """
                    INSERT INTO orden_servicio(id,orden_id,catalogo_id,nombre,categoria,precio,tipo_comision,
                        comision,etapa_actual,estado,resumen_publico,ciclo)
                    VALUES (1,1,2,'Service de moto','TALLER',200,'FIJA',20,2,'TERMINADO','Aceite y filtro',0)
                    """);
            Flyway.configure().dataSource(url, "sa", "").load().migrate();
            try (var r = q.executeQuery("SELECT token_publico FROM vehiculo WHERE id=1")) {
                assertThat(r.next()).isTrue();
                assertThat(r.getString(1)).isEqualTo("token-permanente-previo");
            }
            try (var r = q.executeQuery("SELECT activo FROM servicio_catalogo ORDER BY id")) {
                assertThat(r.next()).isTrue();
                assertThat(r.getBoolean(1)).isFalse();
                assertThat(r.next()).isTrue();
                assertThat(r.getBoolean(1)).isTrue();
            }
            try (var r =
                    q.executeQuery(
                            "SELECT resumen_publico, precio FROM orden_servicio WHERE id=1")) {
                assertThat(r.next()).isTrue();
                assertThat(r.getString(1)).isEqualTo("Aceite y filtro");
                assertThat(r.getBigDecimal(2)).isEqualByComparingTo("200");
            }
            try (var r = q.executeQuery("SELECT notas_internas FROM orden_trabajo WHERE id=1")) {
                assertThat(r.next()).isTrue();
                assertThat(r.getString(1)).isEqualTo("Nota conservada");
            }
        }
    }
}
