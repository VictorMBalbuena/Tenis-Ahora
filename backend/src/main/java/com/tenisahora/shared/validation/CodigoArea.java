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

/** Código de área argentino sin el 0 inicial: solo números, entre 2 y 4. Una sola definición para todos los DTO. */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = CodigoArea.FORMATO)
@ReportAsSingleViolation
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface CodigoArea {

    String FORMATO = "\\d{2,4}";

    String message() default "El código de área debe tener entre 2 y 4 números, sin el 0";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
