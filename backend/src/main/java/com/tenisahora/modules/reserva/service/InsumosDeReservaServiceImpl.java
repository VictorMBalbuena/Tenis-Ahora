package com.tenisahora.modules.reserva.service;

import com.tenisahora.shared.dto.Actor;
import com.tenisahora.modules.reserva.dto.AsignacionDeInsumosRequest;
import com.tenisahora.modules.reserva.dto.InsumoDisponibleResponse;
import com.tenisahora.modules.reserva.dto.ReservaResponse;
import com.tenisahora.modules.reserva.entity.Reserva;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsumosDeReservaServiceImpl implements InsumosDeReservaService {

    private final AccesoAReservas acceso;
    private final AsignacionDeInsumos asignacion;
    private final DisponibilidadDeInsumos disponibilidad;
    private final ArmadoDeRespuestas respuestas;
    private final ReglasDeReserva reglas;

    @Override
    @Transactional
    public ReservaResponse asignar(Long reservaId, AsignacionDeInsumosRequest request, Actor actor) {
        Reserva reserva = acceso.paraGestionar(reservaId, actor, true);
        if (!reserva.getEstado().puedeModificarse()) {
            throw new ReglaDeNegocioException(CodigoError.RESERVA_NO_MODIFICABLE,
                    "La reserva está " + reserva.getEstado().etiqueta() + ": ya no se le pueden cambiar los insumos");
        }
        asignacion.asignar(reserva, request.insumos());
        log.info("Insumos de la reserva {} asignados por la persona {}", reservaId, actor.id());
        return respuestas.deReserva(reserva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsumoDisponibleResponse> disponibles(LocalDate fecha, LocalTime horaInicio, int duracionHoras) {
        return disponibilidad.paraElTurno(fecha, reglas.validarTurno(fecha, horaInicio, duracionHoras));
    }
}
