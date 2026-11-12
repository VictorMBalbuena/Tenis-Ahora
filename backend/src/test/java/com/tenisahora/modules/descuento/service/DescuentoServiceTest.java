package com.tenisahora.modules.descuento.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.modules.descuento.dto.DescuentoRequest;
import com.tenisahora.modules.descuento.dto.DescuentoResponse;
import com.tenisahora.modules.descuento.entity.Descuento;
import com.tenisahora.modules.descuento.entity.TipoDescuento;
import com.tenisahora.modules.descuento.repository.CobroDescuentoRepository;
import com.tenisahora.modules.descuento.repository.DescuentoRepository;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DescuentoServiceTest {

    @Mock
    private DescuentoRepository descuentoRepository;

    @Mock
    private CobroDescuentoRepository cobroDescuentoRepository;

    @Mock
    private PdfService pdfService;

    @InjectMocks
    private DescuentoServiceImpl service;

    private Descuento descuento;

    @BeforeEach
    void setUp() {
        descuento = new Descuento();
        descuento.setId(1L);
        descuento.setTipo(TipoDescuento.PAQUETE_HORAS);
        descuento.setPorcentaje(new BigDecimal("15.00"));
        descuento.setCondiciones("Promo 10h");
        descuento.setActivo(true);
    }

    @Test
    void registrar_conBeneficioPorcentaje_creaDescuento() {
        DescuentoRequest req = new DescuentoRequest("PAQUETE10", TipoDescuento.PAQUETE_HORAS, new BigDecimal("15.00"), null, "Promo 10h", true);
        when(descuentoRepository.save(any(Descuento.class))).thenAnswer(invocation -> {
            Descuento d = invocation.getArgument(0);
            d.setId(1L);
            return d;
        });

        DescuentoResponse resp = service.registrar(req);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.tipo()).isEqualTo(TipoDescuento.PAQUETE_HORAS);
        assertThat(resp.porcentaje()).isEqualByComparingTo("15.00");
        assertThat(resp.montoFijo()).isNull();
        assertThat(resp.vecesAplicado()).isZero();
    }

    @Test
    void registrar_conBeneficioMontoFijo_creaDescuento() {
        DescuentoRequest req = new DescuentoRequest("ESCUELA", TipoDescuento.ESCUELA_AFILIADA, null, new BigDecimal("2500.00"), "Descuento escuelas", true);
        when(descuentoRepository.save(any(Descuento.class))).thenAnswer(invocation -> {
            Descuento d = invocation.getArgument(0);
            d.setId(2L);
            return d;
        });

        DescuentoResponse resp = service.registrar(req);

        assertThat(resp.id()).isEqualTo(2L);
        assertThat(resp.montoFijo()).isEqualByComparingTo("2500.00");
        assertThat(resp.porcentaje()).isNull();
    }

    @Test
    void registrar_conAmbosBeneficios_lanzaBeneficioInvalido() {
        DescuentoRequest req = new DescuentoRequest("PAQUETE10", TipoDescuento.PAQUETE_HORAS, new BigDecimal("10.00"), new BigDecimal("1000.00"), "Invalido", true);

        assertThatThrownBy(() -> service.registrar(req))
                .isInstanceOf(SolicitudInvalidaException.class)
                .satisfies(ex -> assertThat(((SolicitudInvalidaException) ex).getCodigoError()).isEqualTo(CodigoError.BENEFICIO_INVALIDO));
    }

    @Test
    void registrar_sinBeneficios_lanzaBeneficioInvalido() {
        DescuentoRequest req = new DescuentoRequest("PAQUETE10", TipoDescuento.PAQUETE_HORAS, null, null, "Invalido", true);

        assertThatThrownBy(() -> service.registrar(req))
                .isInstanceOf(SolicitudInvalidaException.class)
                .satisfies(ex -> assertThat(((SolicitudInvalidaException) ex).getCodigoError()).isEqualTo(CodigoError.BENEFICIO_INVALIDO));
    }

    @Test
    void registrar_conPorcentajeInvalido_lanzaBeneficioInvalido() {
        DescuentoRequest menorACero = new DescuentoRequest("PAQUETE10", TipoDescuento.PAQUETE_HORAS, new BigDecimal("0.00"), null, "0%", true);
        assertThatThrownBy(() -> service.registrar(menorACero))
                .isInstanceOf(SolicitudInvalidaException.class)
                .satisfies(ex -> assertThat(((SolicitudInvalidaException) ex).getCodigoError()).isEqualTo(CodigoError.BENEFICIO_INVALIDO));

        DescuentoRequest mayorACien = new DescuentoRequest("PAQUETE10", TipoDescuento.PAQUETE_HORAS, new BigDecimal("100.01"), null, ">100%", true);
        assertThatThrownBy(() -> service.registrar(mayorACien))
                .isInstanceOf(SolicitudInvalidaException.class)
                .satisfies(ex -> assertThat(((SolicitudInvalidaException) ex).getCodigoError()).isEqualTo(CodigoError.BENEFICIO_INVALIDO));
    }

    @Test
    void registrar_conMontoInvalido_lanzaBeneficioInvalido() {
        DescuentoRequest cero = new DescuentoRequest("PAQUETE10", TipoDescuento.PAQUETE_HORAS, null, BigDecimal.ZERO, "0 pesos", true);

        assertThatThrownBy(() -> service.registrar(cero))
                .isInstanceOf(SolicitudInvalidaException.class)
                .satisfies(ex -> assertThat(((SolicitudInvalidaException) ex).getCodigoError()).isEqualTo(CodigoError.BENEFICIO_INVALIDO));
    }

    @Test
    void registrar_conCodigoYaExistente_lanzaDescuentoDuplicado() {
        DescuentoRequest req = new DescuentoRequest("PAQUETE10", TipoDescuento.PAQUETE_HORAS, new BigDecimal("10.00"), null, "Repetido", true);
        when(descuentoRepository.existsByCodigoIgnoreCase("PAQUETE10")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(req))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigoError()).isEqualTo(CodigoError.DESCUENTO_DUPLICADO));
    }

    @Test
    void consultar_existente_devuelveDescuentoConVecesAplicado() {
        when(descuentoRepository.findById(1L)).thenReturn(Optional.of(descuento));
        when(cobroDescuentoRepository.countByDescuentoId(1L)).thenReturn(7L);

        DescuentoResponse resp = service.consultar(1L);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.vecesAplicado()).isEqualTo(7L);
    }

    @Test
    void consultar_inexistente_lanzaNoEncontrado() {
        when(descuentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getCodigoError()).isEqualTo(CodigoError.NO_ENCONTRADO));
    }

    @Test
    void buscar_conFiltros_devuelvePagina() {
        Page<Descuento> pagina = new PageImpl<>(List.of(descuento), PageRequest.of(0, 20), 1);
        when(descuentoRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(pagina);
        when(cobroDescuentoRepository.countByDescuentoId(1L)).thenReturn(3L);

        PageResponse<DescuentoResponse> resp = service.buscar(TipoDescuento.PAQUETE_HORAS, true, "promo", 0, 20);

        assertThat(resp.contenido()).hasSize(1);
        assertThat(resp.contenido().getFirst().vecesAplicado()).isEqualTo(3L);
        assertThat(resp.totalElementos()).isEqualTo(1L);
    }

    @Test
    void buscar_aplicaOrdenamientoPorIdAscendente() {
        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        when(descuentoRepository.findAll(any(Specification.class), pageRequestCaptor.capture()))
                .thenReturn(Page.empty());

        service.buscar(null, null, null, 0, 10);

        PageRequest captured = pageRequestCaptor.getValue();
        assertThat(captured.getSort().getOrderFor("id")).isNotNull();
        assertThat(captured.getSort().getOrderFor("id").isAscending()).isTrue();
    }

    @Test
    void modificar_actualizaCampos() {
        when(descuentoRepository.findById(1L)).thenReturn(Optional.of(descuento));
        when(descuentoRepository.save(any(Descuento.class))).thenReturn(descuento);
        when(cobroDescuentoRepository.countByDescuentoId(1L)).thenReturn(4L);

        DescuentoRequest req = new DescuentoRequest("LIGA20", TipoDescuento.LIGA_REGULAR, new BigDecimal("20.00"), null, "Liga act", true);
        DescuentoResponse resp = service.modificar(1L, req);

        assertThat(resp.tipo()).isEqualTo(TipoDescuento.LIGA_REGULAR);
        assertThat(resp.porcentaje()).isEqualByComparingTo("20.00");
        assertThat(descuento.getCondiciones()).isEqualTo("Liga act");
    }

    @Test
    void modificar_aUnCodigoYaUsado_lanzaDescuentoDuplicado() {
        when(descuentoRepository.findById(1L)).thenReturn(Optional.of(descuento));
        when(descuentoRepository.existsByCodigoIgnoreCaseAndIdNot("LIGA20", 1L)).thenReturn(true);

        DescuentoRequest req = new DescuentoRequest("LIGA20", TipoDescuento.LIGA_REGULAR, new BigDecimal("20.00"), null, "Liga act", true);

        assertThatThrownBy(() -> service.modificar(1L, req))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigoError()).isEqualTo(CodigoError.DESCUENTO_DUPLICADO));
    }

    @Test
    void modificar_inexistente_lanzaNoEncontrado() {
        when(descuentoRepository.findById(99L)).thenReturn(Optional.empty());
        DescuentoRequest req = new DescuentoRequest("LIGA20", TipoDescuento.LIGA_REGULAR, new BigDecimal("20.00"), null, "Liga act", true);

        assertThatThrownBy(() -> service.modificar(99L, req))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getCodigoError()).isEqualTo(CodigoError.NO_ENCONTRADO));
    }

    @Test
    void eliminar_sinCobros_eliminaFisicamente() {
        when(descuentoRepository.findById(1L)).thenReturn(Optional.of(descuento));
        when(cobroDescuentoRepository.existsByDescuentoId(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(descuentoRepository).delete(descuento);
    }

    @Test
    void eliminar_conCobrosAsociados_lanzaEnUso() {
        when(descuentoRepository.findById(1L)).thenReturn(Optional.of(descuento));
        when(cobroDescuentoRepository.existsByDescuentoId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(ConflictoException.class)
                .satisfies(ex -> assertThat(((ConflictoException) ex).getCodigoError()).isEqualTo(CodigoError.EN_USO));

        verify(descuentoRepository, never()).delete(any(Descuento.class));
    }

    @Test
    void eliminar_inexistente_lanzaNoEncontrado() {
        when(descuentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getCodigoError()).isEqualTo(CodigoError.NO_ENCONTRADO));
    }

    @Test
    void imprimir_devuelveBytesPdf() {
        when(descuentoRepository.findById(1L)).thenReturn(Optional.of(descuento));
        when(cobroDescuentoRepository.countByDescuentoId(1L)).thenReturn(2L);
        byte[] pdfEsperado = new byte[]{1, 2, 3};
        when(pdfService.generarFicha(eq("Ficha de Descuento"), any())).thenReturn(pdfEsperado);

        byte[] resultado = service.imprimir(1L);

        assertThat(resultado).isEqualTo(pdfEsperado);
    }

    @Test
    void imprimir_inexistente_lanzaNoEncontrado() {
        when(descuentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.imprimir(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .satisfies(ex -> assertThat(((RecursoNoEncontradoException) ex).getCodigoError()).isEqualTo(CodigoError.NO_ENCONTRADO));
    }
}
