import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuthStore } from "@/store/authStore";
import { decidirCertificacionEntrenador, imprimirEntrenador, obtenerEntrenador } from "../../api/entrenadoresApi";
import { EstadoValidacion } from "../../model/entrenador";
import EntrenadorDetallePage from "../EntrenadorDetallePage";

vi.mock("../../api/entrenadoresApi", () => ({
  obtenerEntrenador: vi.fn(),
  decidirCertificacionEntrenador: vi.fn(),
  imprimirEntrenador: vi.fn(),
}));

const entrenadorMock = {
  id: 7,
  nombre: "Lucas",
  apellido: "Pérez",
  dni: "31123456",
  email: "lucas@correo.com",
  codigoArea: "11",
  numeroTelefono: "12345678",
  calle: "Av. Siempreviva",
  numeroCalle: "742",
  activo: true,
  certificacionDeportiva: "Entrenador Nacional de Tenis",
  estadoValidacion: EstadoValidacion.PENDIENTE,
  certificacion: { numeroCertificacion: "PF-2023-0092", entidadEmisora: "ISEF", fechaVencimiento: "2028-01-01", vigente: true },
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/entrenadores/7"]}>
      <Routes>
        <Route path="/entrenadores/:id" element={<EntrenadorDetallePage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe("EntrenadorDetallePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: { email: "admin@tenisahora.com", rol: "ADMINISTRADOR" } });
  });

  it("muestra los datos del entrenador", async () => {
    obtenerEntrenador.mockResolvedValueOnce(entrenadorMock);
    renderPage();

    expect(await screen.findByText("Lucas Pérez")).toBeInTheDocument();
    expect(screen.getByText("Pendiente de validar")).toBeInTheDocument();
  });

  it("como administrador, puede aprobar la certificación", async () => {
    obtenerEntrenador.mockResolvedValueOnce(entrenadorMock);
    decidirCertificacionEntrenador.mockResolvedValueOnce({ ...entrenadorMock, estadoValidacion: EstadoValidacion.VALIDADO, fechaValidacion: "2026-10-24" });
    renderPage();

    await screen.findByText("Lucas Pérez");
    await userEvent.click(screen.getByRole("button", { name: /Aprobar certificación/ }));

    expect(decidirCertificacionEntrenador).toHaveBeenCalledWith("7", { aprobada: true });
    expect(await screen.findByText("Validada")).toBeInTheDocument();
  });

  it("si no es administrador, no ve el botón de aprobar", async () => {
    useAuthStore.setState({ user: { email: "empleado@tenisahora.com", rol: "EMPLEADO" } });
    obtenerEntrenador.mockResolvedValueOnce(entrenadorMock);
    renderPage();

    await screen.findByText("Lucas Pérez");
    expect(screen.queryByRole("button", { name: /Aprobar certificación/ })).not.toBeInTheDocument();
  });

  it("imprimir pide el PDF y lo abre", async () => {
    obtenerEntrenador.mockResolvedValueOnce(entrenadorMock);
    imprimirEntrenador.mockResolvedValueOnce(new Blob(["pdf"]));
    window.URL.createObjectURL = vi.fn(() => "blob:mock");
    const abrir = vi.spyOn(window, "open").mockImplementation(() => {});

    renderPage();
    await screen.findByText("Lucas Pérez");
    await userEvent.click(screen.getByRole("button", { name: "Imprimir ficha" }));

    expect(imprimirEntrenador).toHaveBeenCalledWith("7");
    expect(abrir).toHaveBeenCalledWith("blob:mock", "_blank");
  });

  it("si falla la carga, muestra el estado de error", async () => {
    obtenerEntrenador.mockRejectedValueOnce(new Error("404"));
    renderPage();

    expect(await screen.findByText("No se encontraron los datos del entrenador.")).toBeInTheDocument();
  });
});
