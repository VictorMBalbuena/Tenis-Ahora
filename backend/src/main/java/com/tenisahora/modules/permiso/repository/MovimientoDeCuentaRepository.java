package com.tenisahora.modules.permiso.repository;

import com.tenisahora.modules.permiso.entity.MovimientoDeCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovimientoDeCuentaRepository extends JpaRepository<MovimientoDeCuenta, Long> {

    /**
     * El historial de una cuenta. Se busca también por el id anterior porque un cambio de rol le cambia el id a
     * la persona, y el historial tiene que seguir siendo uno solo.
     */
    @Query("select m from MovimientoDeCuenta m where m.personaId = :id or m.personaIdAnterior = :id "
            + "order by m.fecha desc")
    List<MovimientoDeCuenta> historialDe(@Param("id") Long id);

    /**
     * Después de un cambio de rol, los movimientos anteriores pasan a apuntar al id nuevo. Así la persona se lleva
     * su historial, en vez de que quede partido en dos al mudarse de tabla.
     */
    @Modifying
    @Query("update MovimientoDeCuenta m set m.personaId = :idNuevo where m.personaId = :idAnterior")
    void trasladarHistorial(@Param("idAnterior") Long idAnterior, @Param("idNuevo") Long idNuevo);
}
