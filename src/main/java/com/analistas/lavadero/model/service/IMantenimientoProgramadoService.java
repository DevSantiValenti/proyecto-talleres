package com.analistas.lavadero.model.service;

import static com.analistas.lavadero.model.domain.Reglas.*;

import com.analistas.lavadero.model.domain.*;
import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.repository.*;

import java.math.*;
import java.time.*;
import java.util.*;

public interface IMantenimientoProgramadoService {
    List<MantenimientoDto> listar();

    MantenimientoDto guardar(Long id, MantenimientoEntrada entrada, Long version);

    void realizar(Long id, CumplimientoEntrada datos);

    void cancelar(Long id, VersionEntrada datos);
}
