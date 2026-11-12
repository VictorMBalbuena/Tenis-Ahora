package com.tenisahora.modules.insumo.service;

import com.tenisahora.modules.insumo.dto.CategoriaInsumoRequest;
import com.tenisahora.modules.insumo.dto.CategoriaInsumoResponse;
import com.tenisahora.modules.insumo.entity.CategoriaInsumo;
import com.tenisahora.modules.insumo.mapper.InsumoMapper;
import com.tenisahora.modules.insumo.repository.CategoriaInsumoRepository;
import com.tenisahora.modules.insumo.repository.InsumoRepository;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaInsumoServiceImplTest {

    @Mock
    private CategoriaInsumoRepository categoriaInsumoRepository;
    @Mock
    private InsumoRepository insumoRepository;

    private CategoriaInsumoServiceImpl servicio;

    private static CategoriaInsumo categoria(Long id, String nombre) {
        CategoriaInsumo categoria = new CategoriaInsumo(nombre);
        categoria.setId(id);
        return categoria;
    }

    @BeforeEach
    void setUp() {
        servicio = new CategoriaInsumoServiceImpl(categoriaInsumoRepository, insumoRepository, new InsumoMapper());
    }

    @Test
    void listar_devuelveLasCategoriasOrdenadasPorNombre() {
        when(categoriaInsumoRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(categoria(2L, "Pelota"), categoria(1L, "Raqueta")));

        List<CategoriaInsumoResponse> respuesta = servicio.listar();

        assertThat(respuesta).extracting(CategoriaInsumoResponse::nombre).containsExactly("Pelota", "Raqueta");
    }

    @Test
    void registrar_conNombreLibre_laGuarda() {
        when(categoriaInsumoRepository.existsByNombreIgnoreCase("Grip / overgrip")).thenReturn(false);
        when(categoriaInsumoRepository.save(org.mockito.ArgumentMatchers.any(CategoriaInsumo.class)))
                .thenAnswer(inv -> {
                    CategoriaInsumo guardada = inv.getArgument(0);
                    guardada.setId(4L);
                    return guardada;
                });

        CategoriaInsumoResponse respuesta = servicio.registrar(new CategoriaInsumoRequest("Grip / overgrip"));

        assertThat(respuesta.id()).isEqualTo(4L);
        assertThat(respuesta.nombre()).isEqualTo("Grip / overgrip");
    }

    @Test
    void registrar_conNombreRepetido_409YNoGuarda() {
        when(categoriaInsumoRepository.existsByNombreIgnoreCase("Raqueta")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(new CategoriaInsumoRequest("Raqueta")))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("CATEGORIA_INSUMO_NOMBRE_DUPLICADO");
        verify(categoriaInsumoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void modificar_leCambiaElNombre() {
        CategoriaInsumo existente = categoria(1L, "Raqueta");
        when(categoriaInsumoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaInsumoRepository.existsByNombreIgnoreCaseAndIdNot("Raquetas", 1L)).thenReturn(false);
        when(categoriaInsumoRepository.save(existente)).thenReturn(existente);

        CategoriaInsumoResponse respuesta = servicio.modificar(1L, new CategoriaInsumoRequest("Raquetas"));

        assertThat(respuesta.nombre()).isEqualTo("Raquetas");
        assertThat(existente.getNombre()).isEqualTo("Raquetas");
    }

    @Test
    void modificar_conNombreDeOtraCategoria_409YNoGuarda() {
        CategoriaInsumo existente = categoria(1L, "Raqueta");
        when(categoriaInsumoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaInsumoRepository.existsByNombreIgnoreCaseAndIdNot("Pelota", 1L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.modificar(1L, new CategoriaInsumoRequest("Pelota")))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo("CATEGORIA_INSUMO_NOMBRE_DUPLICADO");
        verify(categoriaInsumoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void modificar_inexistente_404() {
        when(categoriaInsumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.modificar(99L, new CategoriaInsumoRequest("Lo que sea")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting("codigo").isEqualTo("CATEGORIA_INSUMO_NO_ENCONTRADA");
    }

    @Test
    void eliminar_sinInsumosAsignados_laBorra() {
        CategoriaInsumo existente = categoria(1L, "Raqueta");
        when(categoriaInsumoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(insumoRepository.countByCategoriaId(1L)).thenReturn(0L);

        servicio.eliminar(1L);

        verify(categoriaInsumoRepository).delete(existente);
    }

    @Test
    void eliminar_conInsumosAsignados_409YNoBorra() {
        CategoriaInsumo existente = categoria(1L, "Raqueta");
        when(categoriaInsumoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(insumoRepository.countByCategoriaId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> servicio.eliminar(1L))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("3 insumo(s)")
                .extracting("codigo").isEqualTo("CATEGORIA_INSUMO_CON_INSUMOS");
        verify(categoriaInsumoRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void eliminar_inexistente_404() {
        when(categoriaInsumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .extracting("codigo").isEqualTo("CATEGORIA_INSUMO_NO_ENCONTRADA");
    }
}
