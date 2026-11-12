package com.tenisahora.shared.persona;

import com.tenisahora.infrastructure.pdf.PdfService;
import com.tenisahora.infrastructure.pdf.PdfService.CampoFicha;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.MiPerfilResponse;
import com.tenisahora.shared.dto.PageResponse;
import com.tenisahora.shared.dto.PersonaRequest;
import com.tenisahora.shared.dto.PersonaResumenResponse;
import com.tenisahora.shared.dto.PersonaUpdateRequest;
import com.tenisahora.shared.entity.Persona;
import com.tenisahora.shared.event.PersonaEliminadaEvent;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.model.Sexo;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.exception.SolicitudInvalidaException;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.util.Textos;
import com.tenisahora.shared.validation.Dni;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Alta, consulta, modificación, baja e impresión de una persona, escritas una sola vez. Es la clase abstracta Persona
 * del diagrama de clases hecha comportamiento: cada rol (socio, empleado, administrador) la extiende con su propia
 * entidad, su propio repositorio y lo que lo distingue, sin que un rol conozca a otro.
 *
 * @param <E> la entidad del rol
 * @param <R> la respuesta que devuelve el rol
 */
@Slf4j
public abstract class GestionDePersonas<E extends Persona, R> {

    private static final int MAX_TAMANIO_PAGINA = 50;
    private static final int MAX_BYTES_CONTRASENIA = 72; // BCrypt ignora lo que pasa de 72 bytes
    private static final int MIN_LARGO_BUSQUEDA_POR_NOMBRE = 2;
    private static final Pattern FORMATO_DNI = Pattern.compile(Dni.FORMATO);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Datos comunes del alta, vengan del autorregistro o del alta hecha por un empleado. */
    public record DatosAlta(String nombre, String apellido, String dni, LocalDate fechaNacimiento,
                            String calle, String numeroCalle, String codigoArea, String numeroTelefono,
                            String email, String password) {

        static DatosAlta de(PersonaRequest request) {
            return new DatosAlta(request.nombre(), request.apellido(), request.dni(), request.fechaNacimiento(),
                    request.calle(), request.numeroCalle(), request.codigoArea(), request.numeroTelefono(),
                    request.email(), request.password());
        }
    }

    private final RepositorioDePersonas<E> repositorio;
    private final MapeoDePersona mapeo;
    private final DirectorioDeCuentas directorio;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventos;
    private final PdfService pdfService;

    protected GestionDePersonas(RepositorioDePersonas<E> repositorio, MapeoDePersona mapeo, DirectorioDeCuentas directorio,
                                PasswordEncoder passwordEncoder, ApplicationEventPublisher eventos, PdfService pdfService) {
        this.repositorio = repositorio;
        this.mapeo = mapeo;
        this.directorio = directorio;
        this.passwordEncoder = passwordEncoder;
        this.eventos = eventos;
        this.pdfService = pdfService;
    }

    // ---------- Lo que define cada rol ----------

    protected abstract E nueva();

    protected abstract R aRespuesta(E persona);

    /** Título de la ficha en PDF, por ejemplo "Ficha de socio". */
    protected abstract String tituloDeFicha();

    /** Renglones de la ficha que son propios del rol (después de los datos comunes). */
    protected List<CampoFicha> camposPropios(E persona) {
        return List.of();
    }

    /** Quién puede gestionar este rol, además de lo que ya corta la ruta. Lanza AccessDeniedException si no. */
    protected void exigirPermiso(Actor actor) {
    }

    /** Se llama con la persona ya guardada y con id, por ejemplo para asignar el número de socio. */
    protected void alGuardarPorPrimeraVez(E persona) {
    }

    /** Solo lo pisa Socio: único dato propio del rol en /me. */
    protected String numeroSocioDe(E persona) { return null; }

    /** Reglas de la baja de este rol. Por defecto: nadie elimina su propia cuenta. */
    protected void asegurarQueSePuedeEliminar(E persona, Actor actor) {
        if (persona.getId().equals(actor.id())) {
            throw new ReglaDeNegocioException(CodigoError.PERSONA_ELIMINAR_PROPIA_CUENTA,
                    "No podés eliminar tu propia cuenta");
        }
    }

    protected final RepositorioDePersonas<E> repositorio() {
        return repositorio;
    }

    // ---------- Alta ----------

    @Transactional
    public R registrar(PersonaRequest request, Actor actor) {
        exigirPermiso(actor);
        return crear(DatosAlta.de(request), "persona " + actor.id());
    }

