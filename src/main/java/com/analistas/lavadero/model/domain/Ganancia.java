package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "ganancia")
@Getter
@Setter
public class Ganancia extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    private OrdenServicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(name = "importe", precision = 14, scale = 2, nullable = false)
    private BigDecimal importe;

    @Column(name = "fecha")
    private Instant fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liquidacion_id", nullable = true)
    private Liquidacion liquidacion;

    @Column(name = "concepto", length = 1000, nullable = false)
    private String concepto;

    @Column(name = "clave", length = 120, nullable = false)
    private String clave;
}
