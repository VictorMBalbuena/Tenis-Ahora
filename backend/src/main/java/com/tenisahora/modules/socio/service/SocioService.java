package com.tenisahora.modules.socio.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.infrastructure.pdf.PdfService.CampoFicha;
import com.tenisahora.modules.socio.dto.RegistroSocioRequest;
import com.tenisahora.modules.socio.dto.SocioResponse;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.persona.GestionDePersonas;
import com.tenisahora.shared.persona.MapeoDePersona;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Socios del club (CU01 a CU05) y el autorregistro de una persona como socio (CU74). */
@Service
public class SocioService extends GestionDePersonas<Socio, SocioResponse> {

    private final MapeoDePersona mapeo;

    public SocioService(SocioRepository repositorio, MapeoDePersona mapeo, DirectorioDeCuentas directorio,
                        PasswordEncoder passwordEncoder, ApplicationEventPublisher eventos, PdfService pdfService) {
        super(repositorio, mapeo, directorio, passwordEncoder, eventos, pdfService);
        this.mapeo = mapeo;
    }

    /** CU74: una persona crea su propia cuenta. Siempre nace como socio, sin importar qué envíe. */
    @Transactional
    public SocioResponse registrarse(RegistroSocioRequest request) {
        return crear(new DatosAlta(request.nombre(), request.apellido(), request.dni(), null,
                request.calle(), request.numeroCalle(), request.codigoArea(), request.numeroTelefono(),
                request.email(), request.password()), "autorregistro");
    }

    @Override
    protected Socio nueva() {
        return new Socio();
    }

    @Override
    protected SocioResponse aRespuesta(Socio socio) {
        return SocioResponse.de(mapeo.aRespuesta(socio), socio.getNumeroSocio());
    }

    @Override
    protected String tituloDeFicha() {
        return "Ficha de socio";
    }

    /** Único rol con un dato propio en /me. */
    @Override
    protected String numeroSocioDe(Socio socio) {
        return socio.getNumeroSocio();
    }

    @Override
    protected List<CampoFicha> camposPropios(Socio socio) {
        return List.of(new CampoFicha("Número de socio", socio.getNumeroSocio()));
    }

    /** El número de socio sale del id: no hay carreras entre dos altas simultáneas. */
    @Override
    protected void alGuardarPorPrimeraVez(Socio socio) {
        socio.setNumeroSocio(String.format("S-%04d", socio.getId()));
    }
}
