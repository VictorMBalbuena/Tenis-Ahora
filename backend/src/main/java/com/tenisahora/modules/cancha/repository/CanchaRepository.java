package com.tenisahora.modules.cancha.repository;

import com.tenisahora.modules.cancha.entity.Cancha;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CanchaRepository extends JpaRepository<Cancha, Long>, JpaSpecificationExecutor<Cancha> {

    boolean existsByIdentificador(String identificador);

    Optional<Cancha> findByIdentificador(String identificador);

    List<Cancha> findByActivaTrueOrderByIdentificador();

    @Query("select c from Cancha c left join c.tipoCancha tc where c.activa = true and " +
           "(lower(tc.superficie) like lower(concat('%', :superficie, '%')) or " +
           "lower(c.tipoSuperficie) like lower(concat('%', :superficie, '%'))) order by c.identificador")
    List<Cancha> buscarActivasPorSuperficie(@Param("superficie") String superficie);

    /** Para reservar: bloquea la fila de la cancha hasta terminar la transacción, así dos reservas del mismo turno se atienden de a una. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cancha c where c.id = :id")
    Optional<Cancha> buscarParaReservar(Long id);

    boolean existsByIdentificadorIgnoreCase(String identificador);

    boolean existsByIdentificadorIgnoreCaseAndIdNot(String identificador, Long id);

    long countByTipoCanchaIdAndActivaTrue(Long tipoCanchaId);
}
