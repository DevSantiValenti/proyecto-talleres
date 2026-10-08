package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "orden_servicio")
@Getter
@Setter
public class OrdenServicio extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenTrabajo orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalogo_id", nullable = false)
    private ServicioCatalogo catalogo;

    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "precio", precision = 14, scale = 2, nullable = false)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comision", length = 30, nullable = false)
    private TipoComision tipoComision;

    @Column(name = "comision", precision = 14, scale = 2, nullable = false)
    private BigDecimal comision;

    @Column(name = "etapa_actual")
    private int etapaActual;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30, nullable = false)
    private EstadoTrabajo estado = EstadoTrabajo.EN_ESPERA;

    @Column(name = "resumen_publico", length = 2000, nullable = false)
    private String resumenPublico = "";

    @Column(name = "finalizacion")
    private Instant finalizacion;

    @Column(name = "ciclo")
    private int ciclo;

    @ElementCollection
    @CollectionTable(
            name = "orden_servicio_etapa",
            joinColumns = @JoinColumn(name = "propietario_id"))
    @OrderColumn(name = "posicion")
    private List<Etapa> etapas = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "servicio_empleado",
            joinColumns = @JoinColumn(name = "servicio_id"),
            inverseJoinColumns = @JoinColumn(name = "empleado_id"))
    private Set<Empleado> empleados = new LinkedHashSet<>();
}
