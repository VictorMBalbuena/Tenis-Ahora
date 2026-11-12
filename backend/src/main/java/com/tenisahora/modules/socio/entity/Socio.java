package com.tenisahora.modules.socio.entity;

import com.tenisahora.shared.entity.Persona;
import com.tenisahora.shared.model.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "socio")
@Getter
@Setter
@NoArgsConstructor
public class Socio extends Persona {

    @Column(name = "numero_socio")
    private String numeroSocio;

    @Column(name = "codigo_barras")
    private String codigoBarras;

    @Override
    public Rol getRol() {
        return Rol.SOCIO;
    }
}
