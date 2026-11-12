package com.tenisahora.modules.actividad.entity;

import java.util.Arrays;
import java.util.List;

public enum EstadoActividad {
    PROGRAMADA,
    EN_CURSO,
    FINALIZADA,
    CANCELADA;

    /** Programada o en curso ocupan la cancha; finalizada y cancelada no. */
    public boolean ocupaCancha() {
        return this == PROGRAMADA || this == EN_CURSO;
    }

    /** Solo una actividad programada admite nuevas inscripciones. */
    public boolean admiteInscripcion() {
        return this == PROGRAMADA;
    }

    /** Solo una actividad programada se puede modificar (horario, cupo, etc). */
    public boolean puedeModificarse() {
        return this == PROGRAMADA;
    }

    /** Los estados que ocupan la cancha, para las consultas de solapamiento. */
    public static List<EstadoActividad> queOcupanCancha() {
        return Arrays.stream(values()).filter(EstadoActividad::ocupaCancha).toList();
    }
}
