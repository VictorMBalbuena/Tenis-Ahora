package com.tenisahora.modules.torneo.entity;

/**
 * A quien esta abierto el torneo.
 *
 * MASCULINO y FEMENINO se comparan contra el sexo del socio al inscribirlo; MIXTO no mira el sexo
 * de nadie, asi que a un torneo mixto se puede anotar cualquier socio.
 */
public enum CategoriaTorneo {
    MASCULINO,
    FEMENINO,
    MIXTO
}
