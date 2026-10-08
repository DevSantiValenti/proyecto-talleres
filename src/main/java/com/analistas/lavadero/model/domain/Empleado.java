package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "empleado")
@Getter
@Setter
public class Empleado extends EntidadBase {
    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "activo")
    private boolean activo = true;
}
