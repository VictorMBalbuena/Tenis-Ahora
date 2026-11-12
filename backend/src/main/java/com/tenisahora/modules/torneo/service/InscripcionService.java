package com.tenisahora.modules.torneo.service;

import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.modules.torneo.dto.InscribirTorneoRequest;
import com.tenisahora.modules.torneo.dto.InscripcionResponse;
import com.tenisahora.modules.torneo.entity.CategoriaTorneo;
import com.tenisahora.modules.torneo.entity.EstadoInscripcionTorneo;
import com.tenisahora.modules.torneo.entity.EstadoTorneo;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.entity.Modalidad;
import com.tenisahora.modules.torneo.entity.Torneo;
import com.tenisahora.modules.torneo.repository.InscripcionRepository;
import com.tenisahora.modules.torneo.repository.TorneoRepository;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import com.tenisahora.shared.model.Rol;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InscripcionService {

    private final TorneoRepository torneoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final SocioRepository socioRepository;

    public InscripcionService(
            TorneoRepository torneoRepository,
            InscripcionRepository inscripcionRepository,
            SocioRepository socioRepository
    ) {
        this.torneoRepository = torneoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.socioRepository = socioRepository;
    }

    @Transactional
    public InscripcionResponse inscribir(Long torneoId, InscribirTorneoRequest req) {
        return inscribir(torneoId, req, null);
    }

    @Transactional
    public InscripcionResponse inscribir(Long torneoId, InscribirTorneoRequest req, Actor actor) {
        validarAutorizacionSocio(req, actor);
        Torneo torneo = buscarTorneo(torneoId);
        validarEstado(torneo);
        validarModalidad(torneo, req);
        validarNoDuplicado(torneoId, req);

        Socio jugador1 = buscarSocio(req.jugador1Id());
        Socio pareja = req.parejaId() != null ? buscarSocio(req.parejaId()) : null;
        validarCategoria(torneo, jugador1);
        if (pareja != null) {
            validarCategoria(torneo, pareja);
        }

        int numeroEquipo = inscripcionRepository.findByTorneoId(torneoId).size() + 1;
        Inscripcion inscripcion = crearEntidadInscripcion(torneo, jugador1, pareja, numeroEquipo, req.nombreEquipo());

        return InscripcionResponse.de(inscripcionRepository.save(inscripcion));
    }

    /** Mis inscripciones a torneos, también las canceladas: es el historial del socio. */
    public List<InscripcionResponse> delSocio(Long socioId) {
        return inscripcionRepository.delSocio(socioId).stream()
                .map(i -> InscripcionResponse.de(i, ConfirmacionDeInscripcionTorneo.venceEn(i)))
                .toList();
    }

    public InscripcionResponse detalle(Long id, Actor actor) {
        Inscripcion inscripcion = inscripcionRepository.findById(id)
                .filter(i -> actor.rol() != Rol.SOCIO || i.participa(actor.id()))
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.INSCRIPCION_NO_ENCONTRADA, "Inscripción no encontrada"));
        return InscripcionResponse.de(inscripcion, ConfirmacionDeInscripcionTorneo.venceEn(inscripcion));
    }

    public List<InscripcionResponse> listarInscripciones(Long torneoId) {
        buscarTorneo(torneoId);
        return inscripcionRepository.findByTorneoId(torneoId).stream()
                .map(InscripcionResponse::de)
                .toList();
    }

    private void validarEstado(Torneo torneo) {
        if (torneo.getEstado() != EstadoTorneo.PLANIFICADO) {
            throw new SolicitudInvalidaException(CodigoError.TORNEO_ESTADO_INVALIDO, "El torneo no admite inscripciones");
        }
    }

    /**
     * RF47: si el torneo es masculino o femenino, hace falta saber a cuál corresponde cada jugador.
     *
     * <p>Un torneo mixto no distingue, así que no se le pide el sexo a nadie: sería exigir un dato
     * del perfil para una verificación que nunca se hace.
     */
    private void validarCategoria(Torneo torneo, Socio socio) {
        if (torneo.getCategoria() == null || torneo.getCategoria() == CategoriaTorneo.MIXTO) {
            return;
        }
        if (socio.getSexo() == null) {
            throw new ReglaDeNegocioException(CodigoError.CATEGORIA_NO_CORRESPONDE,
                    socio.getNombre() + " tiene que completar el sexo en Mi perfil para inscribirse a un torneo");
        }
        if (!socio.getSexo().name().equals(torneo.getCategoria().name())) {
            throw new ReglaDeNegocioException(CodigoError.CATEGORIA_NO_CORRESPONDE,
                    socio.getNombre() + " no cumple la categoría " + torneo.getCategoria() + " de este torneo");
        }
    }

    private void validarAutorizacionSocio(InscribirTorneoRequest req, Actor actor) {
        if (actor != null && actor.rol() == Rol.SOCIO) {
            boolean esTitular = actor.id().equals(req.jugador1Id());
            boolean esPareja = actor.id().equals(req.parejaId());
            if (!esTitular && !esPareja) {
                throw new AccessDeniedException("Un socio solo puede inscribirse a sí mismo");
            }
        }
    }

    private void validarModalidad(Torneo torneo, InscribirTorneoRequest req) {
        if (torneo.getModalidad() == Modalidad.SINGLE && req.parejaId() != null) {
            throw new SolicitudInvalidaException(CodigoError.MODALIDAD_INVALIDA, "Un torneo single no admite pareja");
        }
        if (torneo.getModalidad() == Modalidad.PAREJA && (req.parejaId() == null || req.parejaId().equals(req.jugador1Id()))) {
            throw new SolicitudInvalidaException(CodigoError.MODALIDAD_INVALIDA, "Un torneo en parejas exige una pareja distinta del jugador 1");
        }
    }

    private void validarNoDuplicado(Long torneoId, InscribirTorneoRequest req) {
        if (inscripcionRepository.existsSocioEnTorneo(torneoId, req.jugador1Id())) {
            throw new ConflictoException(CodigoError.INSCRIPCION_DUPLICADA, "El socio ya está inscripto en este torneo");
        }
        if (req.parejaId() != null && inscripcionRepository.existsSocioEnTorneo(torneoId, req.parejaId())) {
            throw new ConflictoException(CodigoError.INSCRIPCION_DUPLICADA, "La pareja ya está inscripta en este torneo");
        }
    }

    private Inscripcion crearEntidadInscripcion(Torneo t, Socio j1, Socio par, int equipo, String nombreEquipo) {
        Inscripcion i = new Inscripcion();
        i.setTorneo(t);
        i.setJugador1(j1);
        i.setPareja(par);
        i.setEquipo(equipo);
        i.setNombreEquipo(nombreEquipo);
        i.setFechaInscripcion(LocalDate.now());
        i.setFechaCreacion(LocalDateTime.now());
        i.setMontoTotal(t.getCostoInscripcion());
        boolean conArancel = t.getCostoInscripcion() != null && t.getCostoInscripcion().signum() > 0;
        i.setEstado(conArancel ? EstadoInscripcionTorneo.PENDIENTE_PAGO : EstadoInscripcionTorneo.CONFIRMADA);
        return i;
    }

    private Torneo buscarTorneo(Long id) {
        return torneoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.TORNEO_NO_ENCONTRADO, "Torneo no encontrado"));
    }

    private Socio buscarSocio(Long id) {
        return socioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Socio no encontrado"));
    }
}
