package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.service.ActividadesDelRango;
import com.tenisahora.modules.cancha.service.AgendaDeCancha;
import com.tenisahora.modules.cancha.service.FranjaHoraria;
import com.tenisahora.modules.reserva.entity.EstadoReserva;
import com.tenisahora.modules.reserva.repository.ReservaRepository;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** Comprueba que el turno se pueda reservar: cancha activa, dentro de sus horarios, sin bloqueo (RD05) y sin otra reserva encima. */
@Component
@RequiredArgsConstructor
public class ValidadorDeAgenda {

    private static final List<EstadoReserva> QUE_OCUPAN = EstadoReserva.queRetienenHorario();
    private static final Long NINGUNA = 0L;

    private final AgendaDeCancha agenda;
    private final ReservaRepository reservaRepository;
    private final ActividadesDelRango actividadesDelRango;

    /** excluirReservaId es la reserva que se está modificando (no se choca con sí misma); null si es una reserva nueva. */
    public void asegurarTurnoLibre(Cancha cancha, LocalDate fecha, FranjaHoraria franja, Long excluirReservaId) {
        if (!Boolean.TRUE.equals(cancha.getActiva())) {
            throw new ReglaDeNegocioException(CodigoError.CANCHA_INACTIVA, "La cancha está deshabilitada");
        }
        if (!agenda.dentroDeHorario(cancha.getId(), fecha, franja)) {
            throw new ReglaDeNegocioException(CodigoError.RESERVA_FUERA_DE_HORARIO, "La cancha no atiende en ese día y horario");
        }
        if (agenda.bloqueada(cancha.getId(), fecha, franja)) {
            throw new ConflictoException(CodigoError.RESERVA_HORARIO_BLOQUEADO, "La cancha está en mantenimiento en ese horario");
        }
        Long excluir = excluirReservaId == null ? NINGUNA : excluirReservaId;
        if (reservaRepository.haySuperposicion(cancha.getId(), QUE_OCUPAN, fecha, franja.desde(), franja.hasta(), excluir)) {
            throw new ConflictoException(CodigoError.RESERVA_HORARIO_OCUPADO, "Ese horario ya está reservado");
        }
        if (!actividadesDelRango.idsVigentesEnElDia(cancha.getId(), fecha, fecha.getDayOfWeek(), franja).isEmpty()) {
            throw new ConflictoException(CodigoError.RESERVA_HORARIO_OCUPADO, "Ese horario lo ocupa una clase o entrenamiento");
        }
    }
}
