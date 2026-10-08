package com.analistas.lavadero.model.service;

import com.analistas.lavadero.model.domain.dto.Dtos.*;

import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface IUsuarioService extends UserDetailsService {
    List<UsuarioDto> listar();

    void guardar(Long id, UsuarioEntrada d);

    void inicializar(String nombre, String password);
}
