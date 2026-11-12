package com.tenisahora.modules.auth.service;

import com.tenisahora.shared.cuenta.CuentaDeAcceso;
import com.tenisahora.shared.cuenta.DirectorioDeCuentas;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonaDetailsService implements UserDetailsService {

    private final DirectorioDeCuentas directorio;

    @Override
    public UserDetails loadUserByUsername(String email) {
        CuentaDeAcceso cuenta = directorio.porEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Persona no encontrada: " + email));

        List<GrantedAuthority> autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + cuenta.rol().name()));

        return User.builder()
                .username(cuenta.email())
                .password(cuenta.passwordHash())
                .authorities(autoridades)
                .disabled(!cuenta.activa())
                .build();
    }
}
