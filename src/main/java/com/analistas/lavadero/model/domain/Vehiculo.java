package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "vehiculo")
@Getter
@Setter
public class Vehiculo extends EntidadBase {
    @Column(name = "patente", length = 20, nullable = false)
    private String patente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30, nullable = false)
    private TipoVehiculo tipo;

    @Column(name = "marca", length = 100, nullable = false)
    private String marca;

    @Column(name = "modelo", length = 100, nullable = false)
    private String modelo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    @Column(name = "token_publico", length = 64, nullable = false, updatable = false)
    private String tokenPublico;

    @Column(name = "kilometraje")
    private Long kilometraje;

    @Column(name = "kilometraje_fecha")
    private Instant kilometrajeFecha;
}
