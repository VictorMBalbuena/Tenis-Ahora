package com.tenisahora.modules.perfil.service;

import com.tenisahora.infrastructure.archivos.AlmacenamientoDeFotos;
import com.tenisahora.infrastructure.archivos.FotoInvalidaException;
import com.tenisahora.modules.perfil.dto.CambiarPasswordRequest;
import com.tenisahora.modules.perfil.dto.FotoResponse;
import com.tenisahora.modules.perfil.dto.MiPerfilUpdateRequest;
import com.tenisahora.shared.dto.Actor;
import com.tenisahora.shared.dto.MiPerfilResponse;
import com.tenisahora.shared.exception.CodigoError;
import com.tenisahora.shared.exception.ReglaDeNegocioException;
import com.tenisahora.shared.model.Rol;
import com.tenisahora.shared.persona.GestionDePersonas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * GET/PATCH /me, la foto y la contraseña propias. La lógica de cada operación ya vive una sola vez en
 * GestionDePersonas (socio, empleado, administrador, profesor y entrenador la heredan); este servicio solo elige,
 * según el rol del actor, cuál de los cinco usar.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked"})
public class MiPerfilService {

    private final List<GestionDePersonas> gestiones;
    private final AlmacenamientoDeFotos almacenamiento;

    public MiPerfilResponse consultar(Actor actor) {
        return gestionDe(actor.rol()).miPerfil(actor.id());
    }

    public MiPerfilResponse actualizar(Actor actor, MiPerfilUpdateRequest cambios) {
        return gestionDe(actor.rol()).actualizarMiPerfil(actor.id(), cambios.aPersonaUpdateRequest(), cambios.sexo(), cambios.localidad());
    }

    public void cambiarPassword(Actor actor, CambiarPasswordRequest request) {
        gestionDe(actor.rol()).cambiarMiPassword(actor.id(), request.passwordActual(), request.passwordNueva());
    }

    public FotoResponse subirFoto(Actor actor, MultipartFile archivo) {
        String url;
        try {
            url = almacenamiento.guardar(actor.id(), archivo);
        } catch (FotoInvalidaException e) {
            throw new ReglaDeNegocioException(CodigoError.FOTO_INVALIDA, e.getMessage());
        }
        return new FotoResponse(gestionDe(actor.rol()).actualizarMiFoto(actor.id(), url));
    }

    public void quitarFoto(Actor actor) {
        almacenamiento.eliminar(actor.id());
        gestionDe(actor.rol()).quitarMiFoto(actor.id());
    }

    private GestionDePersonas gestionDe(Rol rol) {
        return gestiones.stream()
                .filter(gestion -> gestion.rol() == rol)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay gestión de personas para el rol " + rol));
    }
}
