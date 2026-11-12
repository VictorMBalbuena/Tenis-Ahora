package com.tenisahora.modules.permiso.service;

import com.tenisahora.modules.permiso.dto.MatrizDePermisos;
import com.tenisahora.modules.permiso.entity.RolDelSistema;
import com.tenisahora.modules.permiso.model.AccionSobreModulo;
import com.tenisahora.modules.permiso.model.ModuloDelSistema;
import com.tenisahora.modules.permiso.repository.RolDelSistemaRepository;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.model.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Configurar la matriz de permisos de un rol (CU75).
 *
 * El caso que motivó estos tests: al rol base se le podían quitar permisos pero no agregarle ninguno. El techo
 * se calculaba buscando el propio rol base, así que al editarlo se lo comparaba contra sí mismo y todo lo que se
 * sumara quedaba afuera, en silencio y con respuesta 200.
 */
@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolDelSistemaRepository repositorio;
    @Mock
    private DirectorioDeCuentas directorio;

    @InjectMocks
    private RolService servicio;

    private final Actor admin = new Actor(1L, Rol.ADMINISTRADOR);

    private RolDelSistema socioBase;

    @BeforeEach
    void setUp() {
        socioBase = rol(1L, "Socio", true, Rol.SOCIO, PermisosPorDefecto.de(Rol.SOCIO));
        lenient().when(repositorio.saveAndFlush(any(RolDelSistema.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(directorio.activasCon(any())).thenReturn(0L);
        lenient().when(directorio.cuantasUsan(any())).thenReturn(0L);
    }

    private static RolDelSistema rol(Long id, String nombre, boolean base, Rol rolBase, Set<String> permisos) {
        RolDelSistema r = new RolDelSistema();
        r.setId(id);
        r.setNombre(nombre);
        r.setBase(base);
        r.setRolBase(rolBase);
        r.reemplazarPermisos(permisos);
        return r;
    }

    /** La matriz que viaja desde la pantalla, escrita corta: "RESERVAS", "VER", "CREAR". */
    private static MatrizDePermisos matriz(Map<String, List<String>> modulos) {
        Map<ModuloDelSistema, Set<AccionSobreModulo>> convertidos = new LinkedHashMap<>();
        modulos.forEach((modulo, acciones) -> convertidos.put(
                ModuloDelSistema.valueOf(modulo),
                acciones.stream().map(AccionSobreModulo::valueOf)
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new))));
        return new MatrizDePermisos(convertidos, Set.of());
    }

    @Nested
    @DisplayName("Un rol base define el techo, no lo sufre")
    class RolesBase {

        @Test
        @DisplayName("Se le puede agregar un permiso que no tenía")
        void seLePuedeAgregarUnPermiso() {
            when(repositorio.findById(1L)).thenReturn(Optional.of(socioBase));
            assertThat(socioBase.permisosCopiados()).noneMatch(p -> p.startsWith("USUARIOS:"));

            servicio.configurarPermisos(1L, matriz(Map.of(
                    "RESERVAS", List.of("VER", "CREAR"),
                    "USUARIOS", List.of("VER"))), admin);

            assertThat(socioBase.permisosCopiados()).contains("USUARIOS:VER");
        }

        @Test
        @DisplayName("Se le puede quitar un permiso que tenía")
        void seLePuedeQuitarUnPermiso() {
            when(repositorio.findById(1L)).thenReturn(Optional.of(socioBase));
            assertThat(socioBase.permisosCopiados()).contains("RESERVAS:CREAR");

            servicio.configurarPermisos(1L, matriz(Map.of("RESERVAS", List.of("VER"))), admin);

            assertThat(socioBase.permisosCopiados()).contains("RESERVAS:VER");
            assertThat(socioBase.permisosCopiados()).doesNotContain("RESERVAS:CREAR");
        }

        @Test
        @DisplayName("Quitarle permisos no le baja el techo: después puede recuperarlos")
        void quitarNoBajaElTecho() {
            when(repositorio.findById(1L)).thenReturn(Optional.of(socioBase));

            servicio.configurarPermisos(1L, matriz(Map.of("RESERVAS", List.of("VER"))), admin);
            servicio.configurarPermisos(1L, matriz(Map.of("RESERVAS", List.of("VER", "CREAR"))), admin);

            assertThat(socioBase.permisosCopiados()).contains("RESERVAS:VER", "RESERVAS:CREAR");
        }

        @Test
        @DisplayName("Una matriz vacía le deja el rol sin permisos")
        void matrizVaciaDejaSinPermisos() {
            when(repositorio.findById(1L)).thenReturn(Optional.of(socioBase));

            servicio.configurarPermisos(1L, matriz(Map.of()), admin);

            assertThat(socioBase.permisosCopiados()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Un rol creado no puede superar a su rol base")
    class RolesCreados {

        private RolDelSistema recepcion;

        @BeforeEach
        void crearRolPersonalizado() {
            recepcion = rol(9L, "Recepción", false, Rol.SOCIO, Set.of("RESERVAS:VER"));
            lenient().when(repositorio.findById(9L)).thenReturn(Optional.of(recepcion));
            lenient().when(repositorio.findByRolBaseAndBaseTrue(Rol.SOCIO)).thenReturn(Optional.of(socioBase));
        }

        @Test
        @DisplayName("Lo que el rol base sí tiene, se le concede")
        void concedeLoQueElBaseTiene() {
            servicio.configurarPermisos(9L, matriz(Map.of("RESERVAS", List.of("VER", "CREAR"))), admin);

            assertThat(recepcion.permisosCopiados()).contains("RESERVAS:VER", "RESERVAS:CREAR");
        }

        @Test
        @DisplayName("Lo que el rol base no tiene, se descarta")
        void descartaLoQueElBaseNoTiene() {
            servicio.configurarPermisos(9L, matriz(Map.of(
                    "RESERVAS", List.of("VER"),
                    "USUARIOS", List.of("VER", "ELIMINAR"))), admin);

            assertThat(recepcion.permisosCopiados()).contains("RESERVAS:VER");
            assertThat(recepcion.permisosCopiados()).noneMatch(p -> p.startsWith("USUARIOS:"));
        }

        @Test
        @DisplayName("Si al rol base le agrandan el techo, el creado ya puede alcanzarlo")
        void elTechoLoDaElRolBaseVigente() {
            socioBase.reemplazarPermisos(Set.of("RESERVAS:VER", "USUARIOS:VER"));

            servicio.configurarPermisos(9L, matriz(Map.of("USUARIOS", List.of("VER"))), admin);

            assertThat(recepcion.permisosCopiados()).contains("USUARIOS:VER");
        }
    }

    @Nested
    @DisplayName("El administrador no tiene techo")
    class Administrador {

        @Test
        @DisplayName("Un rol creado sobre ADMINISTRADOR recibe todo lo que se le pida")
        void rolSobreAdministradorNoSeLimita() {
            RolDelSistema asistente = rol(7L, "Asistente", false, Rol.ADMINISTRADOR, Set.of());
            when(repositorio.findById(7L)).thenReturn(Optional.of(asistente));

            servicio.configurarPermisos(7L, matriz(Map.of("PERMISOS", List.of("VER", "EDITAR"))), admin);

            assertThat(asistente.permisosCopiados()).contains("PERMISOS:VER", "PERMISOS:EDITAR");
        }
    }
}
