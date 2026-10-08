package com.analistas.lavadero.model.service;

import com.analistas.lavadero.model.domain.dto.Dtos.*;

public interface IWhatsAppService {
    boolean habilitado();

    ResultadoEnvioDto enviar(MensajeWhatsAppDto mensaje);

    boolean verificarFirma(byte[] cuerpo, String firma);

    boolean verificarToken(String token);
}
