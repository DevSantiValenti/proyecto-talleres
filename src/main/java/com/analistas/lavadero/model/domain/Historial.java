package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "historial")
@Getter
@Setter
public class Historial extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenTrabajo orden;

    @Column(name = "fecha")
    private Instant fecha;

    @Column(name = "actor", length = 100, nullable = false)
    private String actor;

    @Column(name = "accion", length = 2000, nullable = false)
    private String accion;
}
