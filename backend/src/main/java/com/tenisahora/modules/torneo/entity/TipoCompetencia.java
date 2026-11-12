package com.tenisahora.modules.torneo.entity;

/**
 * Si la competencia es un torneo o una liga. La E05 las nombra siempre juntas ("torneo / liga", RF52, RD14, RD21)
 * y comparten todo: inscripción, arancel, cobro y resultados. Lo único que las distingue es cómo se juegan.
 *
 * <p>Un <b>torneo</b> es por eliminación directa: el que pierde queda afuera y el cuadro se va achicando hasta la
 * final. Una <b>liga</b> es todos contra todos: cada participante juega una vez con cada uno, nadie queda afuera,
 * y el campeón sale de la tabla de posiciones.
 *
 * <p>Por eso no hay un módulo de ligas aparte: es la misma competencia con otra forma de armar el fixture.
 */
public enum TipoCompetencia {

    TORNEO("Torneo", "Eliminación directa"),
    LIGA("Liga", "Todos contra todos");

    private final String etiqueta;
    private final String comoSeJuega;

    TipoCompetencia(String etiqueta, String comoSeJuega) {
        this.etiqueta = etiqueta;
        this.comoSeJuega = comoSeJuega;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String comoSeJuega() {
        return comoSeJuega;
    }

    public boolean esLiga() {
        return this == LIGA;
    }
}
