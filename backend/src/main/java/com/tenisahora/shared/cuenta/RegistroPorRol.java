package com.tenisahora.shared.cuenta;

import com.tenisahora.shared.entity.Persona;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ConflictoException;
import com.tenisahora.shared.exception.RecursoNoEncontradoException;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.persona.RepositorioDePersonas;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** El puerto de cuentas sobre el repositorio de un rol. Cada módulo de rol lo declara con una línea. */
public abstract class RegistroPorRol<E extends Persona> implements RegistroDeCuentas {

    private final RepositorioDePersonas<E> repositorio;
    private final Rol rol;

    protected RegistroPorRol(RepositorioDePersonas<E> repositorio, Rol rol) {
        this.repositorio = repositorio;
        this.rol = rol;
    }

    /** Una instancia vacía de la entidad de este rol: la necesita el traspaso para armar la fila nueva (CU74). */
    protected abstract E nueva();

    /**
     * Lo que este rol completa cuando recibe a alguien de otro rol y la fila ya tiene id. El socio, por ejemplo,
     * se numera acá. Por defecto no hay nada que hacer.
     */
    protected void alRecibirEnTraspaso(E persona) {
    }

    @Override
    public boolean atiende(Rol otroRol) {
        return rol == otroRol;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CuentaDeAcceso> porEmail(String email) {
        return repositorio.findByEmail(email).map(this::cuentaDe);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CuentaDeAcceso> porId(Long id) {
        return repositorio.findById(id).map(this::cuentaDe);
    }

    @Override
    public boolean existeDni(String dni) {
        return repositorio.existsByDni(dni);
    }

    @Override
    public boolean existeEmail(String email) {
        return repositorio.existsByEmailIgnoreCase(email);
    }

    // ---------- Administración de la cuenta (CU74, CU75, CU76) ----------

    @Override
    @Transactional(readOnly = true)
    public Optional<DatosDeCuenta> datosDe(Long id) {
        return repositorio.findById(id).map(persona -> new DatosDeCuenta(
                persona.getId(), persona.getNombre(), persona.getApellido(), persona.getDni(), persona.getEmail(),
                rol, Boolean.TRUE.equals(persona.getActivo()), persona.getRolPersonalizadoId()));
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, boolean activa) {
        E persona = obtener(id);
        persona.setActivo(activa);
        repositorio.saveAndFlush(persona);
    }

    @Override
    @Transactional
    public void asignarRolPersonalizado(Long id, Long rolPersonalizadoId) {
        E persona = obtener(id);
        persona.setRolPersonalizadoId(rolPersonalizadoId);
        repositorio.saveAndFlush(persona);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DatosTransferibles> datosParaTraspaso(Long id) {
        return repositorio.findById(id).map(p -> new DatosTransferibles(
                p.getNombre(), p.getApellido(), p.getDni(), p.getFechaNacimiento(), p.getCalle(), p.getNumeroCalle(),
                p.getCodigoArea(), p.getNumeroTelefono(), p.getEmail(), p.getPasswordHash(), p.getActivo(),
                p.getFechaAlta(), p.getFotoUrl(), p.getRolPersonalizadoId()));
    }

    @Override
    @Transactional
    public void soltarParaTraspaso(Long id) {
        E persona = obtener(id);
        try {
            repositorio.delete(persona);
            repositorio.flush();
        } catch (DataIntegrityViolationException e) {
            // La fila quedó referenciada por reservas, inscripciones o asistencias: moverla de tabla dejaría ese
            // historial colgado, así que el cambio de rol no se hace.
            throw new ConflictoException(CodigoError.PERSONA_CON_HISTORIAL,
                    "La persona tiene movimientos registrados con su rol actual, así que no se le puede cambiar el rol. "
                            + "Deshabilitala y creá una cuenta nueva con el rol que necesitás");
        }
    }

    @Override
    @Transactional
    public Long recibirEnTraspaso(DatosTransferibles datos) {
        E persona = nueva();
        persona.setNombre(datos.nombre());
        persona.setApellido(datos.apellido());
        persona.setDni(datos.dni());
        persona.setFechaNacimiento(datos.fechaNacimiento());
        persona.setCalle(datos.calle());
        persona.setNumeroCalle(datos.numeroCalle());
        persona.setCodigoArea(datos.codigoArea());
        persona.setNumeroTelefono(datos.numeroTelefono());
        persona.setEmail(datos.email());
        persona.setPasswordHash(datos.passwordHash());
        persona.setActivo(datos.activo());
        persona.setFechaAlta(datos.fechaAlta());
        persona.setFotoUrl(datos.fotoUrl());
        persona.setRolPersonalizadoId(datos.rolPersonalizadoId());

        E guardada = repositorio.saveAndFlush(persona);
        alRecibirEnTraspaso(guardada);
        return repositorio.saveAndFlush(guardada).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public long activas() {
        return repositorio.findAll().stream().filter(p -> Boolean.TRUE.equals(p.getActivo())).count();
    }

    @Override
    @Transactional(readOnly = true)
    public long conRolPersonalizado(Long rolPersonalizadoId) {
        return repositorio.findAll().stream()
                .filter(p -> rolPersonalizadoId.equals(p.getRolPersonalizadoId()))
                .count();
    }

    private E obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() ->
                new RecursoNoEncontradoException(CodigoError.PERSONA_NO_ENCONTRADA, "Persona no encontrada"));
    }

    private CuentaDeAcceso cuentaDe(E persona) {
        return new CuentaDeAcceso(persona.getId(), persona.getEmail(), persona.getPasswordHash(), rol,
                Boolean.TRUE.equals(persona.getActivo()), persona.getRolPersonalizadoId());
    }
}
