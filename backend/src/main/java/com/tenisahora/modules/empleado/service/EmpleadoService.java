package com.tenisahora.modules.empleado.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.infrastructure.pdf.PdfService.CampoFicha;
import com.tenisahora.modules.empleado.entity.Empleado;
import com.tenisahora.modules.empleado.repository.EmpleadoRepository;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.dto.PersonaResponse;
import com.tenisahora.shared.persona.GestionDePersonas;
import com.tenisahora.shared.persona.MapeoDePersona;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EmpleadoService extends GestionDePersonas<Empleado, PersonaResponse> {

    private final MapeoDePersona mapeo;

    public EmpleadoService(EmpleadoRepository repositorio, MapeoDePersona mapeo, DirectorioDeCuentas directorio,
                           PasswordEncoder passwordEncoder, ApplicationEventPublisher eventos, PdfService pdfService) {
        super(repositorio, mapeo, directorio, passwordEncoder, eventos, pdfService);
        this.mapeo = mapeo;
    }

    @Override
    protected Empleado nueva() {
        Empleado empleado = new Empleado();
        empleado.setFechaContratacion(LocalDate.now());
        return empleado;
    }

    @Override
    protected PersonaResponse aRespuesta(Empleado empleado) {
        return mapeo.aRespuesta(empleado);
    }

    @Override
    protected String tituloDeFicha() {
        return "Ficha de empleado";
    }

    @Override
    protected List<CampoFicha> camposPropios(Empleado empleado) {
        return List.of(new CampoFicha("Fecha de contratación", formatear(empleado.getFechaContratacion())));
    }
}
