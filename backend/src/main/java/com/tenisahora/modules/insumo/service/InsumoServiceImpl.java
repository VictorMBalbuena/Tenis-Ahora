package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.dto.EstadoStock;
import com.tenisahora.modules.insumo.dto.InsumoRequest;
import com.tenisahora.modules.insumo.dto.InsumoResponse;
import com.tenisahora.modules.insumo.dto.InsumoUpdateRequest;
import com.tenisahora.modules.insumo.dto.ResumenInsumosResponse;
import com.tenisahora.modules.insumo.entity.CategoriaInsumo;
import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.entity.TipoMovimiento;
import com.tenisahora.modules.insumo.mapper.InsumoMapper;
import com.tenisahora.modules.insumo.repository.InsumoRepository;
import com.tenisahora.modules.insumo.repository.InsumoSpecs;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.util.Textos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsumoServiceImpl implements InsumoService {

    private final InsumoRepository insumoRepository;
    private final InsumoPorId insumoPorId;
    private final CategoriaInsumoPorId categoriaInsumoPorId;
    private final InsumoMapper insumoMapper;
    private final RegistroDeMovimientos movimientos;
    private final UsoDeInsumos usoDeInsumos;
    private final Clock clock;

    // ---------- Alta (CU10) ----------

    @Override
    @Transactional
    public InsumoResponse registrar(InsumoRequest request, Long actorId) {
        asegurarNombreLibre(request.nombre(), null);
        CategoriaInsumo categoria = categoriaInsumoPorId.buscar(request.categoriaId());
        Insumo guardado = insumoRepository.saveAndFlush(insumoMapper.aEntidad(request, categoria));
        if (guardado.getCantidadDisponible() > 0) {
            movimientos.registrar(guardado, TipoMovimiento.REPOSICION, guardado.getCantidadDisponible(),
                    guardado.getCantidadDisponible(), "Stock inicial", actorId);
        }
        log.info("Insumo {} registrado por la persona {}", guardado.getId(), actorId);
        return insumoMapper.aRespuesta(guardado, 0);
    }

    // ---------- Consulta y listado (CU11, CU102) ----------

    @Override
    @Transactional(readOnly = true)
    public InsumoResponse consultar(Long id) {
        Insumo insumo = insumoPorId.activo(id);
        return insumoMapper.aRespuesta(insumo, usoDeInsumos.enUso(Set.of(id)).getOrDefault(id, 0));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InsumoResponse> buscar(String busqueda, Long categoriaId, EstadoStock estado, int page, int size) {
        Page<Insumo> insumos = insumoRepository.findAll(Specification.allOf(filtros(busqueda, categoriaId, estado)),
                PageRequest.of(page, size, Sort.by("nombre")));
        Map<Long, Integer> enUso = usoDeInsumos.enUso(insumos.stream().map(Insumo::getId).collect(Collectors.toSet()));
        return PageResponse.de(insumos, insumo -> insumoMapper.aRespuesta(insumo, enUso.getOrDefault(insumo.getId(), 0)));
    }

    // ---------- Resumen del listado (tira de KPIs) ----------

    /** Sobre todos los insumos activos del club, no solo los de una página. */
    @Override
    @Transactional(readOnly = true)
    public ResumenInsumosResponse resumen() {
        List<Insumo> activos = insumoRepository.findAll(InsumoSpecs.activo());
        Map<Long, Integer> enUso = usoDeInsumos.enUso(activos.stream().map(Insumo::getId).collect(Collectors.toSet()));

        long enDeposito = activos.stream().mapToLong(Insumo::getCantidadDisponible).sum();
        long asignadas = enUso.values().stream().mapToLong(Integer::intValue).sum();
        long alertas = activos.stream().filter(i -> insumoMapper.estadoDe(i) != EstadoStock.DISPONIBLE).count();

        return new ResumenInsumosResponse(activos.size(), enDeposito, asignadas, alertas);
    }

    // ---------- Modificación (CU12) ----------

    @Override
    @Transactional
    public InsumoResponse modificar(Long id, InsumoUpdateRequest request, Long actorId) {
        Insumo insumo = insumoPorId.activoParaModificar(id);
        if (request.nombre() != null) {
            asegurarNombreLibre(request.nombre(), id);
        }
        CategoriaInsumo categoria = request.categoriaId() != null ? categoriaInsumoPorId.buscar(request.categoriaId()) : null;
        insumoMapper.aplicar(request, insumo, categoria);
        ajustarStock(insumo, request.cantidadDisponible(), actorId);
        insumoRepository.saveAndFlush(insumo);
        log.info("Insumo {} modificado por la persona {}", id, actorId);
        return insumoMapper.aRespuesta(insumo, usoDeInsumos.enUso(Set.of(id)).getOrDefault(id, 0));
    }

    // ---------- Baja (CU101) ----------

    @Override
    @Transactional
    public void eliminar(Long id, Long actorId) {
        Insumo insumo = insumoPorId.activo(id);
        List<Long> reservas = usoDeInsumos.reservasVigentesCon(id, LocalDate.now(clock));
        if (!reservas.isEmpty()) {
            String ids = reservas.stream().map(String::valueOf).collect(Collectors.joining(", "));
            throw new ConflictoException(CodigoError.INSUMO_CON_RESERVAS, "Hay " + reservas.size()
                    + " reserva(s) vigente(s) con este insumo (ids: " + ids + "). Cancelalas o reprogramalas antes de darlo de baja");
        }
        insumo.setActivo(false);
        insumoRepository.save(insumo);
        log.info("Insumo {} dado de baja por la persona {}", id, actorId);
    }

    // ---------- Auxiliares ----------

    private List<Specification<Insumo>> filtros(String busqueda, Long categoriaId, EstadoStock estado) {
        List<Specification<Insumo>> filtros = new ArrayList<>();
        filtros.add(InsumoSpecs.activo());
        if (categoriaId != null) {
            filtros.add(InsumoSpecs.deCategoria(categoriaId));
        }
        if (busqueda != null && !busqueda.isBlank()) {
            filtros.add(InsumoSpecs.nombreContiene(Textos.patronContiene(busqueda.strip())));
        }
        if (estado != null) {
            filtros.add(InsumoSpecs.enEstado(estado));
        }
        return filtros;
    }

    /** El nombre no puede repetirse (sin distinguir mayúsculas), ni siquiera con un insumo dado de baja. */
    private void asegurarNombreLibre(String nombre, Long idPropio) {
        boolean repetido = idPropio == null
                ? insumoRepository.existsByNombreIgnoreCase(nombre)
                : insumoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, idPropio);
        if (repetido) {
            throw new ConflictoException(CodigoError.INSUMO_NOMBRE_DUPLICADO, "Ya existe un insumo con ese nombre");
        }
    }

    /** Corrige la cantidad (rotura, pérdida): se asienta como ajuste con la diferencia. Si no cambia, no hace nada. */
    private void ajustarStock(Insumo insumo, Integer nueva, Long actorId) {
        if (nueva == null || nueva.equals(insumo.getCantidadDisponible())) {
            return;
        }
        int variacion = nueva - insumo.getCantidadDisponible();
        insumo.setCantidadDisponible(nueva);
        movimientos.registrar(insumo, TipoMovimiento.AJUSTE, variacion, nueva, "Ajuste manual", actorId);
    }
}
