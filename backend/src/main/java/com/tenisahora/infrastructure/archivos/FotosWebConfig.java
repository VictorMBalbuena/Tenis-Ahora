package com.tenisahora.infrastructure.archivos;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Sirve las fotos de perfil guardadas en disco como recurso estático público, en /fotos/**. */
@Configuration
@EnableConfigurationProperties(FotosProperties.class)
@RequiredArgsConstructor
public class FotosWebConfig implements WebMvcConfigurer {

    private final FotosProperties propiedades;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String directorio = propiedades.directorio().endsWith("/") ? propiedades.directorio() : propiedades.directorio() + "/";
        registry.addResourceHandler("/fotos/**").addResourceLocations("file:" + directorio);
    }
}
