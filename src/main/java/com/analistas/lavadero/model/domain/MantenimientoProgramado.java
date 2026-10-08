package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "mantenimiento_programado")
@Getter
@Setter
public class MantenimientoProgramado extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id", nullable = true)
    private Cliente destinatario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_id", nullable = true)
    private OrdenServicio origen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realizado_con_id", nullable = true)
    private OrdenServicio realizadoCon;

    @Column(name = "descripcion", length = 300, nullable = false)
    private String descripcion;

    @Column(name = "fecha_objetivo")
    private LocalDate fechaObjetivo;

    @Column(name = "kilometraje_objetivo")
    private Long kilometrajeObjetivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30, nullable = false)
    private EstadoMantenimiento estado = EstadoMantenimiento.PENDIENTE;

    @Column(name = "anticipacion_dias")
    private int anticipacionDias = 7;

    @Column(name = "avisos")
    private boolean avisos = true;

    @Column(name = "revision")
    private int revision = 1;
}
