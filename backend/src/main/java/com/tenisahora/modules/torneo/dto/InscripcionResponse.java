package com.tenisahora.modules.torneo.dto;

import com.tenisahora.modules.torneo.entity.EstadoInscripcionTorneo;
import com.tenisahora.modules.torneo.entity.Inscripcion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InscripcionResponse(
        Long id,
        Long torneoId,
        Long jugador1Id,
        String jugador1Nombre,
        Long parejaId,
        String parejaNombre,
        Integer equipo,
        String nombreEquipo,
        LocalDate fechaInscripcion,
        String torneo,
        EstadoInscripcionTorneo estado,
        BigDecimal montoTotal,
        LocalDateTime venceEn
) {
    public static InscripcionResponse de(Inscripcion inscripcion) {
        return de(inscripcion, null);
    }

    public static InscripcionResponse de(Inscripcion inscripcion, LocalDateTime venceEn) {
        String j1Nombre = inscripcion.getJugador1() != null
                ? inscripcion.getJugador1().getNombre() + " " + inscripcion.getJugador1().getApellido()
                : null;
        Long pId = inscripcion.getPareja() != null ? inscripcion.getPareja().getId() : null;
        String pNombre = inscripcion.getPareja() != null
                ? inscripcion.getPareja().getNombre() + " " + inscripcion.getPareja().getApellido()
                : null;

        return new InscripcionResponse(
                inscripcion.getId(),
                inscripcion.getTorneo().getId(),
                inscripcion.getJugador1().getId(),
                j1Nombre,
                pId,
                pNombre,
                inscripcion.getEquipo(),
                inscripcion.getNombreEquipo(),
                inscripcion.getFechaInscripcion(),
                inscripcion.getTorneo().getNombre(),
                inscripcion.getEstado(),
                inscripcion.getMontoTotal(),
                venceEn
        );
    }
}
