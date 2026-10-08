package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "orden_trabajo")
@Getter
@Setter
public class OrdenTrabajo extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30, nullable = false)
    private EstadoTrabajo estado = EstadoTrabajo.RECIBIDO;

    @Column(name = "ingreso")
    private Instant ingreso;

    @Column(name = "actualizacion")
    private Instant actualizacion;

    @Column(name = "finalizacion")
    private Instant finalizacion;

    @Column(name = "entrega")
    private Instant entrega;

    @Column(name = "notas_internas", length = 2000, nullable = false)
    private String notasInternas = "";

    @Column(name = "presupuesto_version")
    private int presupuestoVersion = 1;

    @Column(name = "version_aprobada")
    private Integer versionAprobada;

    @Column(name = "aprobacion_nota", length = 500, nullable = true)
    private String aprobacionNota;

    @Column(name = "aprobacion_fecha")
    private Instant aprobacionFecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_previsto", length = 30, nullable = true)
    private MedioPago medioPrevisto;

    @Column(name = "kilometraje_ingreso")
    private Long kilometrajeIngreso;

    @Column(name = "vehiculo_activo")
    private Long vehiculoActivo;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<OrdenServicio> servicios = new ArrayList<>();

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Repuesto> repuestos = new ArrayList<>();
}
