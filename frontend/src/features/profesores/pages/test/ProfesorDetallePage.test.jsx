import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "@/store/authStore";
import { decidirCertificacionProfesor, imprimirProfesor, obtenerProfesor } from "../../api/profesoresApi";
import { EstadoValidacion } from "../../model/profesor";
import ProfesorDetallePage from "../ProfesorDetallePage";

vi.mock("../../api/profesoresApi", () => ({
  obtenerProfesor: vi.fn(),
  decidirCertificacionProfesor: vi.fn(),
  imprimirProfesor: vi.fn(),
}));

const profesorMock = {
  id: 7,
  nombre: "Bruno",
  apellido: "Tealdi",
  dni: "44966422",
  email: "bruno@correo.com",
  codigoArea: "11",
  numeroTelefono: "22786604",
  calle: "Av. Rivadavia",
  numeroCalle: "4820",
  activo: true,
  tituloHabilitante: "Profesor Nacional de Tenis",
  estadoValidacion: EstadoValidacion.PENDIENTE,
  certificacion: { numeroCertificacion: "CERT-001", entidadEmisora: "FAT", fechaVencimiento: "2028-01-01", vigente: true },
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/profesores/7"]}>
      <Routes>
        <Route path="/profesores/:id" element={<ProfesorDetallePage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe("ProfesorDetallePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: { email: "admin@tenisahora.com", rol: "ADMINISTRADOR" } });
  });

  it("muestra los datos del profesor", async () => {
    obtenerProfesor.mockResolvedValueOnce(profesorMock);
    renderPage();

    expect(await screen.findByText("Bruno Tealdi")).toBeInTheDocument();
    expect(screen.getByText("Pendiente de validar")).toBeInTheDocument();
  });

  it("como administrador, puede aprobar la certificación", async () => {
    obtenerProfesor.mockResolvedValueOnce(profesorMock);
    decidirCertificacionProfesor.mockResolvedValueOnce({ ...profesorMock, estadoValidacion: EstadoValidacion.VALIDADO, fechaValidacion: "2026-10-24" });
    renderPage();

    await screen.findByText("Bruno Tealdi");
    await userEvent.click(screen.getByRole("button", { name: /Aprobar certificación/ }));

    expect(decidirCertificacionProfesor).toHaveBeenCalledWith("7", { aprobada: true });
    expect(await screen.findByText("Validada")).toBeInTheDocument();
  });

  it("como administrador, puede rechazar la certificación con un motivo", async () => {
    obtenerProfesor.mockResolvedValueOnce(profesorMock);
    decidirCertificacionProfesor.mockResolvedValueOnce({ ...profesorMock, estadoValidacion: "RECHAZADA", motivoRechazo: "Archivo ilegible" });
    renderPage();

    await screen.findByText("Bruno Tealdi");
    await userEvent.click(screen.getByRole("button", { name: "Rechazar certificación" }));
    await userEvent.type(screen.getByPlaceholderText(/archivo del título no es legible/), "Archivo ilegible");
    await userEvent.click(screen.getByRole("button", { name: "Rechazar" }));

    expect(decidirCertificacionProfesor).toHaveBeenCalledWith("7", { aprobada: false, motivo: "Archivo ilegible" });
    expect(await screen.findByText("Motivo del rechazo: Archivo ilegible")).toBeInTheDocument();
  });

  it("si no es administrador, no ve el botón de aprobar", async () => {
    useAuthStore.setState({ user: { email: "empleado@tenisahora.com", rol: "EMPLEADO" } });
    obtenerProfesor.mockResolvedValueOnce(profesorMock);
    renderPage();

    await screen.findByText("Bruno Tealdi");
    expect(screen.queryByRole("button", { name: /Aprobar certificación/ })).not.toBeInTheDocument();
  });

  it("imprimir pide el PDF y lo abre", async () => {
    obtenerProfesor.mockResolvedValueOnce(profesorMock);
    imprimirProfesor.mockResolvedValueOnce(new Blob(["pdf"]));
    window.URL.createObjectURL = vi.fn(() => "blob:mock");
    const abrir = vi.spyOn(window, "open").mockImplementation(() => {});

    renderPage();
    await screen.findByText("Bruno Tealdi");
    await userEvent.click(screen.getByRole("button", { name: "Imprimir ficha" }));

    expect(imprimirProfesor).toHaveBeenCalledWith("7");
    expect(abrir).toHaveBeenCalledWith("blob:mock", "_blank");
  });

  it("si falla la carga, muestra el estado de error", async () => {
    obtenerProfesor.mockRejectedValueOnce(new Error("404"));
    renderPage();

    expect(await screen.findByText("No se encontraron los datos del profesor.")).toBeInTheDocument();
  });
});
