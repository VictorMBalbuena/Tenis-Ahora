package com.tenisahora.config;

import com.tenisahora.modules.actividad.repository.ActividadRepository;
import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.modules.torneo.dto.CargarResultadoRequest;
import com.tenisahora.modules.torneo.dto.PartidoResponse;
import com.tenisahora.modules.torneo.entity.CategoriaTorneo;
import com.tenisahora.modules.torneo.entity.EstadoInscripcionTorneo;
import com.tenisahora.modules.torneo.entity.Inscripcion;
import com.tenisahora.modules.torneo.entity.Modalidad;
import com.tenisahora.modules.torneo.entity.Torneo;
import com.tenisahora.modules.torneo.repository.InscripcionRepository;
import com.tenisahora.modules.torneo.repository.TorneoRepository;
import com.tenisahora.modules.torneo.service.FixtureService;
import com.tenisahora.shared.model.Sexo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Socios y torneos ficticios para ver las pantallas llenas. Quedan en la base y no se duplican al reiniciar. */
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DatosDePruebaDeTorneos {

    private static final String VALOR_DE_PRUEBA_DE_ACTIVIDAD = "30000";
    private static final String REGLAMENTO = "Partidos al mejor de 3 sets con tie-break. Tolerancia de 15 minutos. "
            + "Se aplican las reglas de la Asociación Argentina de Tenis.";
    private static final List<String> HOMBRES = List.of("Lucas Fernández", "Martín Gómez", "Nicolás Díaz", "Tomás Romero",
            "Facundo Torres", "Agustín Sosa", "Santiago Ruiz", "Mateo Álvarez");
    private static final List<String> MUJERES = List.of("Sofía Herrera", "Valentina Castro", "Camila Medina", "Lucía Ortiz",
            "Martina Rojas", "Julieta Vega", "Florencia Silva", "Paula Acosta");

    private final SocioRepository socioRepository;
    private final TorneoRepository torneoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final FixtureService fixtureService;
    private final ActividadRepository actividadRepository;

    public void cargar(Consumer<Socio> completarCuenta) {
        List<Socio> hombres = socios(HOMBRES, Sexo.MASCULINO, 20000001, completarCuenta);
        List<Socio> mujeres = socios(MUJERES, Sexo.FEMENINO, 20000101, completarCuenta);
        cargarAbiertos(hombres, mujeres);
        cargarEnCurso(hombres);
        cargarFinalizado(mujeres);
        ponerValorAActividadesSinValor();
    }

    private void cargarAbiertos(List<Socio> hombres, List<Socio> mujeres) {
        LocalDate hoy = LocalDate.now();
        abierto("Copa Primavera", CategoriaTorneo.MASCULINO, Modalidad.SINGLE, "Polvo de ladrillo (arcilla roja)", hoy.plusDays(20), "15000", hombres.subList(0, 7), false);
        abierto("Abierto Femenino", CategoriaTorneo.FEMENINO, Modalidad.SINGLE, "Cemento / Hormigón", hoy.plusDays(25), "12000", mujeres.subList(0, 8), false);
        abierto("Dobles del Club", CategoriaTorneo.MASCULINO, Modalidad.PAREJA, "Polvo de ladrillo (arcilla roja)", hoy.plusDays(30), "20000", hombres, true);
        abierto("Dobles Femenino de Verano", CategoriaTorneo.FEMENINO, Modalidad.PAREJA, "Hierba natural", hoy.plusDays(40), "20000", mujeres.subList(0, 4), true);
        abierto("Copa Aniversario", CategoriaTorneo.MASCULINO, Modalidad.SINGLE, "Hierba natural", hoy.plusDays(50), "10000", hombres.subList(0, 3), false);
        abierto("Torneo Interno de Otoño", CategoriaTorneo.FEMENINO, Modalidad.SINGLE, "Polvo de ladrillo (arcilla roja)", hoy.plusDays(60), "8000", List.of(), false);
        abierto("Encuentro Amistoso", CategoriaTorneo.FEMENINO, Modalidad.SINGLE, "Cemento / Hormigón", hoy.plusDays(70), "0", mujeres.subList(0, 2), false);
    }

    private void cargarEnCurso(List<Socio> hombres) {
        Torneo torneo = abierto("Torneo Apertura", CategoriaTorneo.MASCULINO, Modalidad.SINGLE, "Cemento / Hormigón",
                LocalDate.now().minusDays(3), "15000", hombres, false);
        generarCuadroSiFalta(torneo);
        List<String> tanteadores = List.of("6-3 6-4", "7-5 3-6 6-2");
        for (int i = 0; i < tanteadores.size(); i++) {
            resultadoSiFalta(torneo, i, tanteadores.get(i));
        }
    }

    private void cargarFinalizado(List<Socio> mujeres) {
        Torneo torneo = abierto("Liga de Invierno", CategoriaTorneo.FEMENINO, Modalidad.SINGLE, "Polvo de ladrillo (arcilla roja)",
                LocalDate.now().minusDays(60), "10000", mujeres.subList(0, 4), false);
        generarCuadroSiFalta(torneo);
        int partidos = fixtureService.listarPartidos(torneo.getId()).size();
        for (int i = 0; i < partidos; i++) {
            resultadoSiFalta(torneo, i, "6-2 6-4");
        }
    }

    private void generarCuadroSiFalta(Torneo torneo) {
        if (fixtureService.listarPartidos(torneo.getId()).isEmpty()) {
            fixtureService.generarFixture(torneo.getId());
        }
    }

    private void resultadoSiFalta(Torneo torneo, int indice, String sets) {
        PartidoResponse partido = fixtureService.listarPartidos(torneo.getId()).get(indice);
        if (partido.sets() == null) {
            fixtureService.cargarResultado(torneo.getId(), partido.id(), new CargarResultadoRequest(sets));
        }
    }

    /** Completa lo que falte en vez de saltear lo que ya existe: si un reinicio corta la carga, la próxima la termina. */
    private Torneo abierto(String nombre, CategoriaTorneo categoria, Modalidad modalidad, String superficie,
                           LocalDate inicio, String arancel, List<Socio> jugadores, boolean enParejas) {
        Torneo torneo = torneoRepository.findFirstByNombre(nombre).orElseGet(() -> {
            Torneo nuevo = new Torneo();
            nuevo.setNombre(nombre);
            nuevo.setCategoria(categoria);
            nuevo.setModalidad(modalidad);
            nuevo.setTipoSuperficie(superficie);
            nuevo.setFechaInicio(inicio);
            nuevo.setFechaFin(inicio.plusDays(7));
            nuevo.setCostoInscripcion(new BigDecimal(arancel));
            nuevo.setReglamento(REGLAMENTO);
            return torneoRepository.save(nuevo);
        });
        if (fixtureService.listarPartidos(torneo.getId()).isEmpty()) {
            inscribir(torneo, jugadores, enParejas);
        }
        return torneo;
    }

    private void inscribir(Torneo torneo, List<Socio> jugadores, boolean enParejas) {
        int paso = enParejas ? 2 : 1;
        for (int i = 0; i + paso <= jugadores.size(); i += paso) {
            if (inscripcionRepository.existsSocioEnTorneo(torneo.getId(), jugadores.get(i).getId())) {
                continue;
            }
            Inscripcion inscripcion = new Inscripcion();
            inscripcion.setTorneo(torneo);
            inscripcion.setJugador1(jugadores.get(i));
            inscripcion.setPareja(enParejas ? jugadores.get(i + 1) : null);
            inscripcion.setEquipo(i / paso + 1);
            inscripcion.setEstado(EstadoInscripcionTorneo.CONFIRMADA);
            inscripcion.setMontoTotal(torneo.getCostoInscripcion());
            inscripcion.setFechaInscripcion(LocalDate.now().minusDays(10));
            inscripcionRepository.save(inscripcion);
        }
    }

    private List<Socio> socios(List<String> nombres, Sexo sexo, int primerDni, Consumer<Socio> completarCuenta) {
        List<Socio> socios = new ArrayList<>();
        for (int i = 0; i < nombres.size(); i++) {
            socios.add(socio(nombres.get(i), sexo, String.valueOf(primerDni + i), completarCuenta));
        }
        return socios;
    }

    private Socio socio(String nombreCompleto, Sexo sexo, String dni, Consumer<Socio> completarCuenta) {
        String email = "socio" + dni + "@tenisahora.com";
        return socioRepository.findByDniAndActivoTrue(dni).orElseGet(() -> {
            String[] partes = nombreCompleto.split(" ");
            Socio socio = new Socio();
            socio.setNombre(partes[0]);
            socio.setApellido(partes[1]);
            socio.setDni(dni);
            socio.setEmail(email);
            socio.setSexo(sexo);
            socio.setNumeroSocio("S-" + dni);
            completarCuenta.accept(socio);
            return socioRepository.save(socio);
        });
    }

    /** Sin valor una actividad no cobra seña: para poder probar el pago, las que no tienen reciben uno ficticio. */
    private void ponerValorAActividadesSinValor() {
        actividadRepository.findAll().stream()
                .filter(actividad -> actividad.getValorMensual() == null)
                .forEach(actividad -> {
                    actividad.setValorMensual(new BigDecimal(VALOR_DE_PRUEBA_DE_ACTIVIDAD));
                    actividadRepository.save(actividad);
                });
    }
}
