package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "cliente")
@Getter
@Setter
public class Cliente extends EntidadBase {
    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "telefono", length = 30, nullable = true)
    private String telefono;

    @Column(name = "consentimiento_whatsapp")
    private boolean consentimientoWhatsapp;

    @Column(name = "consentimiento_fecha")
    private Instant consentimientoFecha;
}
