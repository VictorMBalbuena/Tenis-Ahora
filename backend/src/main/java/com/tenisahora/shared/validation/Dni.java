package com.tenisahora.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** DNI argentino: solo números, 7 u 8. Una sola definición para todos los DTO (y para el servicio, con FORMATO). */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = Dni.FORMATO)
@ReportAsSingleViolation
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface Dni {

    String FORMATO = "\\d{7,8}";

    String message() default "El DNI debe tener 7 u 8 números";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
