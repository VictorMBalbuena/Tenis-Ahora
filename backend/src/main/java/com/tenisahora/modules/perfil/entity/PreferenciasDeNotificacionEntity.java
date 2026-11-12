package com.tenisahora.modules.perfil.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Preferencias de notificaciones de una persona. No es un dato de Persona (clase abstracta sin tabla propia): vive
 * aparte, con el id de persona como clave, válido para cualquier rol sin importar en qué tabla esté.
 */
@Entity
@Table(name = "preferencias_de_notificacion")
@Getter
@Setter
@NoArgsConstructor
public class PreferenciasDeNotificacionEntity {

    @Id
    @Column(name = "persona_id")
    private Long personaId;

    @Column(name = "recordatorio_reservas", nullable = false)
    private boolean recordatorioReservas;

    @Column(name = "recordatorio_clases", nullable = false)
    private boolean recordatorioClases;

    @Column(name = "novedades_del_club", nullable = false)
    private boolean novedadesDelClub;

    public PreferenciasDeNotificacionEntity(Long personaId, boolean recordatorioReservas, boolean recordatorioClases,
                                            boolean novedadesDelClub) {
        this.personaId = personaId;
        this.recordatorioReservas = recordatorioReservas;
        this.recordatorioClases = recordatorioClases;
        this.novedadesDelClub = novedadesDelClub;
    }
}
