package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "liquidacion")
@Getter
@Setter
public class Liquidacion extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(name = "desde")
    private LocalDate desde;

    @Column(name = "hasta")
    private LocalDate hasta;

    @Column(name = "importe", precision = 14, scale = 2, nullable = false)
    private BigDecimal importe;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio", length = 30, nullable = false)
    private MedioPago medio;

    @Column(name = "fecha")
    private Instant fecha;

    @Column(name = "actor", length = 100, nullable = false)
    private String actor;
}
