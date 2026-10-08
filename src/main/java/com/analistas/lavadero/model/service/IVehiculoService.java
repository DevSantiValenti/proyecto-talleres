package com.analistas.lavadero.model.service;

import static com.analistas.lavadero.model.domain.Reglas.*;

import com.analistas.lavadero.model.domain.*;
import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.repository.*;

import java.math.*;
import java.time.*;
import java.util.*;

public interface IVehiculoService {
    List<VehiculoDto> listar();

    VehiculoDto obtener(Long id);

    VehiculoDto guardar(Long id, VehiculoEntrada entrada);

    void kilometraje(Long id, LecturaEntrada entrada);

    List<LecturaDto> lecturas(Long id);
}
