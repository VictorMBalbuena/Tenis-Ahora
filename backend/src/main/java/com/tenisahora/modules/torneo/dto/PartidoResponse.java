package com.tenisahora.modules.torneo.dto;

import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.entity.Partido;

import java.time.LocalDate;

public record PartidoResponse(
        Long id,
        Long torneoId,
        Integer ronda,
        Integer orden,
        Long participanteAId,
        String participanteANombre,
        Long participanteBId,
        String participanteBNombre,
        String sets,
        String resultado,
        LocalDate fecha
) {
    public static PartidoResponse de(Partido p) {
        if (p == null) return null;
        Long aId = p.getParticipanteA() != null ? p.getParticipanteA().getId() : null;
        Long bId = p.getParticipanteB() != null ? p.getParticipanteB().getId() : null;

        return new PartidoResponse(
                p.getId(),
                p.getTorneo().getId(),
                p.getRonda(),
                p.getOrden(),
                aId,
                formatearParticipante(p.getParticipanteA()),
                bId,
                formatearParticipante(p.getParticipanteB()),
                p.getSets(),
                p.getResultado(),
                p.getFecha()
        );
    }

    public static String formatearParticipante(Inscripcion inscripcion) {
        if (inscripcion == null) return null;
        if (inscripcion.getNombreEquipo() != null && !inscripcion.getNombreEquipo().isBlank()) {
            return inscripcion.getNombreEquipo();
        }
        String nombre = inscripcion.getJugador1().getNombre() + " " + inscripcion.getJugador1().getApellido();
        if (inscripcion.getPareja() != null) {
            nombre += " / " + inscripcion.getPareja().getNombre() + " " + inscripcion.getPareja().getApellido();
        }
        return nombre;
    }
}
