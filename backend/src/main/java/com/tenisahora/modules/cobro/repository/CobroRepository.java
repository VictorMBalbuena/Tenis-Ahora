package com.tenisahora.modules.cobro.repository;

import com.tenisahora.modules.cobro.entity.Cobro;
import com.tenisahora.modules.cobro.entity.EstadoCobro;
import com.tenisahora.modules.cobro.entity.MedioPago;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CobroRepository extends JpaRepository<Cobro, Long>, JpaSpecificationExecutor<Cobro> {

    /** Para acreditar o cambiar el estado: bloquea la fila hasta terminar, así dos avisos de la misma orden se atienden de a uno. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cobro c where c.id = :id")
    Optional<Cobro> buscarParaModificar(Long id);

    /** El aviso de Mercado Pago trae el pago, no el cobro: se llega al cobro por el identificador externo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cobro c where c.idPagoExterno = :idPagoExterno")
    Optional<Cobro> buscarPorPagoExternoParaModificar(String idPagoExterno);

    Optional<Cobro> findByIdPagoExterno(String idPagoExterno);

    /** Para traducir el id de pago que trae el webhook (vía la referencia externa) al cobro correspondiente. */
    Optional<Cobro> findByCodigo(String codigo);

    boolean existsByIdPagoExterno(String idPagoExterno);

    boolean existsByReservaIdAndEstadoIn(Long reservaId, Collection<EstadoCobro> estados);

    List<Cobro> findByReservaIdAndEstado(Long reservaId, EstadoCobro estado);

    Optional<Cobro> findFirstByReservaIdAndEstadoAndMedioPago(Long reservaId, EstadoCobro estado, MedioPago medioPago);

    /** Los cobros de un socio: las reservas donde es titular (el acompañante no cuenta), sus inscripciones y sus torneos. */
    @Query("select c from Cobro c left join c.inscripcionActividad actividad left join c.inscripcionTorneo torneo "
            + "where actividad.socio.id = :socioId or torneo.jugador1.id = :socioId or torneo.pareja.id = :socioId "
            + "or exists (select 1 from ReservaJugador jugador where jugador.reserva = c.reserva and jugador.socio.id = :socioId and jugador.esTitular = true)")
    Page<Cobro> delSocio(Long socioId, Pageable pageable);

    /** Los cobros de la inscripción actual: al reinscribirse, los de la baja anterior quedan afuera. */
    @Query("select c from Cobro c where c.inscripcionActividad.id = :inscripcionId and c.estado in :estados and c.fecha >= :desde")
    List<Cobro> deInscripcionActividad(Long inscripcionId, Collection<EstadoCobro> estados, LocalDateTime desde);

    @Query("select c from Cobro c where c.inscripcionTorneo.id = :inscripcionId and c.estado in :estados")
    List<Cobro> deInscripcionTorneo(Long inscripcionId, Collection<EstadoCobro> estados);

    /** Cobros con QR que ya venció y nadie pagó, para pasarlos a VENCIDO y liberar el intento. */
    @Query("select c from Cobro c where c.estado in :estados and c.fechaExpiracionQr is not null "
            + "and c.fechaExpiracionQr < :limite order by c.id")
    List<Cobro> conQrVencido(Collection<EstadoCobro> estados, LocalDateTime limite);

    /**
     * Cobros que siguen esperando el aviso aunque el QR ya venció. Hacen falta para la conciliación: como el webhook
     * responde 200 antes de procesar, Mercado Pago no reintenta y un aviso perdido hay que ir a buscarlo.
     */
    @Query("select c from Cobro c where c.estado in :estados and c.idPagoExterno is not null "
            + "and c.fechaExpiracionQr < :limite order by c.id")
    List<Cobro> pendientesDeConciliar(Collection<EstadoCobro> estados, LocalDateTime limite);
}
