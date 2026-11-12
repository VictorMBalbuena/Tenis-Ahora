package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.CanchaRequest;
import com.tenisahora.modules.cancha.dto.CanchaResponse;
import com.tenisahora.modules.cancha.dto.CanchaUpdateRequest;
import com.tenisahora.modules.cancha.dto.EstadoCancha;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.entity.EstadoDisponibilidad;
import com.tenisahora.modules.cancha.mapper.CanchaMapper;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.cancha.repository.CanchaSpecs;
import com.tenisahora.modules.cancha.repository.DisponibilidadRepository;
import com.tenisahora.modules.cancha.repository.TipoCanchaRepository;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.util.Textos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Set;

@Slf4j
@Service
public class CanchaServiceImpl implements CanchaService {

    private static final String SIN_FILTRO_DE_NOMBRE = "%";

    private final CanchaRepository canchaRepository;
    private final TipoCanchaRepository tipoCanchaRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final CanchaPorId canchaPorId;
    private final CanchaMapper canchaMapper;
    private final Clock clock;

    @Autowired
    public CanchaServiceImpl(CanchaRepository canchaRepository,
                             TipoCanchaRepository tipoCanchaRepository,
                             DisponibilidadRepository disponibilidadRepository,
                             CanchaPorId canchaPorId,
                             CanchaMapper canchaMapper,
                             Clock clock) {
        this.canchaRepository = canchaRepository;
        this.tipoCanchaRepository = tipoCanchaRepository;
        this.disponibilidadRepository = disponibilidadRepository;
        this.canchaPorId = canchaPorId;
        this.canchaMapper = canchaMapper;
        this.clock = clock;
    }

    public CanchaServiceImpl(CanchaRepository canchaRepository,
                             DisponibilidadRepository disponibilidadRepository,
                             CanchaPorId canchaPorId,
                             CanchaMapper canchaMapper,
                             Clock clock) {
        this(canchaRepository, null, disponibilidadRepository, canchaPorId, canchaMapper, clock);
    }

    // ---------- Alta (CU06) ----------

    @Override
    @Transactional
    public CanchaResponse registrar(CanchaRequest request, Long actorId) {
        asegurarIdentificadorLibre(request.identificador(), null);
        Cancha cancha = canchaMapper.aEntidad(request);
        if (request.tipoCanchaId() != null && tipoCanchaRepository != null) {
            tipoCanchaRepository.findByIdAndActivoTrue(request.tipoCanchaId())
                    .ifPresent(tc -> {
                        cancha.setTipoCancha(tc);
                        cancha.setCapacidad(tc.getCapacidad());
                        cancha.setTipoSuperficie(tc.getSuperficie());
                    });
        }
        Cancha guardada = canchaRepository.saveAndFlush(cancha);
        log.info("Cancha {} registrada por la persona {}", guardada.getId(), actorId);
        return canchaMapper.aRespuesta(guardada, false);
    }

    // ---------- Consulta (CU08) ----------

    @Override
    @Transactional(readOnly = true)
    public CanchaResponse consultar(Long id) {
        Cancha cancha = canchaPorId.existente(id);
        return canchaMapper.aRespuesta(cancha, bloqueadasAhora(Set.of(id)).contains(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CanchaResponse> buscar(String superficie, String identificador, EstadoCancha estado, int page, int size) {
        return buscar(superficie, null, identificador, estado, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CanchaResponse> buscar(String superficie, String superficieTexto, String identificador, EstadoCancha estado, int page, int size) {
        PageRequest pagina = PageRequest.of(page, size, Sort.by("identificador"));
        String patron = identificador == null || identificador.isBlank()
                ? SIN_FILTRO_DE_NOMBRE : Textos.patronContiene(identificador.strip());

        Specification<Cancha> filtro = CanchaSpecs.identificadorContiene(patron);
        String sup = (superficie != null && !superficie.isBlank()) ? superficie : superficieTexto;
        if (sup != null && !sup.isBlank()) {
            filtro = filtro.and(CanchaSpecs.deSuperficie(sup.strip()));
        }
        if (estado != null) {
            filtro = filtro.and(filtroPorEstado(estado));
        }

        Page<Cancha> canchas = canchaRepository.findAll(filtro, pagina);
        Set<Long> bloqueadas = bloqueadasAhora(canchas.stream().map(Cancha::getId).toList());
        return PageResponse.de(canchas, cancha -> canchaMapper.aRespuesta(cancha, bloqueadas.contains(cancha.getId())));
    }

    private Specification<Cancha> filtroPorEstado(EstadoCancha estado) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate fecha = ahora.toLocalDate();
        LocalTime hora = ahora.toLocalTime();
        return switch (estado) {
            case DESHABILITADA -> CanchaSpecs.activa(false);
            case MANTENIMIENTO -> CanchaSpecs.activa(true).and(CanchaSpecs.enMantenimientoAhora(fecha, hora));
            case DISPONIBLE -> CanchaSpecs.activa(true).and(Specification.not(CanchaSpecs.enMantenimientoAhora(fecha, hora)));
        };
    }

    // ---------- Modificación (CU07) ----------

    @Override
    @Transactional
    public CanchaResponse modificar(Long id, CanchaUpdateRequest request, Long actorId) {
        Cancha cancha = canchaPorId.existente(id);
        if (request.identificador() != null) {
            asegurarIdentificadorLibre(request.identificador(), id);
        }
        canchaMapper.aplicar(request, cancha);
        if (request.tipoCanchaId() != null) {
            if (request.tipoCanchaId() <= 0) {
                cancha.setTipoCancha(null);
            } else if (tipoCanchaRepository != null) {
                tipoCanchaRepository.findByIdAndActivoTrue(request.tipoCanchaId())
                        .ifPresent(tc -> {
                            cancha.setTipoCancha(tc);
                            cancha.setCapacidad(tc.getCapacidad());
                            cancha.setTipoSuperficie(tc.getSuperficie());
                        });
            }
        }
        canchaRepository.saveAndFlush(cancha);
        log.info("Cancha {} modificada por la persona {}", id, actorId);
        return canchaMapper.aRespuesta(cancha, bloqueadasAhora(Set.of(id)).contains(id));
    }

    // ---------- Auxiliares ----------

    /** El identificador no puede repetirse (sin distinguir mayúsculas); al modificar, la propia cancha no cuenta. */
    private void asegurarIdentificadorLibre(String identificador, Long idPropio) {
        boolean repetido = idPropio == null
                ? canchaRepository.existsByIdentificadorIgnoreCase(identificador)
                : canchaRepository.existsByIdentificadorIgnoreCaseAndIdNot(identificador, idPropio);
        if (repetido) {
            throw new ConflictoException(CodigoError.CANCHA_IDENTIFICADOR_DUPLICADO, "Ya existe una cancha con ese identificador");
        }
    }

    /** Ids de las canchas que en este momento tienen un bloqueo por mantenimiento. */
    private Set<Long> bloqueadasAhora(Collection<Long> canchaIds) {
        if (canchaIds.isEmpty()) {
            return Set.of();
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        return disponibilidadRepository.canchasBloqueadasEn(canchaIds, EstadoDisponibilidad.MANTENIMIENTO,
                ahora.toLocalDate(), ahora.toLocalTime());
    }
}