    protected R crear(DatosAlta datos, String origen) {
        validarContrasenia(datos.password());
        directorio.asegurarDniLibre(datos.dni());
        directorio.asegurarEmailLibre(Textos.normalizarEmail(datos.email()));

        E guardada = guardar(construir(datos));
        alGuardarPorPrimeraVez(guardada);
        log.info("Persona {} creada con rol {} ({})", guardada.getId(), guardada.getRol(), origen);
        return aRespuesta(guardada);
    }

    private E construir(DatosAlta datos) {
        E persona = nueva();
        persona.setNombre(datos.nombre());
        persona.setApellido(datos.apellido());
        persona.setDni(datos.dni());
        persona.setFechaNacimiento(datos.fechaNacimiento());
        persona.setCalle(datos.calle());
        persona.setNumeroCalle(datos.numeroCalle());
        persona.setCodigoArea(datos.codigoArea());
        persona.setNumeroTelefono(datos.numeroTelefono());
        persona.setEmail(Textos.normalizarEmail(datos.email()));
        persona.setPasswordHash(passwordEncoder.encode(datos.password()));
        persona.setActivo(true);
        persona.setFechaAlta(LocalDate.now());
        return persona;
    }

    // ---------- Consulta ----------

    @Transactional(readOnly = true)
    public R consultar(Long id) {
        return aRespuesta(obtener(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<PersonaResumenResponse> buscar(String dni, String nombre, int pagina, int tamanio) {
        Pageable pageable = PageRequest.of(
                Math.max(pagina, 0),
                Math.min(Math.max(tamanio, 1), MAX_TAMANIO_PAGINA),
                Sort.by("apellido", "nombre", "id"));
        return PageResponse.de(repositorio.findAll(filtroDe(dni, nombre), pageable), mapeo::aResumen);
    }

    /** Protegido: un rol con su propia respuesta enriquecida (ej. Profesor) lo reusa para armar su propio buscar(). */
    protected Specification<E> filtroDe(String dni, String nombre) {
        if (hayTexto(dni)) {
            return PersonaSpecs.conDni(dniValido(dni.trim()));
        }
        if (hayTexto(nombre)) {
            return PersonaSpecs.conNombreQueContiene(patronDeNombre(nombre.trim().replaceAll("\\s+", " ")));
        }
        return Specification.where(null);
    }

    private String dniValido(String dni) {
        if (!FORMATO_DNI.matcher(dni).matches()) {
            throw new SolicitudInvalidaException(CodigoError.CRITERIO_DE_BUSQUEDA_INVALIDO,
                    "El DNI debe tener 7 u 8 números");
        }
        return dni;
    }

    private String patronDeNombre(String nombre) {
        if (nombre.length() < MIN_LARGO_BUSQUEDA_POR_NOMBRE) {
            throw new SolicitudInvalidaException(CodigoError.CRITERIO_DE_BUSQUEDA_INVALIDO,
                    "El nombre a buscar debe tener al menos " + MIN_LARGO_BUSQUEDA_POR_NOMBRE + " caracteres");
        }
        return Textos.patronContiene(nombre);
    }

    // ---------- Modificación ----------

    @Transactional
    public R modificar(Long id, PersonaUpdateRequest request, Actor actor) {
        exigirPermiso(actor);
        E persona = obtener(id);
        asegurarQueNoChoca(persona, request);

        mapeo.aplicar(request, persona);
        E guardada = guardar(persona);
        log.info("Persona {} modificada por persona {}", id, actor.id());
        return aRespuesta(guardada);
    }

    /** Solo se revisa lo que cambia: dejar el mismo DNI o correo no choca con uno mismo. */
    private void asegurarQueNoChoca(E persona, PersonaUpdateRequest cambios) {
        if (cambios.dni() != null && !cambios.dni().equals(persona.getDni())) {
            directorio.asegurarDniLibre(cambios.dni());
        }
        if (cambios.email() != null && !Textos.normalizarEmail(cambios.email()).equalsIgnoreCase(persona.getEmail())) {
            directorio.asegurarEmailLibre(Textos.normalizarEmail(cambios.email()));
        }
    }

    // ---------- Mi perfil (/me), sin Actor ni permiso de rol: la lógica vive en MiPerfilDePersona ----------

    /** Para que MiPerfilService elija cuál de los cinco roles usar. */
    public final Rol rol() { return nueva().getRol(); }

    @Transactional(readOnly = true)
    public MiPerfilResponse miPerfil(Long id) { return miPerfilDePersona().consultar(id); }

    @Transactional
    public MiPerfilResponse actualizarMiPerfil(Long id, PersonaUpdateRequest cambios) { return miPerfilDePersona().actualizar(id, cambios); }

    @Transactional
    public MiPerfilResponse actualizarMiPerfil(Long id, PersonaUpdateRequest cambios, Sexo sexo, String localidad) { return miPerfilDePersona().actualizar(id, cambios, sexo, localidad); }

    @Transactional
    public void cambiarMiPassword(Long id, String actual, String nueva) { miPerfilDePersona().cambiarPassword(id, actual, nueva); }

    @Transactional
    public String actualizarMiFoto(Long id, String url) { return miPerfilDePersona().actualizarFoto(id, url); }

    @Transactional
    public void quitarMiFoto(Long id) { miPerfilDePersona().quitarFoto(id); }

    private MiPerfilDePersona<E> miPerfilDePersona() {
        return new MiPerfilDePersona<>(
                this::obtener, this::guardar, this::asegurarQueNoChoca, this::numeroSocioDe, mapeo, passwordEncoder);
    }

    // ---------- Baja ----------

    @Transactional
    public void eliminar(Long id, Actor actor) {
        exigirPermiso(actor);
        E persona = obtener(id);
        asegurarQueSePuedeEliminar(persona, actor);

        borrar(persona);
        log.info("Persona {} eliminada por persona {}", id, actor.id());
    }

    private void borrar(E persona) {
        // Otros módulos (por ejemplo auth) limpian lo suyo antes de borrar, sin que este módulo los conozca
        eventos.publishEvent(new PersonaEliminadaEvent(persona.getId()));
        try {
            repositorio.delete(persona);
            repositorio.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoException(CodigoError.PERSONA_CON_HISTORIAL,
                    "La persona tiene reservas, inscripciones o asistencias. Deshabilitala para conservar su historial");
        }
    }

    // ---------- Impresión ----------

    @Transactional(readOnly = true)
    public byte[] imprimir(Long id) {
        E persona = obtener(id);
        List<CampoFicha> campos = new ArrayList<>(camposComunes(persona));
        campos.addAll(camposPropios(persona));
        return pdfService.generarFicha(tituloDeFicha(), campos);
    }

    private List<CampoFicha> camposComunes(E persona) {
        return List.of(
                new CampoFicha("Nombre", persona.getNombre()),
                new CampoFicha("Apellido", persona.getApellido()),
                new CampoFicha("DNI", persona.getDni()),
                new CampoFicha("Fecha de nacimiento", formatear(persona.getFechaNacimiento())),
                new CampoFicha("Dirección", direccionDe(persona)),
                new CampoFicha("Teléfono", telefonoDe(persona)),
                new CampoFicha("Correo electrónico", persona.getEmail()),
                new CampoFicha("Tipo de persona", persona.getRol().name()),
                new CampoFicha("Estado", Boolean.TRUE.equals(persona.getActivo()) ? "Activa" : "Deshabilitada"),
                new CampoFicha("Fecha de alta", formatear(persona.getFechaAlta())));
    }

    /** Para la ficha se arma el texto con los campos sueltos: "Av. Siempreviva 742". */
    private String direccionDe(Persona persona) {
        if (persona.getCalle() == null) {
            return null;
        }
        return persona.getNumeroCalle() == null ? persona.getCalle() : persona.getCalle() + " " + persona.getNumeroCalle();
    }

    /** Para la ficha se arma el texto con los campos sueltos: "(011) 12345678". */
    private String telefonoDe(Persona persona) {
        if (persona.getNumeroTelefono() == null) {
            return null;
        }
        return persona.getCodigoArea() == null ? persona.getNumeroTelefono()
                : "(0" + persona.getCodigoArea() + ") " + persona.getNumeroTelefono();
    }

    protected static String formatear(LocalDate fecha) {
        return fecha == null ? null : fecha.format(FORMATO_FECHA);
    }

    private E guardar(E persona) {
        try {
            return repositorio.saveAndFlush(persona);
        } catch (DataIntegrityViolationException e) {
            // Dos pedidos simultáneos con el mismo DNI o correo: la restricción única de la base desempata
            throw new ConflictoException(CodigoError.PERSONA_DUPLICADA,
                    "Ya existe una persona con ese DNI o correo electrónico");
        }
    }

    private E obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Persona no encontrada"));
    }

    static void validarContrasenia(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_CONTRASENIA) {
            throw new ReglaDeNegocioException(CodigoError.CONTRASENIA_DEMASIADO_LARGA,
                    "La contraseña no puede superar los " + MAX_BYTES_CONTRASENIA
                            + " bytes (las letras con tilde y los símbolos ocupan más de uno)");
        }
    }

    private boolean hayTexto(String texto) {
        return texto != null && !texto.isBlank();
    }
}
