package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "recordatorio")
@Getter
@Setter
public class Recordatorio extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mantenimiento_id", nullable = false)
    private MantenimientoProgramado mantenimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Cliente destinatario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30, nullable = false)
    private TipoRecordatorio tipo;

    @Column(name = "revision")
    private int revision;

    @Column(name = "programado")
    private Instant programado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30, nullable = false)
    private EstadoRecordatorio estado = EstadoRecordatorio.PENDIENTE;

    @Column(name = "intentos")
    private int intentos;

    @Column(name = "proveedor_id", length = 255, nullable = true)
    private String proveedorId;

    @Column(name = "actualizado")
    private Instant actualizado;

    @Column(name = "detalle", length = 500, nullable = false)
    private String detalle = "";

    @Column(name = "clave", length = 100, nullable = false)
    private String clave;
}
