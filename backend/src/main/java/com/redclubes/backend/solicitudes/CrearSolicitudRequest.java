package com.redclubes.backend.solicitudes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record CrearSolicitudRequest(
        @NotNull @Valid DatosSolicitud datos,
        @AssertTrue @NotNull Boolean consentimiento,
        @NotBlank @Pattern(regexp = "2026-10-v1") String consentimientoVersion,
        @NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") String claveEnvio,
        @Size(max = 200) String sitioWeb
) {}
