package com.tenisahora.modules.cobro.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.modules.cobro.dto.ReciboResponse;
import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import com.tenisahora.modules.cobro.entity.Recibo;
import com.tenisahora.modules.cobro.mapper.CobroMapper;
import com.tenisahora.modules.cobro.repository.ReciboRepository;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.tenisahora.modules.cobro.DatosDePrueba.cobro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaDeRecibosTest {

    @Mock
    private ReciboRepository reciboRepository;

    private ConsultaDeRecibos consulta;

    @BeforeEach
    void setUp() {
        consulta = new ConsultaDeRecibos(reciboRepository, new CobroMapper(), new PdfService());
    }

    private Recibo reciboDe(Cobro cobro) {
        Recibo recibo = new Recibo();
        recibo.setCobro(cobro);
        recibo.setNumeroRecibo("R-000007");
        recibo.setMonto(cobro.getMontoFinal());
        recibo.setConcepto("SEÑA");
        recibo.setMedioPago(cobro.getMedioPago());
        recibo.setNumeroOperacion("186778732035");
        return recibo;
    }

    @Test
    void consultar_devuelveElCobroQueLoOrigino() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.QR, "8000.00");
        when(reciboRepository.findById(1L)).thenReturn(Optional.of(reciboDe(cobro)));

        ReciboResponse respuesta = consulta.consultar(1L);

        assertThat(respuesta.numeroOperacion()).isEqualTo("186778732035");
        assertThat(respuesta.cobro().id()).isEqualTo(7L);
        assertThat(respuesta.cobro().codigo()).isEqualTo(cobro.getCodigo());
    }

    @Test
    void imprimir_generaElPdf() {
        Cobro cobro = cobro(7L, EstadoCobro.PAGADO, MedioPago.QR, "8000.00");
        when(reciboRepository.findById(1L)).thenReturn(Optional.of(reciboDe(cobro)));

        assertThat(consulta.imprimir(1L)).isNotEmpty();
    }

    @Test
    void consultar_reciboInexistente_404() {
        when(reciboRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consulta.consultar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
