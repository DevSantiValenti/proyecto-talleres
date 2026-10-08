package com.analistas.lavadero.model.service;

import static com.analistas.lavadero.model.domain.Reglas.*;

import com.analistas.lavadero.model.domain.*;
import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.repository.*;

import java.math.*;
import java.time.*;
import java.util.*;

public interface IFichaVehiculoService {
    FichaPublicaDto ficha(String token);

    Long vehiculoId(String token);
}
