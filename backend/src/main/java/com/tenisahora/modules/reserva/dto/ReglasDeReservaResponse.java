package com.tenisahora.modules.reserva.dto;

/** Las reglas vigentes de las reservas: el front las lee de acá para no tener ninguna escrita en su código. */
public record ReglasDeReservaResponse(
        int antelacionMaximaDias,
        int duracionMaximaHoras,
        int porcentajeSenia,
        int plazoCancelacionHoras,
        int porcentajeReembolsoATiempo,
        int minutosParaVencerPendientes
) {
}
