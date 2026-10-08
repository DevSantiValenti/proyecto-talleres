package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "repuesto")
@Getter
@Setter
public class Repuesto extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenTrabajo orden;

    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "cantidad")
    private int cantidad;

    @Column(name = "costo", precision = 14, scale = 2, nullable = false)
    private BigDecimal costo;

    @Column(name = "precio", precision = 14, scale = 2, nullable = false)
    private BigDecimal precio;
}
