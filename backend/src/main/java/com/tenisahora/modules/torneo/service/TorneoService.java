package com.tenisahora.modules.torneo.service;

import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.torneo.dto.CrearTorneoRequest;
import com.tenisahora.modules.torneo.dto.ModificarTorneoRequest;
import com.tenisahora.modules.torneo.dto.TorneoResponse;
import com.tenisahora.modules.torneo.entity.EstadoTorneo;
import com.tenisahora.modules.torneo.entity.Modalidad;
import com.tenisahora.modules.torneo.entity.TipoCompetencia;
import com.tenisahora.modules.torneo.entity.Torneo;
import com.tenisahora.modules.torneo.repository.InscripcionRepository;
import com.tenisahora.modules.torneo.repository.TorneoRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TorneoService {

    private final TorneoRepository torneoRepository;
    private final CanchaRepository canchaRepository;
    private final InscripcionRepository inscripcionRepository;

    public TorneoService(
            TorneoRepository torneoRepository,
            CanchaRepository canchaRepository,
            InscripcionRepository inscripcionRepository
    ) {
        this.torneoRepository = torneoRepository;
        this.canchaRepository = canchaRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    @Transactional
    public TorneoResponse crearTorneo(CrearTorneoRequest req) {
        if (req.fechaFin().isBefore(req.fechaInicio())) {
            throw new SolicitudInvalidaException(CodigoError.TORNEO_FECHAS_INVALIDAS, "La fecha de fin no puede ser anterior a la de inicio");
        }

        Torneo torneo = new Torneo();
        torneo.setNombre(req.nombre());
        torneo.setTipo(req.tipoOTorneo());
        if (req.canchaId() != null) {
            torneo.setCancha(buscarCancha(req.canchaId()));
        }
        torneo.setCategoria(req.categoria());
        torneo.setModalidad(req.modalidad());
        torneo.setTipoSuperficie(req.tipoSuperficie());
        torneo.setFechaInicio(req.fechaInicio());
        torneo.setFechaFin(req.fechaFin());
        torneo.setEstado(EstadoTorneo.PLANIFICADO);
        torneo.setCostoInscripcion(req.costoInscripcion());
        torneo.setReglamento(req.reglamento());

        return TorneoResponse.de(torneoRepository.save(torneo));
    }

    public List<TorneoResponse> listarTorneos() {
        return torneoRepository.findAll().stream().map(this::conInscriptos).toList();
    }

    public TorneoResponse obtenerTorneo(Long id) {
        return conInscriptos(buscarTorneoEntidad(id));
    }

    @Transactional
    public TorneoResponse modificarTorneo(Long id, ModificarTorneoRequest req) {
        Torneo torneo = buscarTorneoEntidad(id);
        if (torneo.getEstado() != EstadoTorneo.PLANIFICADO) {
            throw new SolicitudInvalidaException(CodigoError.TORNEO_ESTADO_INVALIDO, "Solo se pueden modificar torneos en estado PLANIFICADO");
        }

        validarCambioModalidad(id, torneo, req.modalidad());
        actualizarFechasYNombre(torneo, req);
        if (req.canchaId() != null) torneo.setCancha(buscarCancha(req.canchaId()));
        validarCambioDeTipo(id, torneo, req.tipo());
        if (req.tipo() != null) torneo.setTipo(req.tipo());
        if (req.categoria() != null) torneo.setCategoria(req.categoria());
        if (req.modalidad() != null) torneo.setModalidad(req.modalidad());
        if (req.tipoSuperficie() != null) torneo.setTipoSuperficie(req.tipoSuperficie());
        if (req.costoInscripcion() != null) torneo.setCostoInscripcion(req.costoInscripcion());
        if (req.reglamento() != null) torneo.setReglamento(req.reglamento());

        return TorneoResponse.de(torneoRepository.save(torneo));
    }

    @Transactional
    public void eliminarTorneo(Long id) {
        Torneo torneo = buscarTorneoEntidad(id);
        if (torneo.getEstado() != EstadoTorneo.PLANIFICADO) {
            throw new ConflictoException(CodigoError.TORNEO_ESTADO_INVALIDO, "Solo se pueden eliminar torneos en estado PLANIFICADO");
        }
        if (!inscripcionRepository.findByTorneoId(id).isEmpty()) {
            throw new ConflictoException(CodigoError.TORNEO_ESTADO_INVALIDO, "No se puede eliminar un torneo con inscriptos");
        }
        torneoRepository.delete(torneo);
    }

    private TorneoResponse conInscriptos(Torneo torneo) {
        return TorneoResponse.de(torneo, inscripcionRepository.findByTorneoId(torneo.getId()).size());
    }

    public Torneo buscarTorneoEntidad(Long id) {
        return torneoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.TORNEO_NO_ENCONTRADO, "Torneo no encontrado"));
    }

    private Cancha buscarCancha(Long canchaId) {
        return canchaRepository.findById(canchaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.CANCHA_NO_ENCONTRADA, "Cancha no encontrada"));
    }

    /**
     * El tipo define cómo se arma el fixture, así que no se cambia con gente ya anotada: quien se inscribió a
     * una liga no se anotó para quedar eliminado en el primer partido.
     */
    private void validarCambioDeTipo(Long id, Torneo torneo, TipoCompetencia nuevoTipo) {
        if (nuevoTipo != null && nuevoTipo != torneo.getTipo() && !inscripcionRepository.findByTorneoId(id).isEmpty()) {
            throw new ConflictoException(CodigoError.TORNEO_ESTADO_INVALIDO,
                    "No se puede pasar de torneo a liga (ni al revés) con gente ya inscripta");
        }
    }

    private void validarCambioModalidad(Long id, Torneo torneo, Modalidad nuevaModalidad) {
        if (nuevaModalidad != null && nuevaModalidad != torneo.getModalidad() && !inscripcionRepository.findByTorneoId(id).isEmpty()) {
            throw new ConflictoException(CodigoError.TORNEO_ESTADO_INVALIDO, "No se puede cambiar la modalidad de un torneo con inscriptos");
        }
    }

    private void actualizarFechasYNombre(Torneo torneo, ModificarTorneoRequest req) {
        LocalDate inicio = req.fechaInicio() != null ? req.fechaInicio() : torneo.getFechaInicio();
        LocalDate fin = req.fechaFin() != null ? req.fechaFin() : torneo.getFechaFin();
        if (fin.isBefore(inicio)) {
            throw new SolicitudInvalidaException(CodigoError.TORNEO_FECHAS_INVALIDAS, "La fecha de fin no puede ser anterior a la de inicio");
        }
        torneo.setFechaInicio(inicio);
        torneo.setFechaFin(fin);
        if (req.nombre() != null) torneo.setNombre(req.nombre());
    }
}
