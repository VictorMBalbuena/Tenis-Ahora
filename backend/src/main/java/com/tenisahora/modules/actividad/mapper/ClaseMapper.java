package com.tenisahora.modules.actividad.mapper;

import com.tenisahora.modules.actividad.dto.AlumnoDeClaseResponse;
import com.tenisahora.modules.actividad.dto.ClaseResponse;
import com.tenisahora.modules.actividad.dto.InscripcionResponse;
import com.tenisahora.modules.actividad.dto.MiClaseResponse;
import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.entity.InscripcionClase;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.shared.entity.Persona;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.Set;

@Component
public class ClaseMapper {

    public ClaseResponse aRespuesta(Clase clase, Set<DayOfWeek> dias, int cupoOcupado) {
        return new ClaseResponse(clase.getId(), clase.getCodigo(), clase.getNombre(), clase.getModalidad(), clase.getNivel(),
                clase.getEstado(), clase.getCancha().getId(), clase.getCancha().getIdentificador(),
                clase.getProfesor().getId(), nombreDe(clase.getProfesor()), clase.getFechaInicio(), clase.getFechaFin(), dias,
                clase.getHoraInicio(), clase.getHoraFin(), clase.getCupoMaximo(), clase.getCupoMinimo(), cupoOcupado,
                clase.getCupoMaximo() - cupoOcupado, clase.getValorMensual(), clase.getCategoria());
    }

    public MiClaseResponse aMiClase(Clase clase, Set<DayOfWeek> dias, int cupoOcupado) {
        return new MiClaseResponse(clase.getId(), clase.getCodigo(), clase.getNombre(), clase.getNivel(), clase.getEstado(),
                clase.getCancha().getIdentificador(), clase.getFechaInicio(), clase.getFechaFin(), dias, clase.getHoraInicio(),
                clase.getHoraFin(), clase.getCupoMaximo(), cupoOcupado);
    }

    public AlumnoDeClaseResponse aAlumno(InscripcionClase inscripcion) {
        Socio socio = inscripcion.getSocio();
        return new AlumnoDeClaseResponse(socio.getId(), nombreDe(socio), socio.getDni(), inscripcion.getFechaInscripcion(),
                inscripcion.getId(), inscripcion.getEstado());
    }

    public InscripcionResponse aInscripcionResponse(InscripcionClase inscripcion) {
        Socio socio = inscripcion.getSocio();
        return new InscripcionResponse(inscripcion.getId(), inscripcion.getActividad().getId(), socio.getId(), nombreDe(socio),
                inscripcion.getFechaInscripcion(), inscripcion.getEstado());
    }

    /** El código de la actividad es el id con formato: A-000012. */
    public static String codigoDe(Long id) {
        return String.format("A-%06d", id);
    }

    private String nombreDe(Persona persona) {
        return persona.getApellido() + ", " + persona.getNombre();
    }
}
