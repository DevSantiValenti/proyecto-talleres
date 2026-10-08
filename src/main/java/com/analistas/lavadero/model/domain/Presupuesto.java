package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "presupuesto")
@Getter
@Setter
public class Presupuesto extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenTrabajo orden;

    @Column(name = "numero")
    private int numero;

    @Column(name = "detalle", columnDefinition = "text", nullable = false)
    private String detalle;

    @Column(name = "total", precision = 14, scale = 2, nullable = false)
    private BigDecimal total;

    @Column(name = "fecha")
    private Instant fecha;

    @Column(name = "actor", length = 100, nullable = false)
    private String actor;
}
