package com.tenisahora.config;

import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.insumo.entity.CategoriaInsumo;
import com.tenisahora.modules.insumo.entity.Insumo;
import com.tenisahora.modules.insumo.repository.CategoriaInsumoRepository;
import com.tenisahora.modules.insumo.repository.InsumoRepository;
import com.tenisahora.modules.cancha.entity.TipoCancha;
import com.tenisahora.modules.cancha.repository.TipoCanchaRepository;
import com.tenisahora.modules.cancha.entity.HorarioSemanal;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.cancha.repository.HorarioSemanalRepository;
import com.tenisahora.modules.administrador.entity.Administrador;
import com.tenisahora.modules.empleado.entity.Empleado;
import com.tenisahora.modules.instructor.entity.Certificacion;
import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.modules.instructor.entity.Profesor;
import com.tenisahora.modules.instructor.repository.CertificacionRepository;
import com.tenisahora.modules.instructor.repository.ProfesorRepository;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.shared.entity.Persona;
import com.tenisahora.modules.administrador.repository.AdministradorRepository;
import com.tenisahora.modules.empleado.repository.EmpleadoRepository;
import com.tenisahora.modules.socio.repository.SocioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.time.LocalDate;

@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String PASSWORD_PRUEBA = "TenisAhora2026!";

    private final AdministradorRepository administradorRepository;
    private final EmpleadoRepository empleadoRepository;
    private final SocioRepository socioRepository;
    private final ProfesorRepository profesorRepository;
    private final CertificacionRepository certificacionRepository;
    private final CanchaRepository canchaRepository;
    private final TipoCanchaRepository tipoCanchaRepository;
    private final HorarioSemanalRepository horarioRepository;
    private final InsumoRepository insumoRepository;
    private final CategoriaInsumoRepository categoriaInsumoRepository;
    private final DatosDePruebaDeTorneos datosDePruebaDeTorneos;
    private final DatosDePruebaDeActividades datosDePruebaDeActividades;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        cargarPersonas();
        cargarTiposDeCancha();
        cargarCanchas();
        cargarHorarios();
        cargarCategoriasDeInsumo();
        cargarInsumos();
        cargarDatosDePrueba();
    }

    /** Son solo para ver las pantallas llenas: si fallan, la aplicación tiene que arrancar igual. */
    private void cargarDatosDePrueba() {
        try {
            datosDePruebaDeActividades.cargar();
            datosDePruebaDeTorneos.cargar(socio -> {
                socio.setPasswordHash(encoder.encode(PASSWORD_PRUEBA));
                socio.setFechaAlta(LocalDate.now());
            });
        } catch (RuntimeException e) {
            log.error("No se pudieron cargar los datos de prueba", e);
        }
    }

    private void cargarPersonas() {
        if (!administradorRepository.existsByEmailIgnoreCase("admin@tenisahora.com")) {
            Administrador admin = new Administrador();
            completar(admin, "Admin", "Prueba", "10000001", "admin@tenisahora.com");
            administradorRepository.save(admin);
        }
        if (!empleadoRepository.existsByEmailIgnoreCase("empleado@tenisahora.com")) {
            Empleado empleado = new Empleado();
            completar(empleado, "Empleado", "Prueba", "10000002", "empleado@tenisahora.com");
            empleado.setFechaContratacion(LocalDate.now());
            empleadoRepository.save(empleado);
        }
        if (!socioRepository.existsByEmailIgnoreCase("socio@tenisahora.com")) {
            Socio socio = new Socio();
            completar(socio, "Socio", "Prueba", "10000003", "socio@tenisahora.com");
            socio.setNumeroSocio("S-0001");
            socioRepository.save(socio);
        }
        if (!profesorRepository.existsByEmailIgnoreCase("profesor@tenisahora.com")) {
            cargarProfesor();
        }
    }

    /** Validado, con título y certificación vigente: para probar BACK 11 sin tener que pasar por CU20/CU31 a mano. */
    private void cargarProfesor() {
        Certificacion certificacion = certificacionRepository.findByNumeroCertificacionIgnoreCase("CERT-SEED-0001")
                .orElseGet(() -> {
                    Certificacion c = new Certificacion();
                    c.setNumeroCertificacion("CERT-SEED-0001");
                    c.setEntidadEmisora("Federación Argentina de Tenis");
                    c.setFechaEmision(LocalDate.now().minusYears(1));
                    c.setFechaVencimiento(LocalDate.now().plusYears(2));
                    return certificacionRepository.save(c);
                });

        Profesor profesor = new Profesor();
        completar(profesor, "Profesor", "Prueba", "10000004", "profesor@tenisahora.com");
        profesor.setFechaContratacion(LocalDate.now());
        profesor.setTituloHabilitante("Profesor Nacional de Educación Física");
        profesor.setCertificacion(certificacion);
        profesor.setEspecialidad("Tenis general");
        profesor.setValorPorHora(new BigDecimal("8000.00"));
        profesor.setEstadoValidacion(EstadoValidacion.VALIDADO);
        profesor.setFechaValidacion(LocalDate.now());
        profesorRepository.save(profesor);
    }

    private void completar(Persona u, String nombre, String apellido, String dni, String email) {
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setDni(dni);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(PASSWORD_PRUEBA));
        u.setFechaAlta(LocalDate.now());
    }

    private void cargarTiposDeCancha() {
        crearTipoCancha("Polvo de Ladrillo Rojo Dobles", "Polvo de ladrillo (arcilla roja)", 4, "La más lenta; bote muy alto y permite deslizarse");
        crearTipoCancha("Arcilla Verde Singles", "Har-Tru (arcilla verde)", 2, "Ligeramente más rápida y compacta que la roja (típica de EE. UU.)");
        crearTipoCancha("Rápida Acrílica Pro Dobles", "Resina acrílica", 4, "Velocidad media-rápida, amortiguación intermedia y rebote uniforme");
        crearTipoCancha("Hormigón Clásico Dobles", "Cemento / Hormigón", 4, "Rígida, veloz, bote muy regular y alto desgaste articular");
        crearTipoCancha("Pasto Tradicional Singles", "Hierba natural", 2, "La más rápida tradicional; bote bajo, veloz e irregular");
        crearTipoCancha("Césped Sintético Dobles", "Césped artificial con arena", 4, "Bote medio-bajo, menor mantenimiento y muy común en clubes");
        crearTipoCancha("Moqueta Indoor Singles", "Alfombra textil sintética", 2, "Extremadamente rápida y bote muy rasante bajo techo");
        crearTipoCancha("Parqué Dobles", "Madera (parqué)", 4, "Rebote rápido y bajo, exclusiva de recintos techados cerrados");
    }

    private void crearTipoCancha(String nombre, String superficie, int capacidad, String descripcion) {
        TipoCancha tipo = tipoCanchaRepository.findByNombreIgnoreCase(nombre).orElseGet(TipoCancha::new);
        tipo.setNombre(nombre);
        tipo.setSuperficie(superficie);
        tipo.setCapacidad(capacidad);
        tipo.setDescripcion(descripcion);
        tipo.setActivo(true);
        tipoCanchaRepository.save(tipo);
    }

    private void cargarCanchas() {
        TipoCancha ladrillo = tipoCanchaRepository.findByNombreIgnoreCase("Polvo de Ladrillo Rojo Dobles")
                .or(() -> tipoCanchaRepository.findByNombreIgnoreCase("Polvo de Ladrillo Dobles"))
                .orElse(null);
        TipoCancha harTru = tipoCanchaRepository.findByNombreIgnoreCase("Arcilla Verde Singles")
                .or(() -> tipoCanchaRepository.findByNombreIgnoreCase("Arcilla Verde Har-Tru"))
                .orElse(null);
        TipoCancha acrilica = tipoCanchaRepository.findByNombreIgnoreCase("Rápida Acrílica Pro Dobles")
                .or(() -> tipoCanchaRepository.findByNombreIgnoreCase("Rápida Acrílica Pro"))
                .orElse(null);
        TipoCancha hormigon = tipoCanchaRepository.findByNombreIgnoreCase("Hormigón Clásico Dobles")
                .or(() -> tipoCanchaRepository.findByNombreIgnoreCase("Hormigón Clásico"))
                .orElse(null);
        TipoCancha cespedSintetico = tipoCanchaRepository.findByNombreIgnoreCase("Césped Sintético Dobles")
                .or(() -> tipoCanchaRepository.findByNombreIgnoreCase("Césped Sintético Club"))
                .orElse(null);

        crearCancha("Cancha 1", new BigDecimal("8000"), ladrillo, false, true, true, true, "Cancha principal de arcilla roja con tribuna lateral");
        crearCancha("Cancha 2", new BigDecimal("7500"), ladrillo, false, true, false, false, "Cancha de polvo de ladrillo intermedia");
        crearCancha("Cancha 3", new BigDecimal("6500"), harTru, false, true, false, false, "Cancha de arcilla verde americana para individuales");
        crearCancha("Cancha 4", new BigDecimal("7000"), acrilica, true, true, false, true, "Cancha techada de resina acrílica Plexicushion");
        crearCancha("Cancha 5", new BigDecimal("6000"), hormigon, false, false, false, false, "Cancha rápida de cemento descubierta");
        crearCancha("Cancha 6", new BigDecimal("8500"), cespedSintetico, false, true, false, true, "Cancha de césped sintético con arena");
    }

    private void crearCancha(String identificador, BigDecimal precio, TipoCancha tipoCancha,
                             boolean techada, boolean iluminacion, boolean tribuna, boolean aptaTorneos, String observaciones) {
        Cancha cancha = canchaRepository.findByIdentificador(identificador).orElseGet(Cancha::new);
        cancha.setIdentificador(identificador);
        cancha.setTipoCancha(tipoCancha);
        cancha.setTipoSuperficie(tipoCancha != null ? tipoCancha.getSuperficie() : null);
        cancha.setCapacidad(tipoCancha != null ? tipoCancha.getCapacidad() : 4);
        cancha.setPrecioPorHora(precio);
        cancha.setTechada(techada);
        cancha.setIluminacion(iluminacion);
        cancha.setTribuna(tribuna);
        cancha.setAptaTorneos(aptaTorneos);
        cancha.setObservaciones(observaciones);
        cancha.setActiva(true);
        canchaRepository.save(cancha);
    }

    /** Cada cancha sin horarios queda abierta todos los días de 8 a 22, para poder probar la grilla. */
    private void cargarHorarios() {
        canchaRepository.findAll().stream()
                .filter(cancha -> !horarioRepository.existsByCanchaId(cancha.getId()))
                .forEach(cancha -> horarioRepository.saveAll(Arrays.stream(DayOfWeek.values())
                        .map(dia -> horarioDe(cancha, dia)).toList()));
    }

    private HorarioSemanal horarioDe(Cancha cancha, DayOfWeek dia) {
        HorarioSemanal horario = new HorarioSemanal();
        horario.setCancha(cancha);
        horario.setDiaSemana(dia);
        horario.setHoraInicio(LocalTime.of(8, 0));
        horario.setHoraFin(LocalTime.of(22, 0));
        return horario;
    }

    /** RAQUETA, PELOTA y RED son el mínimo que pide la cátedra; de acá en más son editables desde la app. */
    private void cargarCategoriasDeInsumo() {
        Arrays.asList("Raqueta", "Pelota", "Red").forEach(nombre -> {
            if (!categoriaInsumoRepository.existsByNombreIgnoreCase(nombre)) {
                categoriaInsumoRepository.save(new CategoriaInsumo(nombre));
            }
        });
    }

    private void cargarInsumos() {
        CategoriaInsumo raqueta = categoriaDe("Raqueta");
        CategoriaInsumo pelota = categoriaDe("Pelota");
        CategoriaInsumo red = categoriaDe("Red");
        crearInsumo("Raquetas", raqueta, 24, 6);
        crearInsumo("Pelotas de tenis", pelota, 46, 15);
        crearInsumo("Redes", red, 7, 8);
    }

    private CategoriaInsumo categoriaDe(String nombre) {
        return categoriaInsumoRepository.findAllByOrderByNombreAsc().stream()
                .filter(categoria -> categoria.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .orElseThrow();
    }

    private void crearInsumo(String nombre, CategoriaInsumo categoria, int cantidad, int minima) {
        Insumo insumo = insumoRepository.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> {
                    Insumo i = new Insumo();
                    i.setNombre(nombre);
                    i.setCantidadDisponible(cantidad);
                    i.setCantidadMinima(minima);
                    return i;
                });
        insumo.setCategoria(categoria);
        insumoRepository.save(insumo);
    }
}
