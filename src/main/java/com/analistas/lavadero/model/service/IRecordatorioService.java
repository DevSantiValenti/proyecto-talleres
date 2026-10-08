package com.analistas.lavadero.model.service;

import com.analistas.lavadero.model.domain.dto.Dtos.RecordatorioDto;

import java.util.List;

public interface IRecordatorioService {
    void programar();

    void despachar();

    List<RecordatorioDto> listar();

    void webhook(byte[] cuerpo, String firma);
}
