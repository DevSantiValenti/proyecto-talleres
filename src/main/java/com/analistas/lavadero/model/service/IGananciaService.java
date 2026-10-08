package com.analistas.lavadero.model.service;

import static com.analistas.lavadero.model.domain.Reglas.*;

import com.analistas.lavadero.model.domain.*;
import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.repository.*;

import java.math.*;
import java.time.*;
import java.util.*;

public interface IGananciaService {
    void generar(OrdenServicio servicio);

    void revertir(OrdenServicio servicio, String motivo);

    List<GananciaDto> listar();

    List<LiquidacionDto> liquidaciones();

    LiquidacionDto liquidar(LiquidacionEntrada datos);

    void ajustar(AjusteEntrada datos);
}
