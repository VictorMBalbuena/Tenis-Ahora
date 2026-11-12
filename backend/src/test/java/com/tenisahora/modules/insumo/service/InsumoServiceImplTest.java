package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.dto.EstadoStock;
import com.tenisahora.modules.insumo.dto.InsumoRequest;
import com.tenisahora.modules.insumo.dto.InsumoResponse;
import com.tenisahora.modules.insumo.dto.InsumoUpdateRequest;
import com.tenisahora.modules.insumo.entity.CategoriaInsumo;
import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.entity.TipoMovimiento;
import com.tenisahora.modules.insumo.mapper.InsumoMapper;
import com.tenisahora.modules.insumo.repository.CategoriaInsumoRepository;
import com.tenisahora.modules.insumo.repository.InsumoRepository;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsumoServiceImplTest {

    private static final long ACTOR = 2L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-19T10:00:00Z"), ZoneOffset.UTC);

    private static final CategoriaInsumo PELOTA = categoria(2L, "PELOTA");
    private static final CategoriaInsumo RED = categoria(1L, "RED");

    @Mock
    private InsumoRepository insumoRepository;
    @Mock
    private CategoriaInsumoRepository categoriaInsumoRepository;
    @Mock
    private RegistroDeMovimientos movimientos;
    @Mock
    private UsoDeInsumos usoDeInsumos;

    private InsumoServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new InsumoServiceImpl(insumoRepository, new InsumoPorId(insumoRepository),
                new CategoriaInsumoPorId(categoriaInsumoRepository), new InsumoMapper(), movimientos, usoDeInsumos, RELOJ);
    }

    private static CategoriaInsumo categoria(Long id, String nombre) {
        CategoriaInsumo categoria = new CategoriaInsumo(nombre);
        categoria.setId(id);
        return categoria;
    }

    private static Insumo insumo(Long id, String nombre, int cantidad, int minima) {
        Insumo insumo = new Insumo();
        insumo.setId(id);
        insumo.setNombre(nombre);
        insumo.setCategoria(PELOTA);
        insumo.setCantidadDisponible(cantidad);
        insumo.setCantidadMinima(minima);
        insumo.setActivo(true);
        return insumo;
    }

    // ---------- CU10 ----------

    @Test
    void registrar_guardaActivoYAsientaElStockInicial() {
        when(insumoRepository.existsByNombreIgnoreCase("Pelotas")).thenReturn(false);
        when(categoriaInsumoRepository.findById(PELOTA.getId())).thenReturn(Optional.of(PELOTA));
        when(insumoRepository.saveAndFlush(any(Insumo.class))).thenAnswer(inv -> {
            Insumo guardado = inv.getArgument(0);
            guardado.setId(9L);
            return guardado;
        });

        InsumoResponse respuesta = servicio.registrar(new InsumoRequest("  Pelotas ", PELOTA.getId(), 40, 10), ACTOR);

        assertThat(respuesta.id()).isEqualTo(9L);
        assertThat(respuesta.nombre()).isEqualTo("Pelotas");
        assertThat(respuesta.estado()).isEqualTo(EstadoStock.DISPONIBLE);
        assertThat(respuesta.enUso()).isZero();
        ArgumentCaptor<Insumo> guardado = ArgumentCaptor.forClass(Insumo.class);
        verify(insumoRepository).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getActivo()).isTrue();
        verify(movimientos).registrar(any(Insumo.class), eq(TipoMovimiento.REPOSICION), eq(40), eq(40), eq("Stock inicial"), eq(ACTOR));
    }

    @Test
    void registrar_sinStockInicial_noAsientaNingunMovimiento() {
        when(categoriaInsumoRepository.findById(RED.getId())).thenReturn(Optional.of(RED));
        when(insumoRepository.saveAndFlush(any(Insumo.class))).thenAnswer(inv -> inv.getArgument(0));

        InsumoResponse respuesta = servicio.registrar(new InsumoRequest("Redes", RED.getId(), 0, 2), ACTOR);

        assertThat(respuesta.estado()).isEqualTo(EstadoStock.SIN_STOCK);
        verify(movimientos, never()).registrar(any(), any(), anyInt(), anyInt(), any(), any());
    }

    @Test
    void registrar_conNombreRepetido_409YNoGuarda() {
        when(insumoRepository.existsByNombreIgnoreCase("Pelotas")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(new InsumoRequest("Pelotas", PELOTA.getId(), 5, 1), ACTOR))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("INSUMO_NOMBRE_DUPLICADO");
        verify(insumoRepository, never()).saveAndFlush(any());
    }

    // ---------- CU11 y CU102 ----------

    @Test
    void consultar_devuelveElInsumoConLasUnidadesEnUso() {
        when(insumoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(insumo(1L, "Raquetas", 5, 6)));
        when(usoDeInsumos.enUso(Set.of(1L))).thenReturn(Map.of(1L, 8));

        InsumoResponse respuesta = servicio.consultar(1L);

        assertThat(respuesta.enUso()).isEqualTo(8);
        assertThat(respuesta.estado()).isEqualTo(EstadoStock.STOCK_BAJO);
    }

    @Test
    void consultar_inexistenteODadoDeBaja_404() {
        when(insumoRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Insumo no encontrado")
                .extracting("codigo").isEqualTo("INSUMO_NO_ENCONTRADO");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_devuelveLaPaginaConLasUnidadesEnUsoDeCadaInsumo() {
        Page<Insumo> pagina = new PageImpl<>(List.of(insumo(1L, "Pelotas", 0, 5), insumo(2L, "Redes", 9, 5)), PageRequest.of(0, 20), 2);
        when(insumoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);
        when(usoDeInsumos.enUso(Set.of(1L, 2L))).thenReturn(Map.of(2L, 3));

        PageResponse<InsumoResponse> respuesta = servicio.buscar(" pelota ", PELOTA.getId(), EstadoStock.SIN_STOCK, 0, 20);

        assertThat(respuesta.contenido()).extracting(InsumoResponse::enUso).containsExactly(0, 3);
        assertThat(respuesta.contenido()).extracting(InsumoResponse::estado).containsExactly(EstadoStock.SIN_STOCK, EstadoStock.DISPONIBLE);
        assertThat(respuesta.totalElementos()).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_sinResultados_devuelveListaVacia_A1() {
        when(insumoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(usoDeInsumos.enUso(Set.of())).thenReturn(Map.of());

        assertThat(servicio.buscar(null, null, null, 0, 20).contenido()).isEmpty();
    }

    // ---------- CU12 ----------

    @Test
    void modificar_cambiaSoloLoQueVieneSinTocarElStock() {
        Insumo existente = insumo(1L, "Pelotas", 20, 5);
        when(insumoRepository.buscarActivoParaModificar(1L)).thenReturn(Optional.of(existente));
        when(insumoRepository.existsByNombreIgnoreCaseAndIdNot("Pelotas de torneo", 1L)).thenReturn(false);

        InsumoResponse respuesta = servicio.modificar(1L, new InsumoUpdateRequest("Pelotas de torneo", null, 12, null), ACTOR);

        assertThat(respuesta.nombre()).isEqualTo("Pelotas de torneo");
        assertThat(respuesta.cantidadMinima()).isEqualTo(12);
        assertThat(respuesta.cantidadDisponible()).isEqualTo(20);
        assertThat(respuesta.categoria().id()).isEqualTo(PELOTA.getId());
        verify(movimientos, never()).registrar(any(), any(), anyInt(), anyInt(), any(), any());
        verify(insumoRepository).saveAndFlush(existente);
    }

    @Test
    void modificar_conCantidadNueva_asientaUnAjusteConLaDiferencia() {
        Insumo existente = insumo(1L, "Pelotas", 20, 5);
        when(insumoRepository.buscarActivoParaModificar(1L)).thenReturn(Optional.of(existente));

        InsumoResponse respuesta = servicio.modificar(1L, new InsumoUpdateRequest(null, null, null, 17), ACTOR);

        assertThat(respuesta.cantidadDisponible()).isEqualTo(17);
        verify(movimientos).registrar(existente, TipoMovimiento.AJUSTE, -3, 17, "Ajuste manual", ACTOR);
    }

    @Test
    void modificar_conLaMismaCantidad_noAsientaNada() {
        when(insumoRepository.buscarActivoParaModificar(1L)).thenReturn(Optional.of(insumo(1L, "Pelotas", 20, 5)));

        servicio.modificar(1L, new InsumoUpdateRequest(null, null, null, 20), ACTOR);

        verify(movimientos, never()).registrar(any(), any(), anyInt(), anyInt(), any(), any());
    }

    @Test
    void modificar_nombreDeOtroInsumo_409YNoGuarda() {
        when(insumoRepository.buscarActivoParaModificar(1L)).thenReturn(Optional.of(insumo(1L, "Pelotas", 20, 5)));
        when(insumoRepository.existsByNombreIgnoreCaseAndIdNot("Redes", 1L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.modificar(1L, new InsumoUpdateRequest("Redes", null, null, null), ACTOR))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("INSUMO_NOMBRE_DUPLICADO");
        verify(insumoRepository, never()).saveAndFlush(any());
    }

    @Test
    void modificar_inexistente_404() {
        when(insumoRepository.buscarActivoParaModificar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.modificar(99L, new InsumoUpdateRequest(null, null, 1, null), ACTOR))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------- CU101 ----------

    @Test
    void eliminar_sinReservasVigentes_leHaceLaBajaLogica() {
        Insumo existente = insumo(1L, "Pelotas", 20, 5);
        when(insumoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(existente));
        when(usoDeInsumos.reservasVigentesCon(1L, LocalDate.of(2026, 10, 19))).thenReturn(List.of());

        servicio.eliminar(1L, ACTOR);

        assertThat(existente.getActivo()).isFalse();
        verify(insumoRepository).save(existente);
        verify(insumoRepository, never()).delete(any(Insumo.class));
    }

    @Test
    void eliminar_conReservasVigentes_409ConSusIdsYNoLeHaceLaBaja() {
        Insumo existente = insumo(1L, "Pelotas", 20, 5);
        when(insumoRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(existente));
        when(usoDeInsumos.reservasVigentesCon(1L, LocalDate.of(2026, 10, 19))).thenReturn(List.of(3L, 8L));

        assertThatThrownBy(() -> servicio.eliminar(1L, ACTOR))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("2 reserva(s)")
                .hasMessageContaining("3, 8")
                .hasMessageContaining("reprogramalas")
                .extracting("codigo").isEqualTo("INSUMO_CON_RESERVAS");
        assertThat(existente.getActivo()).isTrue();
        verify(insumoRepository, never()).save(any());
    }

    @Test
    void eliminar_inexistente_404() {
        when(insumoRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(99L, ACTOR)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------- Resumen (tira de KPIs) ----------

    @Test
    @SuppressWarnings("unchecked")
    void resumen_sumaSobreTodosLosActivosNoSoloUnaPagina() {
        Insumo disponible = insumo(1L, "Pelotas", 10, 5);
        Insumo stockBajo = insumo(2L, "Redes", 3, 5);
        when(insumoRepository.findAll(any(Specification.class))).thenReturn(List.of(disponible, stockBajo));
        when(usoDeInsumos.enUso(Set.of(1L, 2L))).thenReturn(Map.of(1L, 4));

        var respuesta = servicio.resumen();

        assertThat(respuesta.distintos()).isEqualTo(2);
        assertThat(respuesta.enDeposito()).isEqualTo(13);
        assertThat(respuesta.asignadas()).isEqualTo(4);
        assertThat(respuesta.alertas()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void resumen_sinInsumos_todoEnCero() {
        when(insumoRepository.findAll(any(Specification.class))).thenReturn(List.of());
        when(usoDeInsumos.enUso(Set.of())).thenReturn(Map.of());

        var respuesta = servicio.resumen();

        assertThat(respuesta.distintos()).isZero();
        assertThat(respuesta.enDeposito()).isZero();
        assertThat(respuesta.asignadas()).isZero();
        assertThat(respuesta.alertas()).isZero();
    }
}
