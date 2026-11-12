package com.tenisahora.shared.cuenta;

import java.time.LocalDate;

/**
 * Los datos de Persona que sobreviven a un cambio de rol (CU74). Son exactamente los de la clase abstracta Persona:
 * lo propio de cada rol (número de socio, título habilitante, certificación) no se traslada, porque pertenece al rol
 * que la persona deja.
 *
 * <p>Va el hash de la contraseña, no la contraseña: el cambio de rol no obliga a la persona a elegir una nueva.
 */
public record DatosTransferibles(String nombre, String apellido, String dni, LocalDate fechaNacimiento,
                                 String calle, String numeroCalle, String codigoArea, String numeroTelefono,
                                 String email, String passwordHash, Boolean activo, LocalDate fechaAlta,
                                 String fotoUrl, Long rolPersonalizadoId) {
}
