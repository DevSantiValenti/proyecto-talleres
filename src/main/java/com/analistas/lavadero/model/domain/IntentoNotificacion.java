package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "intento_notificacion")
@Getter
@Setter
public class IntentoNotificacion extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recordatorio_id", nullable = false)
    private Recordatorio recordatorio;

    @Column(name = "fecha")
    private Instant fecha;

    @Column(name = "resultado", length = 500, nullable = false)
    private String resultado;
}
