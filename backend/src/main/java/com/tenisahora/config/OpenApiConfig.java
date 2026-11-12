package com.tenisahora.config;

import com.tenisahora.shared.dto.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;

/**
 * Contrato de la API (DIS 06): lo que se ve en /swagger-ui. Define el esquema de autenticación JWT y agrega solo las
 * respuestas de error comunes (400, 401, 403, 404, 429), así los controllers solo describen lo propio de cada endpoint.
 */
@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tenis Ahora API")
                        .version("0.0.1")
                        .description("API del sistema de gestión del club de tenis. Para probar los endpoints protegidos: "
                                + "iniciar sesión en POST /api/auth/login, copiar el accessToken y pegarlo en *Authorize*."))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token de acceso (accessToken) que devuelve el login. Vence a los 30 minutos.")));
    }

    /** Publica el esquema ErrorResponse, al que apuntan todas las respuestas de error. */
    @Bean
    public OpenApiCustomizer esquemaDeError() {
        return openApi -> ModelConverters.getInstance().readAll(ErrorResponse.class)
                .forEach(openApi.getComponents()::addSchemas);
    }

    /** Agrega a cada endpoint los errores que le corresponden, sin pisar los que el controller ya describió. */
    @Bean
    public OperationCustomizer erroresComunes() {
        return (operation, metodo) -> {
            ApiResponses respuestas = respuestasDe(operation);
            if (tieneDatosDeEntrada(operation)) {
                agregarSiFalta(respuestas, "400", "Datos con formato inválido. El detalle por campo viene en `campos`");
            }
            if (pideSesion(metodo)) {
                agregarSiFalta(respuestas, "401", "Sin sesión, o la sesión venció");
                agregarSiFalta(respuestas, "403", "Tu rol no puede hacer esto");
            }
            if (tieneIdEnLaRuta(operation)) {
                agregarSiFalta(respuestas, "404", "No existe lo que se pidió");
            }
            if (esDeAutenticacion(metodo)) {
                agregarSiFalta(respuestas, "429", "Demasiados pedidos desde la misma IP. Retry-After dice cuántos segundos esperar");
            }
            conEsquemaDeError(respuestas);
            return operation;
        };
    }

    private ApiResponses respuestasDe(Operation operation) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        return operation.getResponses();
    }

    private void agregarSiFalta(ApiResponses respuestas, String codigo, String descripcion) {
        if (!respuestas.containsKey(codigo)) {
            respuestas.addApiResponse(codigo, new ApiResponse().description(descripcion));
        }
    }

    /** Toda respuesta 4xx se describe con el esquema ErrorResponse, la haya declarado el controller o la agregue esta clase. */
    private void conEsquemaDeError(ApiResponses respuestas) {
        Content contenido = new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")));
        respuestas.forEach((codigo, respuesta) -> {
            if (codigo.startsWith("4")) {
                respuesta.setContent(contenido);
            }
        });
    }

    private boolean tieneDatosDeEntrada(Operation operation) {
        return operation.getRequestBody() != null
                || (operation.getParameters() != null && !operation.getParameters().isEmpty());
    }

    private boolean tieneIdEnLaRuta(Operation operation) {
        return operation.getParameters() != null
                && operation.getParameters().stream().anyMatch(parametro -> "path".equals(parametro.getIn()));
    }

    private boolean pideSesion(HandlerMethod metodo) {
        return AnnotatedElementUtils.hasAnnotation(metodo.getBeanType(), SecurityRequirement.class)
                || metodo.hasMethodAnnotation(SecurityRequirement.class);
    }

    private boolean esDeAutenticacion(HandlerMethod metodo) {
        RequestMapping mapeo = AnnotatedElementUtils.findMergedAnnotation(metodo.getBeanType(), RequestMapping.class);
        return mapeo != null && Arrays.stream(mapeo.value()).anyMatch(ruta -> ruta.startsWith("/api/auth"));
    }
}
