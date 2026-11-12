package com.tenisahora.modules.reserva.service;

import com.tenisahora.modules.cancha.dto.FranjaResponse;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.cancha.service.DisponibilidadService;
import com.tenisahora.modules.reserva.dto.CanchaDisponibleResponse;
import com.tenisahora.modules.reserva.dto.DisponibilidadReservaResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadParaReservarImpl implements DisponibilidadParaReservar {

    private final CanchaRepository canchaRepository;
    private final DisponibilidadService disponibilidadService;
    private final ReglasDeReserva reglas;
    private final ReservasProperties propiedades;

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadReservaResponse consultar(LocalDate fecha, LocalTime horaDesde, LocalTime horaHasta, String superficie) {
        validarRangoHorario(horaDesde, horaHasta);
        List<Cancha> canchas = (superficie == null || superficie.isBlank())
                ? canchaRepository.findByActivaTrueOrderByIdentificador()
                : canchaRepository.buscarActivasPorSuperficie(superficie.strip());
        List<CanchaDisponibleResponse> grillas = canchas.stream().map(cancha -> grillaDe(cancha, fecha, horaDesde, horaHasta)).toList();
        boolean reservable = reglas.esReservable(fecha);
        return new DisponibilidadReservaResponse(fecha, reservable, reservable ? null : aviso(), grillas);
    }

    private CanchaDisponibleResponse grillaDe(Cancha cancha, LocalDate fecha, LocalTime horaDesde, LocalTime horaHasta) {
        List<FranjaResponse> franjas = disponibilidadService.consultar(cancha.getId(), fecha, fecha).franjas().stream()
                .filter(franja -> dentroDelRango(franja, horaDesde, horaHasta)).toList();
        return new CanchaDisponibleResponse(cancha.getId(), cancha.getIdentificador(), cancha.getSuperficie(),
                cancha.getPrecioPorHora(), franjas);
    }

    private boolean dentroDelRango(FranjaResponse franja, LocalTime horaDesde, LocalTime horaHasta) {
        return (horaDesde == null || !franja.desde().isBefore(horaDesde)) && (horaHasta == null || !franja.hasta().isAfter(horaHasta));
    }

    private void validarRangoHorario(LocalTime horaDesde, LocalTime horaHasta) {
        boolean soloUno = (horaDesde == null) != (horaHasta == null);
        if (soloUno || (horaDesde != null && !horaDesde.isBefore(horaHasta))) {
            throw new SolicitudInvalidaException(CodigoError.RANGO_DE_CONSULTA_INVALIDO,
                    "El rango horario necesita las dos horas, con el inicio antes del fin");
        }
    }

    private String aviso() {
        return "Solo se puede reservar de hoy hasta con " + propiedades.antelacionMaximaDias()
                + " días de antelación: esta fecha es solo de consulta";
    }
}
