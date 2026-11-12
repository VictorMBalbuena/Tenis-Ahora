package com.tenisahora.modules.instructor.service;

import com.tenisahora.modules.instructor.dto.CertificacionRequest;
import com.tenisahora.modules.instructor.entity.Certificacion;
import com.tenisahora.modules.instructor.repository.CertificacionRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;


@Component
@RequiredArgsConstructor
public class ValidacionDeCertificaciones {

    private final CertificacionRepository certificaciones;

    public void exigirTitulo(String tituloHabilitante) {
        if (tituloHabilitante == null || tituloHabilitante.isBlank()) {
            throw new ReglaDeNegocioException(CodigoError.PROFESOR_SIN_TITULO,
                    "El profesor necesita un título habilitante (RD02)");
        }
    }

    public void exigirCertificacionDeportiva(String certificacionDeportiva) {
        if (certificacionDeportiva == null || certificacionDeportiva.isBlank()) {
            throw new ReglaDeNegocioException(CodigoError.ENTRENADOR_SIN_CERTIFICACION,
                    "El entrenador necesita una certificación deportiva (RD03)");
        }
    }

    public void exigirVigente(CertificacionRequest datos, LocalDate hoy) {
        if (datos.fechaVencimiento() != null && datos.fechaVencimiento().isBefore(hoy)) {
            throw new ReglaDeNegocioException(CodigoError.CERTIFICACION_VENCIDA, "La certificación está vencida");
        }
    }

    public void exigirVigente(Certificacion certificacion, LocalDate hoy) {
        if (!certificacion.estaVigente(hoy)) {
            throw new ReglaDeNegocioException(CodigoError.CERTIFICACION_VENCIDA, "La certificación está vencida");
        }
    }

    public void exigirNumeroLibre(String numeroCertificacion, Long idActual) {
        boolean enUso = idActual == null
                ? certificaciones.existsByNumeroCertificacionIgnoreCase(numeroCertificacion)
                : certificaciones.existsByNumeroCertificacionIgnoreCaseAndIdNot(numeroCertificacion, idActual);
        if (enUso) {
            throw new ConflictoException(CodigoError.CERTIFICACION_DUPLICADA,
                    "Ya existe una certificación con ese número");
        }
    }
}
