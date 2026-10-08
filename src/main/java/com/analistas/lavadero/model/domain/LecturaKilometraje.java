package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "lectura_kilometraje")
@Getter
@Setter
public class LecturaKilometraje extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(name = "kilometraje")
    private long kilometraje;

    @Column(name = "fecha")
    private Instant fecha;

    @Column(name = "actor", length = 100, nullable = false)
    private String actor;

    @Column(name = "motivo", length = 1000, nullable = false)
    private String motivo;
}
