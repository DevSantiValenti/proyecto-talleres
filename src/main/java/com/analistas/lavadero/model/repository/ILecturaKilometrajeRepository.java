package com.analistas.lavadero.model.repository;

import com.analistas.lavadero.model.domain.*;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.*;

public interface ILecturaKilometrajeRepository extends JpaRepository<LecturaKilometraje, Long> {
    List<LecturaKilometraje> findByVehiculoIdOrderByFechaDesc(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from LecturaKilometraje e where e.id = :id")
    Optional<LecturaKilometraje> bloquear(@Param("id") Long id);
}
