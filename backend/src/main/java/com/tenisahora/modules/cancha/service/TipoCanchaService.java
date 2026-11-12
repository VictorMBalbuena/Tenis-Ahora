package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.TipoCanchaRequest;
import com.tenisahora.modules.cancha.dto.TipoCanchaResponse;

import java.util.List;

public interface TipoCanchaService {

    TipoCanchaResponse registrar(TipoCanchaRequest request, Long actorId);

    TipoCanchaResponse modificar(Long id, TipoCanchaRequest request, Long actorId);

    TipoCanchaResponse consultar(Long id);

    List<TipoCanchaResponse> listar();

    void eliminar(Long id, Long actorId);

    byte[] generarReportePdf();
}

