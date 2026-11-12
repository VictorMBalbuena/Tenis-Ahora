package com.tenisahora.modules.permiso.service;

import com.tenisahora.modules.permiso.model.AccionSobreModulo;
import com.tenisahora.modules.permiso.model.CodigoDePermiso;
import com.tenisahora.modules.permiso.model.ModuloDelSistema;
import com.tenisahora.modules.permiso.model.PermisoEspecial;
import com.tenisahora.shared.model.Rol;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.tenisahora.modules.permiso.model.AccionSobreModulo.CREAR;
import static com.tenisahora.modules.permiso.model.AccionSobreModulo.EDITAR;
import static com.tenisahora.modules.permiso.model.AccionSobreModulo.ELIMINAR;
import static com.tenisahora.modules.permiso.model.AccionSobreModulo.VER;
import static com.tenisahora.modules.permiso.model.ModuloDelSistema.ACTIVIDADES;
import static com.tenisahora.modules.permiso.model.ModuloDelSistema.CANCHAS;
import static com.tenisahora.modules.permiso.model.ModuloDelSistema.CLASES;
import static com.tenisahora.modules.permiso.model.ModuloDelSistema.COBROS;
import static com.tenisahora.modules.permiso.model.ModuloDelSistema.ENTRENADORES;
import static com.tenisahora.modules.permiso.model.ModuloDelSistema.ENTRENAMIENTOS;
import static com.tenisahora.modules.permiso.model.ModuloDelSistema.INSUMOS;
import static com.tenisahora.modules.permiso.model.ModuloDelSistema.PROFESORES;
import static com.tenisahora.modules.permiso.model.ModuloDelSistema.RECIBOS;
import static com.tenisahora.modules.permiso.model.ModuloDelSistema.RESERVAS;
import static com.tenisahora.modules.permiso.model.ModuloDelSistema.TORNEOS;
import static com.tenisahora.modules.permiso.model.ModuloDelSistema.USUARIOS;

/**
 * La matriz con la que arrancan los cinco roles base.
 *
 * <p><b>Está escrita para no cambiar nada.</b> Reproduce lo que cada rol ya podía hacer antes de que existiera el
 * módulo de permisos, leyendo las reglas por ruta de {@code SecurityConfig}. El control de permisos se suma a esas
 * reglas, nunca las reemplaza: una casilla marcada acá no habilita nada que la ruta no habilitara ya. Por eso, el
 * día que se activa, ningún rol gana ni pierde acceso; recién cambia algo cuando un administrador edita la matriz.
 *
 * <p>Se usa solo la primera vez, cuando el rol todavía no existe en la base. Después manda lo que haya configurado
 * el administrador, y esta clase no vuelve a pisarlo.
 */
final class PermisosPorDefecto {

    private PermisosPorDefecto() {
    }

    static Set<String> de(Rol rol) {
        return switch (rol) {
            case ADMINISTRADOR -> todo();
            case EMPLEADO -> delEmpleado();
            case SOCIO -> delSocio();
            case PROFESOR -> delProfesor();
            case ENTRENADOR -> delEntrenador();
        };
    }

    /** El administrador puede todo: es el único rol que administra permisos. */
    private static Set<String> todo() {
        Set<String> codigos = new LinkedHashSet<>();
        for (ModuloDelSistema modulo : ModuloDelSistema.values()) {
            for (AccionSobreModulo accion : AccionSobreModulo.values()) {
                codigos.add(CodigoDePermiso.de(modulo, accion));
            }
        }
        for (PermisoEspecial especial : PermisoEspecial.values()) {
            codigos.add(CodigoDePermiso.de(especial));
        }
        return codigos;
    }

    /** Todo el mostrador del club. Quedan afuera descuentos, reportes, administradores y la administración de roles. */
    private static Set<String> delEmpleado() {
        Set<String> codigos = new LinkedHashSet<>();
        for (ModuloDelSistema modulo : new ModuloDelSistema[]{USUARIOS, CANCHAS, RESERVAS, INSUMOS, PROFESORES,
                ENTRENADORES, CLASES, ENTRENAMIENTOS, ACTIVIDADES, TORNEOS, COBROS, RECIBOS}) {
            codigos.addAll(todasLasAcciones(modulo));
        }
        // Bloquear una cancha por mantenimiento ya era parte de /api/canchas/**, que es del personal
        codigos.add(CodigoDePermiso.de(PermisoEspecial.BLOQUEAR_CANCHAS));
        return codigos;
    }

    /** Lo que un socio hace por su cuenta: reservar, pagar, anotarse. */
    private static Set<String> delSocio() {
        Set<String> codigos = new LinkedHashSet<>();
        codigos.addAll(todasLasAcciones(RESERVAS));
        codigos.addAll(acciones(COBROS, VER, CREAR, EDITAR));
        codigos.addAll(acciones(TORNEOS, VER, CREAR));
        // Inscribirse y darse de baja de una clase o un entrenamiento
        codigos.addAll(acciones(CLASES, VER, CREAR, ELIMINAR));
        codigos.addAll(acciones(ENTRENAMIENTOS, VER, CREAR, ELIMINAR));
        // Ver sus propias inscripciones: la pantalla de clases y entrenamientos del socio las pide junto con
        // el listado (GET /api/actividades/inscripciones/mias). Sin esto, esa pantalla no carga.
        codigos.addAll(acciones(ACTIVIDADES, VER));
        // Solo para mirar la grilla de disponibilidad
        codigos.addAll(acciones(CANCHAS, VER));
        return codigos;
    }

    /** El profesor ve sus clases y toma asistencia. */
    private static Set<String> delProfesor() {
        Set<String> codigos = new LinkedHashSet<>(acciones(CLASES, VER));
        codigos.addAll(acciones(ACTIVIDADES, VER, CREAR));
        return codigos;
    }

    /** El entrenador, lo mismo con los entrenamientos. */
    private static Set<String> delEntrenador() {
        Set<String> codigos = new LinkedHashSet<>(acciones(ENTRENAMIENTOS, VER));
        codigos.addAll(acciones(ACTIVIDADES, VER, CREAR));
        return codigos;
    }

    private static Set<String> todasLasAcciones(ModuloDelSistema modulo) {
        return acciones(modulo, VER, CREAR, EDITAR, ELIMINAR);
    }

    private static Set<String> acciones(ModuloDelSistema modulo, AccionSobreModulo... acciones) {
        return Arrays.stream(acciones)
                .map(accion -> CodigoDePermiso.de(modulo, accion))
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }
}
