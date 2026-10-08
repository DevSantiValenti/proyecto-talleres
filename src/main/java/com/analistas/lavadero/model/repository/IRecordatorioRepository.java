package com.analistas.lavadero.model.repository;

import com.analistas.lavadero.model.domain.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface IRecordatorioRepository extends JpaRepository<Recordatorio, Long> {
    List<Recordatorio> findByMantenimientoId(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Recordatorio> findByProveedorId(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Recordatorio> findByClave(String clave);

    boolean existsByClave(String clave);

    List<Recordatorio> findByEstadoAndProgramadoLessThanEqual(
            EstadoRecordatorio estado, Instant ahora);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Recordatorio e where e.id = :id")
    Optional<Recordatorio> bloquear(@Param("id") Long id);
}
