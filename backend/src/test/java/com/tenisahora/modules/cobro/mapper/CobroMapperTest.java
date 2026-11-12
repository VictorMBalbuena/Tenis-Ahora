package com.tenisahora.modules.cobro.mapper;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.torneo.entity.Torneo;
import org.junit.jupiter.api.Test;

import static com.tenisahora.modules.cobro.DatosDePrueba.cobro;
import static org.assertj.core.api.Assertions.assertThat;

class CobroMapperTest {

    private final CobroMapper mapper = new CobroMapper();

    @Test
    void aRespuesta_deUnaReserva_noLlevaOrigenDeInscripcion() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.QR, "8000.00");

        assertThat(mapper.aRespuesta(cobro).origen()).isNull();
    }

    @Test
    void aRespuesta_deUnTorneo_nombraElTorneo() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.QR, "8000.00");
        cobro.setReserva(null);
        Torneo torneo = new Torneo();
        torneo.setNombre("Apertura");
        cobro.setTorneo(torneo);

        assertThat(mapper.aRespuesta(cobro).origen()).isEqualTo("Inscripción al torneo Apertura");
    }
}
