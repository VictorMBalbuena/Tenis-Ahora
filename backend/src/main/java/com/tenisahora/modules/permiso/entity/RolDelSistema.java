package com.tenisahora.modules.permiso.entity;

import com.tenisahora.shared.model.Rol;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Un rol con su conjunto de permisos (CU75, CU85). En la misma tabla conviven los cinco roles base, que existen
 * desde siempre y no se borran, y los roles que crea el administrador.
 *
 * <p>Un rol creado siempre se apoya en un rol base: la persona tiene que vivir en alguna de las cinco tablas de
 * rol, así que un rol nuevo no inventa una tabla, define un conjunto de permisos distinto sobre un rol que ya
 * existe. Por ejemplo, "Recepción" puede apoyarse en EMPLEADO y no incluir el permiso de anular cobros.
 */
@Entity
@Table(name = "rol_del_sistema")
@Getter
@Setter
@NoArgsConstructor
public class RolDelSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    /** Los cinco roles base no se editan en su identidad ni se borran: solo se les cambian los permisos. */
    @Column(nullable = false)
    private Boolean base = false;

    /** En qué tabla de rol vive quien tiene este rol. Para los roles base, es el rol mismo. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rol_base", nullable = false, length = 20)
    private Rol rolBase;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDate fechaAlta = LocalDate.now();

    @Column(name = "creado_por_id")
    private Long creadoPorId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rol_permiso", joinColumns = @JoinColumn(name = "rol_id"))
    @Column(name = "codigo", nullable = false, length = 60)
    private Set<String> permisos = new LinkedHashSet<>();

    public boolean esBase() {
        return Boolean.TRUE.equals(base);
    }

    public void reemplazarPermisos(Set<String> codigos) {
        permisos.clear();
        permisos.addAll(new LinkedHashSet<>(codigos));
    }

    public Set<String> permisosCopiados() {
        return new HashSet<>(permisos);
    }
}
