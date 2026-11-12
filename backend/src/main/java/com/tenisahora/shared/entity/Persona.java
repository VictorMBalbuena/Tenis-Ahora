package com.tenisahora.shared.entity;

import com.tenisahora.shared.model.Sexo;
import com.tenisahora.shared.model.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;

/**
 * Clase abstracta Persona del diagrama de clases: solo comparte atributos y comportamiento. No tiene tabla; cada rol
 * (socio, empleado, administrador, profesor, entrenador) guarda todos sus datos en la suya.
 *
 * <p>El id sale de una única secuencia compartida, así que es único entre todas las tablas de rol. Por eso el token y
 * las referencias de otros módulos pueden llevar solo el número.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "persona_id_seq")
    @SequenceGenerator(name = "persona_id_seq", sequenceName = "persona_id_seq", allocationSize = 1)
    protected Long id;

    @Column(nullable = false)
    protected String nombre;

    @Column(nullable = false)
    protected String apellido;

    @Column(nullable = false, unique = true, length = 20)
    protected String dni;

    @Column(name = "fecha_nacimiento")
    protected LocalDate fechaNacimiento;

    @Column(length = 150)
    protected String calle;

    @Column(name = "numero_calle", length = 6)
    protected String numeroCalle;

    @Column(name = "codigo_area", length = 4)
    protected String codigoArea;

    @Column(name = "numero_telefono", length = 8)
    protected String numeroTelefono;

    @Column(nullable = false, unique = true)
    protected String email;

    @Column(name = "password_hash", nullable = false)
    protected String passwordHash;

    @Column(nullable = false)
    protected Boolean activo = true;

    @Column(name = "fecha_alta", nullable = false)
    protected LocalDate fechaAlta;

    @Column(name = "foto_url")
    protected String fotoUrl;

    /**
     * Rol personalizado que le dio el administrador (CU85), si tiene uno. Es solo el número: el módulo de permisos
     * es el que sabe qué significa, y así Persona no depende de él. Sin rol personalizado, los permisos de la
     * cuenta son los configurados para su rol base (CU75).
     */
    @Column(name = "rol_personalizado_id")
    protected Long rolPersonalizadoId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    protected Sexo sexo;

    @Column(length = 100)
    protected String localidad;

    /** El rol lo da la clase, no una columna: una fila de socio no puede decir que es profesor. */
    public abstract Rol getRol();

    public Integer calcularEdad() {
        if (fechaNacimiento == null) return null;
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }
}
