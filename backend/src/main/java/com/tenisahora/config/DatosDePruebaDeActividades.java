package com.tenisahora.config;

import com.tenisahora.modules.actividad.entity.Actividad;
import com.tenisahora.modules.actividad.entity.ActividadDia;
import com.tenisahora.modules.actividad.entity.Clase;
import com.tenisahora.modules.actividad.entity.Entrenamiento;
import com.tenisahora.modules.actividad.entity.Modalidad;
import com.tenisahora.modules.actividad.entity.NivelClase;
import com.tenisahora.modules.actividad.mapper.ClaseMapper;
import com.tenisahora.modules.actividad.repository.ActividadDiaRepository;
import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.cancha.entity.Cancha;
import com.tenisahora.modules.cancha.repository.CanchaRepository;
import com.tenisahora.modules.instructor.repository.EntrenadorRepository;
import com.tenisahora.modules.instructor.repository.ProfesorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Clases y entrenamientos ficticios con cupo y valor, para ver las tarjetas del socio y probar la seña. No se duplican al reiniciar. */
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DatosDePruebaDeActividades {

    private static final int CUPO = 12;

    private final ActividadRepository actividadRepository;
    private final ActividadDiaRepository diasRepository;
    private final CanchaRepository canchaRepository;
    private final ProfesorRepository profesorRepository;
    private final EntrenadorRepository entrenadorRepository;

    public void cargar() {
        List<String> existentes = actividadRepository.findAll().stream().map(Actividad::getNombre).toList();
        clase(existentes, "Iniciación adultos", NivelClase.INICIAL, "25000", 18, List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY));
        clase(existentes, "Técnica de revés", NivelClase.INTERMEDIO, "28000", 19, List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        clase(existentes, "Saque y volea", NivelClase.AVANZADO, "32000", 20, List.of(DayOfWeek.FRIDAY));
        clase(existentes, "Tenis los sábados", NivelClase.INICIAL, "20000", 10, List.of(DayOfWeek.SATURDAY));
        entrenamiento(existentes, "Preparación física", "Resistencia y velocidad en cancha", "30000", 7, List.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY));
        entrenamiento(existentes, "Alto rendimiento", "Preparación para torneos", "45000", 21, List.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY));
        entrenamiento(existentes, "Juego de dobles", "Táctica y posicionamiento", "26000", 11, List.of(DayOfWeek.SUNDAY));
    }

    private void clase(List<String> existentes, String nombre, NivelClase nivel, String valor, int hora, List<DayOfWeek> dias) {
        if (existentes.contains(nombre)) {
            return;
        }
        Clase clase = new Clase();
        clase.setNivel(nivel);
        clase.setProfesor(profesorRepository.findAll().stream().findFirst().orElse(null));
        guardar(clase, nombre, valor, hora, dias);
    }

    private void entrenamiento(List<String> existentes, String nombre, String objetivo, String valor, int hora, List<DayOfWeek> dias) {
        if (existentes.contains(nombre)) {
            return;
        }
        Entrenamiento entrenamiento = new Entrenamiento();
        entrenamiento.setObjetivo(objetivo);
        entrenamiento.setEntrenador(entrenadorRepository.findAll().stream().findFirst().orElse(null));
        guardar(entrenamiento, nombre, valor, hora, dias);
    }

    private void guardar(Actividad actividad, String nombre, String valor, int hora, List<DayOfWeek> dias) {
        Cancha cancha = canchaRepository.findByActivaTrueOrderByIdentificador().stream().findFirst().orElse(null);
        actividad.setNombre(nombre);
        actividad.setCancha(cancha);
        actividad.setModalidad(Modalidad.GRUPAL);
        actividad.setFechaInicio(LocalDate.now().plusDays(10));
        actividad.setFechaFin(LocalDate.now().plusDays(100));
        actividad.setHoraInicio(LocalTime.of(hora, 0));
        actividad.setHoraFin(LocalTime.of(hora + 1, 0));
        actividad.setCupoMaximo(CUPO);
        actividad.setValorMensual(new BigDecimal(valor));
        Actividad guardada = actividadRepository.save(actividad);
        guardada.setCodigo(ClaseMapper.codigoDe(guardada.getId()));
        actividadRepository.save(guardada);
        diasRepository.saveAll(dias.stream().map(dia -> new ActividadDia(guardada, dia)).toList());
    }
}
