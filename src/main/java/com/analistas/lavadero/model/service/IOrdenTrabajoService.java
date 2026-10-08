package com.analistas.lavadero.model.service;

import static com.analistas.lavadero.model.domain.Reglas.*;

import com.analistas.lavadero.model.domain.*;
import com.analistas.lavadero.model.domain.dto.Dtos.*;
import com.analistas.lavadero.model.repository.*;

import java.math.*;
import java.time.*;
import java.util.*;

public interface IOrdenTrabajoService {

    List<OrdenOperacionDto> cola();

    List<OrdenAdminDto> listar();

    OrdenAdminDto obtener(Long id);

    OrdenAdminDto crear(OrdenEntrada datos);

    void asignar(Long id, Long servicioId, AsignacionEntrada datos);

    void avanzar(Long id, Long servicioId, AvanceEntrada datos);

    void aprobar(Long id, VersionEntrada datos);

    void agregarServicio(Long id, long version, LineaEntrada datos);

    void precio(Long id, Long servicioId, PrecioEntrada datos);

    void repuesto(Long id, RepuestoEntrada datos);

    void editarRepuesto(Long id, Long repuestoId, RepuestoEntrada datos);

    void resumen(Long id, Long servicioId, ResumenTrabajoEntrada datos);

    void quitarRepuesto(Long id, Long repuestoId, VersionEntrada datos);

    void cobrar(Long id, CobroEntrada datos);

    void entregar(Long id, VersionEntrada datos);

    void cancelar(Long id, VersionEntrada datos);

    void reabrir(Long id, Long servicioId, VersionEntrada datos);

    void cancelarServicio(Long id, Long servicioId, VersionEntrada datos);
}
