package com.tenisahora.modules.insumo.repository;

import com.tenisahora.modules.insumo.entity.Insumo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InsumoRepository extends JpaRepository<Insumo, Long>, JpaSpecificationExecutor<Insumo> {

    /** El nombre es único entre todos los insumos, también los dados de baja. */
    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Insumo> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    Optional<Insumo> findByIdAndActivoTrue(Long id);

    /** Para bloquear el borrado de una categoría: cuenta también los insumos dados de baja, que igual la referencian. */
    long countByCategoriaId(Long categoriaId);

    /** Para modificar: bloquea la fila hasta terminar, así dos ajustes a la vez no se pisan. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Insumo i where i.id = :id and i.activo = true")
    Optional<Insumo> buscarActivoParaModificar(Long id);

    /** Bloquea las filas de los insumos activos pedidos, siempre en orden de id, así dos pedidos que piden los mismos no se traban entre sí. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Insumo i where i.id in :ids and i.activo = true order by i.id")
    List<Insumo> bloquearActivos(Collection<Long> ids);

    /** Resta en un solo paso y solo si alcanza: el stock nunca queda negativo, ni con pedidos simultáneos. Devuelve 1 si lo hizo, 0 si no. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Insumo i set i.cantidadDisponible = i.cantidadDisponible - :cantidad "
            + "where i.id = :id and i.activo = true and i.cantidadDisponible >= :cantidad")
    int descontar(Long id, int cantidad);

    /** Suma en un solo paso y solo si no se pasa del máximo (margen = máximo menos la cantidad). Devuelve 1 si lo hizo, 0 si no. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Insumo i set i.cantidadDisponible = i.cantidadDisponible + :cantidad "
            + "where i.id = :id and i.activo = true and i.cantidadDisponible <= :margen")
    int reponer(Long id, int cantidad, int margen);
}
