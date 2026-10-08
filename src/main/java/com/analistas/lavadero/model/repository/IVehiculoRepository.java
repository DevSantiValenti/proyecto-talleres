package com.analistas.lavadero.model.repository;

import com.analistas.lavadero.model.domain.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface IVehiculoRepository extends JpaRepository<Vehiculo, Long> {
    Optional<Vehiculo> findByPatente(String patente);

    Optional<Vehiculo> findByTokenPublico(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Vehiculo e where e.id = :id")
    Optional<Vehiculo> bloquear(@Param("id") Long id);
}
