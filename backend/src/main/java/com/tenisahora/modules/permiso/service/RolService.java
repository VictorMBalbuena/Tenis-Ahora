package com.tenisahora.modules.permiso.service;

import com.tenisahora.modules.permiso.dto.MatrizDePermisos;
import com.tenisahora.modules.permiso.dto.RolRequest;
import com.tenisahora.modules.permiso.dto.RolResponse;
import com.tenisahora.modules.permiso.entity.RolDelSistema;
import com.tenisahora.modules.permiso.repository.RolDelSistemaRepository;
import com.tenisahora.security.ConsultaDePermisos;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Los roles del sistema y sus permisos (CU75 Configurar permisos por rol, CU85 Crear rol).
 *
 * <p>También es el que responde qué puede hacer una sesión: implementa {@link ConsultaDePermisos}, que es el
 * puerto que usa el filtro de seguridad. Se guarda en memoria lo ya consultado, porque la matriz se lee en cada
 * pedido y se edita muy de vez en cuando; cualquier escritura vacía esa memoria.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RolService implements ConsultaDePermisos {

    private final RolDelSistemaRepository repositorio;
    private final DirectorioDeCuentas directorio;

    /** Clave: el nombre del rol base, o "personalizado:<id>". */
    private final Map<String, Set<String>> cache = new ConcurrentHashMap<>();

    /**
     * Los cinco roles base tienen que existir siempre. Si falta alguno (la primera vez, o porque se agregó un rol
     * al enum) se crea con los permisos por defecto, que son los que ese rol ya tenía. Los que ya están no se
     * tocan: lo que configuró el administrador manda.
     */
    @PostConstruct
    @Transactional
    public void asegurarRolesBase() {
        for (Rol rol : Rol.values()) {
            if (repositorio.findByRolBaseAndBaseTrue(rol).isEmpty()) {
                RolDelSistema nuevo = new RolDelSistema();
                nuevo.setNombre(etiquetaDe(rol));
                nuevo.setDescripcion("Rol base del sistema");
                nuevo.setBase(true);
                nuevo.setRolBase(rol);
                nuevo.reemplazarPermisos(PermisosPorDefecto.de(rol));
                repositorio.save(nuevo);
                log.info("Rol base {} creado con sus permisos por defecto", rol);
            }
        }
        cache.clear();
    }

    // ---------- CU75 y CU85 ----------

    @Transactional(readOnly = true)
    public List<RolResponse> listar() {
        return repositorio.findAllByOrderByBaseDescNombreAsc().stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public RolResponse consultar(Long id) {
        return aRespuesta(obtener(id));
    }

    /** CU85: crear un rol. El nombre no se puede repetir, como pide RF85. */
    @Transactional
    public RolResponse crear(RolRequest request, Actor actor) {
        asegurarNombreLibre(request.nombre(), null);

        RolDelSistema rol = new RolDelSistema();
        rol.setNombre(request.nombre());
        rol.setDescripcion(request.descripcion());
        rol.setBase(false);
        rol.setRolBase(request.rolBase());
        rol.setCreadoPorId(actor.id());
        rol.reemplazarPermisos(permisosLimitados(request.permisos(), request.rolBase()));

        RolDelSistema guardado = repositorio.saveAndFlush(rol);
        cache.clear();
        log.info("Rol {} ({}) creado por persona {}", guardado.getNombre(), guardado.getId(), actor.id());
        return aRespuesta(guardado);
    }

    /** CU75: cambiar la matriz de un rol. Vale tanto para los roles base como para los creados. */
    @Transactional
    public RolResponse configurarPermisos(Long id, MatrizDePermisos permisos, Actor actor) {
        RolDelSistema rol = obtener(id);
        rol.reemplazarPermisos(permisosParaGuardar(rol, permisos));

        RolDelSistema guardado = repositorio.saveAndFlush(rol);
        cache.clear();
        log.info("Permisos del rol {} ({}) configurados por persona {}", rol.getNombre(), id, actor.id());
        return aRespuesta(guardado);
    }

    /** Cambiar el nombre y la descripción de un rol creado. Los roles base conservan su identidad. */
    @Transactional
    public RolResponse renombrar(Long id, String nombre, String descripcion, Actor actor) {
        RolDelSistema rol = obtener(id);
        if (rol.esBase()) {
            throw new ReglaDeNegocioException(CodigoError.ROL_BASE_NO_SE_BORRA,
                    "Los roles base no se renombran: solo se les cambian los permisos");
        }
        asegurarNombreLibre(nombre, id);
        rol.setNombre(nombre.strip());
        rol.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.strip());

        RolDelSistema guardado = repositorio.saveAndFlush(rol);
        cache.clear();
        log.info("Rol {} renombrado por persona {}", id, actor.id());
        return aRespuesta(guardado);
    }

    @Transactional
    public void eliminar(Long id, Actor actor) {
        RolDelSistema rol = obtener(id);
        if (rol.esBase()) {
            throw new ReglaDeNegocioException(CodigoError.ROL_BASE_NO_SE_BORRA,
                    "Los cinco roles base no se pueden eliminar");
        }
        long enUso = directorio.cuantasUsan(id);
        if (enUso > 0) {
            throw new ConflictoException(CodigoError.ROL_EN_USO,
                    "El rol está asignado a " + enUso + (enUso == 1 ? " persona" : " personas")
                            + ". Cambiales el rol antes de eliminarlo");
        }
        repositorio.delete(rol);
        cache.clear();
        log.info("Rol {} eliminado por persona {}", id, actor.id());
    }

    // ---------- Lo que necesitan otros ----------

    /** El rol personalizado, verificando que se apoye en el rol base que corresponde (lo usa CU74). */
    @Transactional(readOnly = true)
    public RolDelSistema personalizadoCompatible(Long rolPersonalizadoId, Rol rolBase) {
        RolDelSistema rol = obtener(rolPersonalizadoId);
        if (rol.esBase()) {
            throw new ReglaDeNegocioException(CodigoError.ROL_PERSONALIZADO_INCOMPATIBLE,
                    "Ese es un rol base: no hace falta asignarlo aparte");
        }
        if (rol.getRolBase() != rolBase) {
            throw new ReglaDeNegocioException(CodigoError.ROL_PERSONALIZADO_INCOMPATIBLE,
                    "El rol \"" + rol.getNombre() + "\" se apoya en " + etiquetaDe(rol.getRolBase())
                            + ", así que no se puede asignar a alguien con rol " + etiquetaDe(rolBase));
        }
        return rol;
    }

    @Transactional(readOnly = true)
    public Optional<String> nombreDe(Long rolPersonalizadoId) {
        if (rolPersonalizadoId == null) {
            return Optional.empty();
        }
        return repositorio.findById(rolPersonalizadoId).map(RolDelSistema::getNombre);
    }

    /**
     * Los permisos de la sesión, que el filtro JWT consulta en cada pedido. Primero se mira si la persona tiene
     * un rol personalizado asignado; si no, valen los de su rol base.
     */
    @Override
    @Transactional(readOnly = true)
    public Set<String> permisosDe(Long personaId, String rolBase) {
        Long personalizado = rolConocido(rolBase)
                .flatMap(rol -> directorio.porId(personaId, rol))
                .map(cuenta -> cuenta.rolPersonalizadoId())
                .orElse(null);

        if (personalizado != null) {
            Long id = personalizado;
            return cache.computeIfAbsent("personalizado:" + id, clave ->
                    repositorio.findById(id)
                            .map(RolDelSistema::permisosCopiados)
                            .orElseGet(() -> permisosDeRolBase(rolBase)));
        }
        return cache.computeIfAbsent("base:" + rolBase, clave -> permisosDeRolBase(rolBase));
    }

    private Optional<Rol> rolConocido(String rol) {
        try {
            return Optional.of(Rol.valueOf(rol));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private Set<String> permisosDeRolBase(String rolBase) {
        try {
            return repositorio.findByRolBaseAndBaseTrue(Rol.valueOf(rolBase))
                    .map(RolDelSistema::permisosCopiados)
                    .orElseGet(Set::of);
        } catch (IllegalArgumentException e) {
            return Set.of();
        }
    }

    // ---------- Apoyo ----------

    /**
     * Qué permisos se le guardan a un rol al configurarlo.
     *
     * Un rol base no se limita: es el que define el techo. Limitarlo contra sí mismo dejaba quitarle permisos pero
     * nunca agregarle ninguno, y encima cada quita bajaba el techo para siempre. Los roles creados sí se limitan.
     */
    private Set<String> permisosParaGuardar(RolDelSistema rol, MatrizDePermisos permisos) {
        if (rol.esBase()) {
            return permisos == null ? Set.of() : permisos.aCodigos();
        }
        return permisosLimitados(permisos, rol.getRolBase());
    }

    /**
     * Un rol creado nunca puede dar más permisos que su rol base: si "Recepción" se apoya en EMPLEADO, no puede
     * habilitar reportes, que el empleado no tiene. Así la matriz no se convierte en una puerta de atrás para
     * darle permisos de administrador a cualquiera.
     */
    private Set<String> permisosLimitados(MatrizDePermisos pedidos, Rol rolBase) {
        Set<String> solicitados = pedidos == null ? Set.of() : pedidos.aCodigos();
        if (rolBase == Rol.ADMINISTRADOR) {
            return solicitados;
        }
        Set<String> techo = permisosDeRolBase(rolBase.name());
        return solicitados.stream().filter(techo::contains).collect(java.util.stream.Collectors.toCollection(
                java.util.LinkedHashSet::new));
    }

    private void asegurarNombreLibre(String nombre, Long exceptoId) {
        repositorio.findByNombreIgnoreCase(nombre.strip())
                .filter(otro -> !otro.getId().equals(exceptoId))
                .ifPresent(otro -> {
                    throw new ConflictoException(CodigoError.ROL_NOMBRE_DUPLICADO,
                            "Ya existe un rol con el nombre \"" + otro.getNombre() + "\"");
                });
    }

    private RolDelSistema obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException(CodigoError.ROL_NO_ENCONTRADO, "El rol no existe"));
    }

    private RolResponse aRespuesta(RolDelSistema rol) {
        long cuentas = rol.esBase() ? directorio.activasCon(rol.getRolBase()) : directorio.cuantasUsan(rol.getId());
        return new RolResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), rol.esBase(), rol.getRolBase(),
                rol.getFechaAlta(), cuentas, MatrizDePermisos.deCodigos(rol.permisosCopiados()));
    }

    static String etiquetaDe(Rol rol) {
        return switch (rol) {
            case SOCIO -> "Socio";
            case EMPLEADO -> "Empleado";
            case ADMINISTRADOR -> "Administrador";
            case PROFESOR -> "Profesor";
            case ENTRENADOR -> "Entrenador";
        };
    }
}
