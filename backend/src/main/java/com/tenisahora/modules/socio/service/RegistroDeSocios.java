package com.tenisahora.modules.socio.service;

import com.tenisahora.modules.socio.entity.Socio;
import com.tenisahora.modules.socio.repository.SocioRepository;
import com.tenisahora.shared.cuenta.RegistroPorRol;
import com.tenisahora.shared.model.Rol;
import org.springframework.stereotype.Component;

@Component
public class RegistroDeSocios extends RegistroPorRol<Socio> {

    public RegistroDeSocios(SocioRepository repositorio) {
        super(repositorio, Rol.SOCIO);
    }

    @Override
    protected Socio nueva() {
        return new Socio();
    }

    /** Quien pasa a ser socio recibe su número, igual que en un alta normal (ver SocioService). */
    @Override
    protected void alRecibirEnTraspaso(Socio socio) {
        socio.setNumeroSocio(String.format("S-%04d", socio.getId()));
    }
}
