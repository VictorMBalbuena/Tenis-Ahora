package com.tenisahora.modules.instructor.service;

import com.tenisahora.modules.instructor.dto.HabilitacionResponse;
import com.tenisahora.shared.model.Rol;

public interface AsignacionDeInstructores {
    
    void asegurarAsignable(Rol rol, Long instructorId);

    HabilitacionResponse habilitacion(Rol rol, Long instructorId);
}
