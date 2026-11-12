package com.tenisahora.modules.administrador.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.modules.administrador.entity.Administrador;
import com.tenisahora.modules.administrador.repository.AdministradorRepository;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.PersonaResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.persona.GestionDePersonas;
import com.tenisahora.shared.persona.MapeoDePersona;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdministradorService extends GestionDePersonas<Administrador, PersonaResponse> {

    private final AdministradorRepository administradores;
    private final MapeoDePersona mapeo;

    public AdministradorService(AdministradorRepository repositorio, MapeoDePersona mapeo, DirectorioDeCuentas directorio,
                                PasswordEncoder passwordEncoder, ApplicationEventPublisher eventos, PdfService pdfService) {
        super(repositorio, mapeo, directorio, passwordEncoder, eventos, pdfService);
        this.administradores = repositorio;
        this.mapeo = mapeo;
    }

    @Override
    protected Administrador nueva() {
        return new Administrador();
    }

    @Override
    protected PersonaResponse aRespuesta(Administrador administrador) {
        return mapeo.aRespuesta(administrador);
    }

    @Override
    protected String tituloDeFicha() {
        return "Ficha de administrador";
    }

    /** Solo un administrador crea, modifica o elimina cuentas de administrador. */
    @Override
    protected void exigirPermiso(Actor actor) {
        if (!actor.esAdministrador()) {
            throw new AccessDeniedException("Solo un administrador puede gestionar cuentas de administrador");
        }
    }

    @Override
    protected void asegurarQueSePuedeEliminar(Administrador administrador, Actor actor) {
        super.asegurarQueSePuedeEliminar(administrador, actor);
        if (Boolean.TRUE.equals(administrador.getActivo()) && administradores.countByActivoTrue() <= 1) {
            throw new ReglaDeNegocioException(CodigoError.PERSONA_ULTIMO_ADMINISTRADOR,
                    "No se puede eliminar al único administrador del club");
        }
    }
}
