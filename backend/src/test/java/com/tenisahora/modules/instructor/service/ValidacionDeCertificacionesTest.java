package com.tenisahora.modules.instructor.service;

import com.tenisahora.modules.instructor.dto.CertificacionRequest;
import com.tenisahora.modules.instructor.entity.Certificacion;
import com.tenisahora.modules.instructor.repository.CertificacionRepository;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidacionDeCertificacionesTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);

    @Mock
    private CertificacionRepository certificaciones;
    @InjectMocks
    private ValidacionDeCertificaciones validacion;


    @Test
    void exigirTitulo_vacio_422() {
        assertThatThrownBy(() -> validacion.exigirTitulo(" "))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("PROFESOR_SIN_TITULO");
    }

    @Test
    void exigirTitulo_presente_noHaceNada() {
        validacion.exigirTitulo("Profesor Nacional");
    }

    @Test
    void exigirCertificacionDeportiva_vacia_422() {
        assertThatThrownBy(() -> validacion.exigirCertificacionDeportiva(" "))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("ENTRENADOR_SIN_CERTIFICACION");
    }

    @Test
    void exigirCertificacionDeportiva_presente_noHaceNada() {
        validacion.exigirCertificacionDeportiva("Entrenador Nacional de Tenis");
    }

    @Test
    void exigirVigente_requestVencido_422() {
        CertificacionRequest vencida = new CertificacionRequest("C1", "FAT", HOY.minusYears(1), HOY.minusDays(1), null);

        assertThatThrownBy(() -> validacion.exigirVigente(vencida, HOY))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CERTIFICACION_VENCIDA");
    }

    @Test
    void exigirVigente_entidadVencida_422() {
        Certificacion certificacion = new Certificacion();
        certificacion.setFechaVencimiento(HOY.minusDays(1));

        assertThatThrownBy(() -> validacion.exigirVigente(certificacion, HOY))
                .isInstanceOf(ReglaDeNegocioException.class)
                .extracting("codigo").isEqualTo("CERTIFICACION_VENCIDA");
    }


    @Test
    void exigirNumeroLibre_sinCertificacionPropia_yaUsado_409() {
        when(certificaciones.existsByNumeroCertificacionIgnoreCase("CERT-001")).thenReturn(true);

        assertThatThrownBy(() -> validacion.exigirNumeroLibre("CERT-001", null))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("CERTIFICACION_DUPLICADA");
    }

    @Test
    void exigirNumeroLibre_esLaMismaCertificacion_noHaceNada() {
        when(certificaciones.existsByNumeroCertificacionIgnoreCaseAndIdNot("CERT-001", 5L)).thenReturn(false);

        validacion.exigirNumeroLibre("CERT-001", 5L);
    }

    @Test
    void exigirNumeroLibre_otraCertificacionLaUsa_409() {
        when(certificaciones.existsByNumeroCertificacionIgnoreCaseAndIdNot("CERT-001", 5L)).thenReturn(true);

        assertThatThrownBy(() -> validacion.exigirNumeroLibre("CERT-001", 5L))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("CERTIFICACION_DUPLICADA");
    }
}
