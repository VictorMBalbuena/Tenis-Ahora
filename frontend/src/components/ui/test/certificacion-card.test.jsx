import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CertificacionCard } from "../certificacion-card";

describe("CertificacionCard", () => {
  const datos = {
    numeroCertificacion: "CERT-001",
    entidadEmisora: "FAT",
    fechaEmision: "2024-01-01",
    fechaVencimiento: "",
    archivoTituloUrl: "",
  };

  it("muestra los valores actuales", () => {
    render(<CertificacionCard datos={datos} errores={{}} onChange={() => vi.fn()} />);
    expect(screen.getByDisplayValue("CERT-001")).toBeInTheDocument();
    expect(screen.getByDisplayValue("FAT")).toBeInTheDocument();
  });

  it("muestra el error de un campo requerido", () => {
    render(
      <CertificacionCard
        datos={{ ...datos, numeroCertificacion: "" }}
        errores={{ numeroCertificacion: "Escribí el número de certificación." }}
        onChange={() => vi.fn()}
      />
    );
    expect(screen.getByText("Escribí el número de certificación.")).toBeInTheDocument();
  });
});
