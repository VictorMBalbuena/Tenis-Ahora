package com.tenisahora.modules.actividad;

import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.entity.Entrenamiento;
import com.tenisahora.modules.actividad.entity.EstadoActividad;
import com.tenisahora.modules.actividad.entity.Modalidad;
import com.tenisahora.modules.actividad.entity.NivelClase;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.instructor.entity.Entrenador;
import com.tenisahora.modules.instructor.entity.EstadoValidacion;
import com.tenisahora.modules.instructor.entity.Profesor;
import com.tenisahora.modules.socio.entity.Socio;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Set;

/** Datos comunes de los tests de clases. "Hoy" es el lunes 19 de octubre de 2026 a las 12:00. */
public final class DatosDePrueba {

    public static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneOffset.UTC);
    public static final LocalDate HOY = LocalDate.of(2026, 10, 19);

    private DatosDePrueba() {
    }

    public static Cancha cancha(Long id) {
        Cancha cancha = new Cancha();
        cancha.setId(id);
        cancha.setIdentificador("Cancha " + id);
        cancha.setTipoSuperficie("CEMENTO");
        cancha.setCapacidad(4);
        cancha.setActiva(true);
        return cancha;
    }

    public static Profesor profesor(Long id, boolean validado) {
        Profesor profesor = new Profesor();
        profesor.setId(id);
        profesor.setNombre("Nombre" + id);
        profesor.setApellido("Apellido" + id);
        profesor.setDni("2000000" + id);
        profesor.setActivo(true);
        profesor.setEstadoValidacion(validado ? EstadoValidacion.VALIDADO : EstadoValidacion.PENDIENTE);
        return profesor;
    }

    public static Entrenador entrenador(Long id, boolean validado) {
        Entrenador entrenador = new Entrenador();
        entrenador.setId(id);
        entrenador.setNombre("Nombre" + id);
        entrenador.setApellido("Apellido" + id);
        entrenador.setDni("4000000" + id);
        entrenador.setActivo(true);
        entrenador.setEstadoValidacion(validado ? EstadoValidacion.VALIDADO : EstadoValidacion.PENDIENTE);
        return entrenador;
    }

    public static Socio socio(Long id) {
        Socio socio = new Socio();
        socio.setId(id);
        socio.setNombre("Nombre" + id);
        socio.setApellido("Apellido" + id);
        socio.setDni("3000000" + id);
        socio.setActivo(true);
        return socio;
    }

    /** Una clase los lunes y miércoles de 18 a 19, programada, con cupo 20. */
    public static Clase clase(Long id) {
        Clase clase = new Clase();
        clase.setId(id);
        clase.setCodigo(String.format("A-%06d", id));
        clase.setNombre("Tenis para principiantes");
        clase.setModalidad(Modalidad.GRUPAL);
        clase.setNivel(NivelClase.INICIAL);
        clase.setCancha(cancha(1L));
        clase.setProfesor(profesor(4L, true));
        clase.setFechaInicio(HOY);
        clase.setFechaFin(HOY.plusMonths(4));
        clase.setHoraInicio(LocalTime.of(18, 0));
        clase.setHoraFin(LocalTime.of(19, 0));
        clase.setEstado(EstadoActividad.PROGRAMADA);
        clase.setCupoMaximo(20);
        return clase;
    }

    /** Un entrenamiento los lunes y miércoles de 18 a 19, programado, con cupo 20. */
    public static Entrenamiento entrenamiento(Long id) {
        Entrenamiento entrenamiento = new Entrenamiento();
        entrenamiento.setId(id);
        entrenamiento.setCodigo(String.format("A-%06d", id));
        entrenamiento.setNombre("Entrenamiento funcional");
        entrenamiento.setModalidad(Modalidad.GRUPAL);
        entrenamiento.setObjetivo("Mejorar el saque");
        entrenamiento.setCancha(cancha(1L));
        entrenamiento.setEntrenador(entrenador(5L, true));
        entrenamiento.setFechaInicio(HOY);
        entrenamiento.setFechaFin(HOY.plusMonths(4));
        entrenamiento.setHoraInicio(LocalTime.of(18, 0));
        entrenamiento.setHoraFin(LocalTime.of(19, 0));
        entrenamiento.setEstado(EstadoActividad.PROGRAMADA);
        entrenamiento.setCupoMaximo(20);
        return entrenamiento;
    }

    public static final Set<DayOfWeek> LUNES_Y_MIERCOLES = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
}
