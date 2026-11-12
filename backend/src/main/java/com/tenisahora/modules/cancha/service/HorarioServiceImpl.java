package com.tenisahora.modules.cancha.service;

import com.tenisahora.modules.cancha.dto.HorarioRequest;
import com.tenisahora.modules.cancha.dto.HorarioResponse;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.entity.HorarioSemanal;
import com.tenisahora.modules.cancha.mapper.HorarioMapper;
import com.tenisahora.modules.cancha.repository.HorarioSemanalRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HorarioServiceImpl implements HorarioService {

    private final HorarioSemanalRepository horarioRepository;
    private final CanchaPorId canchaPorId;
    private final HorarioMapper horarioMapper;
    private final ReservasDelRango reservasDelRango;
    private final Clock clock;

    @Override
    @Transactional
    public List<HorarioResponse> registrar(Long canchaId, HorarioRequest request, Long actorId) {
        Cancha cancha = canchaPorId.activa(canchaId);
        FranjaHoraria franja = FranjaHoraria.validada(request.desde(), request.hasta(), CodigoError.HORARIO_INVALIDO);
        List<DayOfWeek> dias = request.dias().stream().sorted().toList();
        dias.forEach(dia -> asegurarQueNoSePisa(canchaId, dia, franja));

        List<HorarioSemanal> guardados = horarioRepository.saveAll(dias.stream().map(dia -> nuevo(cancha, dia, franja)).toList());
        log.info("{} horario(s) registrados en la cancha {} por la persona {}", guardados.size(), canchaId, actorId);
        return guardados.stream().map(horarioMapper::aRespuesta).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HorarioResponse> listar(Long canchaId) {
        canchaPorId.existente(canchaId);
        return horarioRepository.findByCanchaIdOrderByDiaSemanaAscHoraInicioAsc(canchaId).stream()
                .map(horarioMapper::aRespuesta).toList();
    }

    @Override
    @Transactional
    public void eliminar(Long canchaId, Long horarioId, Long actorId) {
        canchaPorId.existente(canchaId);
        HorarioSemanal horario = horarioRepository.findByIdAndCanchaId(horarioId, canchaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(CodigoError.HORARIO_NO_ENCONTRADO, "Horario no encontrado"));
        asegurarQueNoTieneReservas(canchaId, horario);
        horarioRepository.delete(horario);
        log.info("Horario {} de la cancha {} eliminado por la persona {}", horarioId, canchaId, actorId);
    }

    /** Un horario con reservas vigentes no se borra: primero se cancelan o se reprograman. */
    private void asegurarQueNoTieneReservas(Long canchaId, HorarioSemanal horario) {
        FranjaHoraria franja = new FranjaHoraria(horario.getHoraInicio(), horario.getHoraFin());
        List<Long> reservas = reservasDelRango.idsVigentesEnElDia(canchaId, LocalDate.now(clock), horario.getDiaSemana(), franja);
        if (!reservas.isEmpty()) {
            String ids = reservas.stream().map(String::valueOf).collect(Collectors.joining(", "));
            throw new ConflictoException(CodigoError.HORARIO_CON_RESERVAS, "Hay " + reservas.size() + " reserva(s) vigente(s) en ese horario (ids: "
                    + ids + "). Cancelalas o reprogramalas antes de eliminarlo");
        }
    }

    private void asegurarQueNoSePisa(Long canchaId, DayOfWeek dia, FranjaHoraria franja) {
        if (horarioRepository.haySolapamiento(canchaId, dia, franja.desde(), franja.hasta())) {
            throw new ConflictoException(CodigoError.HORARIO_SUPERPUESTO,
                    "El horario se superpone con uno ya registrado el día " + dia);
        }
    }

    private HorarioSemanal nuevo(Cancha cancha, DayOfWeek dia, FranjaHoraria franja) {
        HorarioSemanal horario = new HorarioSemanal();
        horario.setCancha(cancha);
        horario.setDiaSemana(dia);
        horario.setHoraInicio(franja.desde());
        horario.setHoraFin(franja.hasta());
        return horario;
    }
}
