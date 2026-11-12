package com.tenisahora.infrastructure.archivos;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Guarda y borra la foto de perfil en el disco del servidor, servida como recurso estático en /fotos (ver
 * {@link FotosWebConfig}). El archivo se nombra con el id de la persona: una foto nueva pisa a la anterior sin dejar
 * huérfanos, y no hace falta guardar el nombre de archivo en ningún lado más que en fotoUrl.
 */
@Service
@RequiredArgsConstructor
public class AlmacenamientoDeFotos {

    private static final Map<String, String> EXTENSION_POR_TIPO = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private static final long TAMANIO_MAXIMO_BYTES = 2L * 1024 * 1024;

    private final FotosProperties propiedades;

    public String guardar(Long personaId, MultipartFile archivo) {
        String extension = extensionValida(archivo);
        borrarCualquierExtension(personaId);

        Path destino = directorio().resolve(personaId + "." + extension);
        try {
            Files.createDirectories(destino.getParent());
            archivo.transferTo(destino);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la foto de perfil", e);
        }
        return urlPublica(destino.getFileName().toString());
    }

    public void eliminar(Long personaId) {
        borrarCualquierExtension(personaId);
    }

    private String extensionValida(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new FotoInvalidaException("Elegí una imagen");
        }
        String extension = EXTENSION_POR_TIPO.get(archivo.getContentType());
        if (extension == null) {
            throw new FotoInvalidaException("La foto tiene que ser JPG, PNG o WebP");
        }
        if (archivo.getSize() > TAMANIO_MAXIMO_BYTES) {
            throw new FotoInvalidaException("La foto no puede pesar más de 2 MB");
        }
        return extension;
    }

    private void borrarCualquierExtension(Long personaId) {
        EXTENSION_POR_TIPO.values().forEach(extension -> {
            try {
                Files.deleteIfExists(directorio().resolve(personaId + "." + extension));
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo borrar la foto de perfil anterior", e);
            }
        });
    }

    private Path directorio() {
        return Path.of(propiedades.directorio());
    }

    private String urlPublica(String nombreDeArchivo) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/fotos/")
                .path(nombreDeArchivo)
                .toUriString();
    }
}
