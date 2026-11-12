import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { actualizarEntrenador, obtenerEntrenador } from "../../api/entrenadoresApi";
import EditarEntrenadorPage from "../EditarEntrenadorPage";

vi.mock("../../api/entrenadoresApi", () => ({
  obtenerEntrenador: vi.fn(),
  actualizarEntrenador: vi.fn(),
}));

const entrenadorMock = {
  id: 7,
  nombre: "Lucas",
  apellido: "Pérez",
  dni: "31123456",
  fechaNacimiento: "1992-03-10",
  calle: "Av. Siempreviva",
  numeroCalle: "742",
  codigoArea: "11",
  numeroTelefono: "12345678",
  email: "lucas@correo.com",
  certificacionDeportiva: "Entrenador Nacional de Tenis",
  especialidad: "Alto rendimiento",
  valorPorHora: 9500,
  certificacion: {
    numeroCertificacion: "PF-2023-0092",
    entidadEmisora: "ISEF",
    fechaEmision: "2023-04-18",
    fechaVencimiento: "2027-04-18",
    archivoTituloUrl: null,
  },
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/entrenadores/editar/7"]}>
      <Routes>
        <Route path="/entrenadores/editar/:id" element={<EditarEntrenadorPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe("EditarEntrenadorPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("carga los datos del entrenador y los muestra en el formulario", async () => {
    obtenerEntrenador.mockResolvedValueOnce(entrenadorMock);
    renderPage();

    expect(await screen.findByDisplayValue("Lucas")).toBeInTheDocument();
    expect(screen.getByDisplayValue("PF-2023-0092")).toBeInTheDocument();
  });

  it("si no cambió nada, no llama a actualizarEntrenador", async () => {
    obtenerEntrenador.mockResolvedValueOnce(entrenadorMock);
    renderPage();

    await screen.findByDisplayValue("Lucas");
    await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

    expect(actualizarEntrenador).not.toHaveBeenCalled();
  });

  it("al cambiar el nombre, manda solo ese campo", async () => {
    obtenerEntrenador.mockResolvedValueOnce(entrenadorMock);
    actualizarEntrenador.mockResolvedValueOnce(entrenadorMock);
    renderPage();

    await screen.findByDisplayValue("Lucas");
    await userEvent.type(screen.getByDisplayValue("Lucas"), "o");
    await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

    expect(actualizarEntrenador).toHaveBeenCalledWith("7", { nombre: "Lucaso" });
  });

  it("si falla la carga, muestra el estado de error", async () => {
    obtenerEntrenador.mockRejectedValueOnce(new Error("404"));
    renderPage();

    expect(await screen.findByText("No se encontraron los datos del entrenador.")).toBeInTheDocument();
  });
});
