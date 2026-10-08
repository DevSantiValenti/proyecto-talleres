package com.analistas.lavadero.model.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "servicio_catalogo")
@Getter
@Setter
public class ServicioCatalogo extends EntidadBase {
    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "precio", precision = 14, scale = 2, nullable = false)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comision", length = 30, nullable = false)
    private TipoComision tipoComision;

    @Column(name = "comision", precision = 14, scale = 2, nullable = false)
    private BigDecimal comision;

    @Column(name = "activo")
    private boolean activo = true;

    @ElementCollection
    @CollectionTable(
            name = "servicio_catalogo_etapa",
            joinColumns = @JoinColumn(name = "propietario_id"))
    @OrderColumn(name = "posicion")
    private List<Etapa> etapas = new ArrayList<>();
}
