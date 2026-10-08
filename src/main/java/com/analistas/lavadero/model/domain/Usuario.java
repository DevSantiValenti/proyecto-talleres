package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario extends EntidadBase {
    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "password", length = 100, nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", length = 30, nullable = false)
    private Rol rol;

    @Column(name = "activo")
    private boolean activo = true;
}
