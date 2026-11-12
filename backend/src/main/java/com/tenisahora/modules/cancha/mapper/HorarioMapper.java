package com.tenisahora.modules.cancha.mapper;

import com.tenisahora.modules.cancha.dto.HorarioResponse;
import com.tenisahora.modules.cancha.entity.HorarioSemanal;
import org.springframework.stereotype.Component;

@Component
public class HorarioMapper {

    public HorarioResponse aRespuesta(HorarioSemanal horario) {
        return new HorarioResponse(horario.getId(), horario.getDiaSemana(), horario.getHoraInicio(), horario.getHoraFin());
    }
}
