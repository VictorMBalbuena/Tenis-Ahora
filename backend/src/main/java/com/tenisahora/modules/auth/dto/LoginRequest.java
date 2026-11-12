package com.tenisahora.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "socio@tenisahora.com") @NotBlank @Email String email,
        @Schema(example = "TenisAhora2026!") @NotBlank String password
) {
}
