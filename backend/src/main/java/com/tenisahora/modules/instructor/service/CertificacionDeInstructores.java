package com.tenisahora.modules.instructor.service;

import com.tenisahora.modules.instructor.dto.CertificacionRequest;
import com.tenisahora.modules.instructor.dto.ValidacionRequest;
import com.tenisahora.modules.instructor.entity.Certificacion;
import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.modules.instructor.entity.Instructor;
import com.tenisahora.modules.instructor.repository.CertificacionRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CertificacionDeInstructores {

    private final CertificacionRepository certificaciones;
    private final ValidacionDeCertificaciones validacion;
    private final Clock clock;

    public void registrar(Instructor instructor, CertificacionRequest datos) {
        validacion.exigirVigente(datos, LocalDate.now(clock));
        validacion.exigirNumeroLibre(datos.numeroCertificacion(), null);
        instructor.setCertificacion(certificaciones.save(construir(datos)));
        instructor.setEstadoValidacion(EstadoValidacion.PENDIENTE);
    }

    public void reemplazar(Instructor instructor, CertificacionRequest datos) {
        Certificacion actual = instructor.getCertificacion();
        validacion.exigirVigente(datos, LocalDate.now(clock));
        validacion.exigirNumeroLibre(datos.numeroCertificacion(), actual == null ? null : actual.getId());
        if (actual != null) {
            copiar(actual, datos);
            instructor.setCertificacion(certificaciones.save(actual));
        } else {
            instructor.setCertificacion(certificaciones.save(construir(datos)));
        }
        reiniciarValidacion(instructor);
    }

    public void decidir(Instructor instructor, ValidacionRequest decision, Actor actor) {
        if (!instructor.getEstadoValidacion().puedeValidarse()) {
            if (Boolean.TRUE.equals(decision.aprobada())) {
                return;
            }
            throw new ReglaDeNegocioException(CodigoError.CERTIFICACION_YA_DECIDIDA,
                    "La certificación ya fue validada: no se puede rechazar");
        }
        if (Boolean.TRUE.equals(decision.aprobada())) {
            aprobar(instructor);
        } else {
            rechazar(instructor, decision.motivo());
        }
        instructor.setFechaValidacion(LocalDate.now(clock));
        instructor.setValidadoPorId(actor.id());
    }

    private void aprobar(Instructor instructor) {
        validacion.exigirVigente(instructor.getCertificacion(), LocalDate.now(clock));
        instructor.setEstadoValidacion(EstadoValidacion.VALIDADO);
        instructor.setMotivoRechazo(null);
    }

    private void rechazar(Instructor instructor, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException(CodigoError.MOTIVO_DE_RECHAZO_REQUERIDO,
                    "Hay que indicar el motivo del rechazo");
        }
        instructor.setEstadoValidacion(EstadoValidacion.RECHAZADA);
        instructor.setMotivoRechazo(motivo);
    }
    
    public void reiniciarValidacion(Instructor instructor) {
        instructor.setEstadoValidacion(EstadoValidacion.PENDIENTE);
        instructor.setFechaValidacion(null);
        instructor.setValidadoPorId(null);
        instructor.setMotivoRechazo(null);
    }

    private Certificacion construir(CertificacionRequest datos) {
        Certificacion certificacion = new Certificacion();
        copiar(certificacion, datos);
        return certificacion;
    }

    private void copiar(Certificacion certificacion, CertificacionRequest datos) {
        certificacion.setNumeroCertificacion(datos.numeroCertificacion());
        certificacion.setEntidadEmisora(datos.entidadEmisora());
        certificacion.setFechaEmision(datos.fechaEmision());
        certificacion.setFechaVencimiento(datos.fechaVencimiento());
        certificacion.setArchivoTituloUrl(datos.archivoTituloUrl());
    }
}
