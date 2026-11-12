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

/** Número de teléfono sin el código de área ni el 15: solo números, entre 6 y 8. Una sola definición para todos los DTO. */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = NumeroTelefono.FORMATO)
@ReportAsSingleViolation
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface NumeroTelefono {

    String FORMATO = "\\d{6,8}";

    String message() default "El número de teléfono debe tener entre 6 y 8 números, sin el 15";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
