package com.tenisahora.modules.torneo.service;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.modules.torneo.dto.CargarResultadoRequest;
import com.tenisahora.modules.torneo.dto.PartidoResponse;
import com.tenisahora.modules.torneo.dto.PosicionResponse;
import com.tenisahora.modules.torneo.entity.EstadoInscripcionTorneo;
import com.tenisahora.modules.torneo.entity.EstadoTorneo;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.entity.Partido;
import com.tenisahora.modules.torneo.entity.TipoCompetencia;
import com.tenisahora.modules.torneo.entity.Torneo;
import com.tenisahora.modules.torneo.repository.InscripcionRepository;
import com.tenisahora.modules.torneo.repository.PartidoRepository;
import com.tenisahora.modules.torneo.repository.TorneoRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class FixtureService {

    private final TorneoRepository torneoRepository;
    private final PartidoRepository partidoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PdfService pdfService;

    public FixtureService(
            TorneoRepository torneoRepository,
            PartidoRepository partidoRepository,
            InscripcionRepository inscripcionRepository,
            PdfService pdfService
    ) {
        this.torneoRepository = torneoRepository;
        this.partidoRepository = partidoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.pdfService = pdfService;
    }

    /**
     * Arma el fixture. Un torneo se juega por eliminación directa y una liga, todos contra todos: lo único que
     * cambia entre los dos tipos de competencia es esto y cómo se avanza al cargar un resultado.
     */
    @Transactional
    public List<PartidoResponse> generarFixture(Long torneoId) {
        Torneo torneo = buscarTorneo(torneoId);
        validarNoHayFixturePrevio(torneoId);
        validarEstadoParaFixture(torneo);

        // Solo juegan los que confirmaron la inscripción, en una liga igual que en un torneo.
        List<Inscripcion> inscriptos = inscripcionRepository.findByTorneoId(torneoId).stream()
                .filter(i -> i.getEstado() == EstadoInscripcionTorneo.CONFIRMADA)
                .toList();
        List<Partido> partidos = esLiga(torneo)
                ? crearPartidosDeLiga(torneo, inscriptos)
                : crearPartidosDeTorneo(torneo, inscriptos);
        partidoRepository.saveAll(partidos);

        torneo.setEstado(EstadoTorneo.EN_CURSO);
        torneoRepository.save(torneo);

        return partidos.stream().map(PartidoResponse::de).toList();
    }

    /**
     * La tabla de posiciones de una liga (los partidos ganados mandan; desempatan los sets y los games).
     * En un torneo no tiene sentido: ahí el que pierde queda afuera y el orden lo da el cuadro.
     */
    public List<PosicionResponse> tablaDePosiciones(Long torneoId) {
        Torneo torneo = buscarTorneo(torneoId);
        if (!esLiga(torneo)) {
            throw new SolicitudInvalidaException(CodigoError.TORNEO_ESTADO_INVALIDO,
                    "La tabla de posiciones es de las ligas. Un torneo se sigue por su cuadro");
        }
        return TablaDePosiciones.calcular(
                inscripcionRepository.findByTorneoId(torneoId),
                partidoRepository.findByTorneoIdOrderByRondaAscOrdenAsc(torneoId));
    }

    private boolean esLiga(Torneo torneo) {
        return torneo.getTipo() != null && torneo.getTipo().esLiga();
    }

    /**
     * Eliminación directa: hace falta una potencia de 2 para que el cuadro cierre sin que nadie pase de arranque.
     */
    private List<Partido> crearPartidosDeTorneo(Torneo torneo, List<Inscripcion> inscriptos) {
        int n = inscriptos.size();
        if (n < 2 || (n & (n - 1)) != 0) {
            throw new SolicitudInvalidaException(CodigoError.CUPOS_INSUFICIENTES, "La cantidad de inscriptos debe ser al menos 2 y potencia de 2");
        }
        return crearPartidosFixture(torneo, inscriptos, n);
    }

    /**
     * Todos contra todos: sirve cualquier cantidad desde 2. La ronda del partido es el número de fecha, así se
     * reusa el mismo orden de listado que el cuadro de un torneo.
     */
    private List<Partido> crearPartidosDeLiga(Torneo torneo, List<Inscripcion> inscriptos) {
        if (inscriptos.size() < 2) {
            throw new SolicitudInvalidaException(CodigoError.CUPOS_INSUFICIENTES, "Una liga necesita al menos 2 inscriptos");
        }
        return CalendarioDeLiga.armar(inscriptos.size()).stream()
                .map(cruce -> instanciarPartido(torneo, cruce.fecha(), cruce.orden(),
                        inscriptos.get(cruce.localIndice()), inscriptos.get(cruce.visitanteIndice())))
                .toList();
    }

    public List<PartidoResponse> listarPartidos(Long torneoId) {
        buscarTorneo(torneoId);
        return partidoRepository.findByTorneoIdOrderByRondaAscOrdenAsc(torneoId).stream()
                .map(PartidoResponse::de)
                .toList();
    }

    public byte[] generarPdfResultados(Long torneoId) {
        Torneo torneo = buscarTorneo(torneoId);
        List<Partido> partidos = partidoRepository.findByTorneoIdOrderByRondaAscOrdenAsc(torneoId);

        List<PdfService.CampoFicha> campos = partidos.stream()
                .map(this::mapearCampoPartido)
                .toList();

        String etiqueta = torneo.getTipo() == null ? TipoCompetencia.TORNEO.etiqueta() : torneo.getTipo().etiqueta();
        String titulo = etiqueta + ": " + (torneo.getNombre() != null ? torneo.getNombre() : etiqueta + " #" + torneo.getId());
        return pdfService.generarFicha(titulo, campos);
    }

    private PdfService.CampoFicha mapearCampoPartido(Partido p) {
        String etiqueta = "Ronda " + p.getRonda() + " - Partido " + p.getOrden();
        String nomA = p.getParticipanteA() != null ? PartidoResponse.formatearParticipante(p.getParticipanteA()) : "A definir";
        String nomB = p.getParticipanteB() != null ? PartidoResponse.formatearParticipante(p.getParticipanteB()) : "A definir";
        String sets = p.getSets() != null ? p.getSets() : "-";
        String res = p.getResultado() != null ? p.getResultado() : "Pendiente";
        String valor = nomA + " vs " + nomB + " | Sets: " + sets + " | Resultado: " + res;
        return new PdfService.CampoFicha(etiqueta, valor);
    }

    @Transactional
    public PartidoResponse cargarResultado(Long torneoId, Long partidoId, CargarResultadoRequest req) {
        Torneo torneo = buscarTorneo(torneoId);
        Partido partido = buscarPartido(partidoId);
        validarPartidoDisputable(torneoId, partido);

        ValidadorResultadoTenis.ResultadoValidado validado = ValidadorResultadoTenis.validar(req.sets());
        if (!validado.valido()) {
            throw new SolicitudInvalidaException(CodigoError.RESULTADO_INVALIDO, validado.error());
        }

        Inscripcion ganador = validado.ganador() == 1 ? partido.getParticipanteA() : partido.getParticipanteB();
        partido.setSets(req.sets().trim());
        partido.setResultado("Gana " + PartidoResponse.formatearParticipante(ganador));

        if (esLiga(torneo)) {
            // En una liga nadie queda afuera: el resultado solo suma a la tabla
            partidoRepository.save(partido);
            finalizarSiSeJugaronTodos(torneo);
        } else {
            avanzarGanadorOFinalizar(torneo, partido, ganador);
            partidoRepository.save(partido);
        }
        return PartidoResponse.de(partido);
    }

    private List<Partido> crearPartidosFixture(Torneo torneo, List<Inscripcion> inscriptos, int n) {
        List<Partido> partidos = new ArrayList<>();
        int rondas = 31 - Integer.numberOfLeadingZeros(n);
        for (int i = 0; i < n / 2; i++) {
            partidos.add(instanciarPartido(torneo, 1, i + 1, inscriptos.get(2 * i), inscriptos.get(2 * i + 1)));
        }
        for (int r = 2; r <= rondas; r++) {
            int partidosEnRonda = n >> r;
            for (int i = 0; i < partidosEnRonda; i++) {
                partidos.add(instanciarPartido(torneo, r, i + 1, null, null));
            }
        }
        return partidos;
    }

    private Partido instanciarPartido(Torneo t, int ronda, int orden, Inscripcion a, Inscripcion b) {
        Partido p = new Partido();
        p.setTorneo(t);
        p.setRonda(ronda);
        p.setOrden(orden);
        p.setParticipanteA(a);
        p.setParticipanteB(b);
        p.setFecha(t.getFechaInicio());
        return p;
    }

    /** Una liga termina cuando se jugaron todas sus fechas, no cuando hay una final. */
    private void finalizarSiSeJugaronTodos(Torneo torneo) {
        boolean quedaAlguno = partidoRepository.findByTorneoIdOrderByRondaAscOrdenAsc(torneo.getId()).stream()
                .anyMatch(p -> p.getResultado() == null);
        if (!quedaAlguno) {
            torneo.setEstado(EstadoTorneo.FINALIZADO);
            torneoRepository.save(torneo);
        }
    }

    private void avanzarGanadorOFinalizar(Torneo torneo, Partido partido, Inscripcion ganador) {
        int sigRonda = partido.getRonda() + 1;
        int sigOrden = (partido.getOrden() + 1) / 2;
        Optional<Partido> sigOpt = partidoRepository.findByTorneoIdAndRondaAndOrden(torneo.getId(), sigRonda, sigOrden);

        if (sigOpt.isEmpty()) {
            torneo.setEstado(EstadoTorneo.FINALIZADO);
            torneoRepository.save(torneo);
        } else {
            Partido sig = sigOpt.get();
            if (partido.getOrden() % 2 != 0) {
                sig.setParticipanteA(ganador);
            } else {
                sig.setParticipanteB(ganador);
            }
            partidoRepository.save(sig);
        }
    }

    private void validarPartidoDisputable(Long torneoId, Partido p) {
        if (!p.getTorneo().getId().equals(torneoId)) {
            throw new RecursoNoEncontradoException(CodigoError.PARTIDO_NO_ENCONTRADO, "El partido no pertenece al torneo");
        }
        if (p.getResultado() != null) {
            throw new SolicitudInvalidaException(CodigoError.PARTIDO_NO_DISPUTABLE, "El resultado de este partido ya fue registrado");
        }
        if (p.getParticipanteA() == null || p.getParticipanteB() == null) {
            throw new SolicitudInvalidaException(CodigoError.PARTIDO_NO_DISPUTABLE, "El partido no tiene ambos participantes definidos");
        }
    }

    private void validarEstadoParaFixture(Torneo t) {
        if (t.getEstado() != EstadoTorneo.PLANIFICADO) {
            throw new SolicitudInvalidaException(CodigoError.TORNEO_ESTADO_INVALIDO, "Solo se puede generar fixture en torneos planificados");
        }
    }

    private void validarNoHayFixturePrevio(Long torneoId) {
        if (!partidoRepository.findByTorneoIdOrderByRondaAscOrdenAsc(torneoId).isEmpty()) {
            throw new ConflictoException(CodigoError.FIXTURE_YA_GENERADO, "El fixture ya fue generado");
        }
    }

    public Torneo buscarTorneo(Long id) {
        return torneoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.TORNEO_NO_ENCONTRADO, "Torneo no encontrado"));
    }

    private Partido buscarPartido(Long id) {
        return partidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.PARTIDO_NO_ENCONTRADO, "Partido no encontrado"));
    }
}
