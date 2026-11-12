import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { actualizarProfesor, obtenerProfesor } from "../../api/profesoresApi";
import EditarProfesorPage from "../EditarProfesorPage";

vi.mock("../../api/profesoresApi", () => ({
  obtenerProfesor: vi.fn(),
  actualizarProfesor: vi.fn(),
}));

const profesorMock = {
  id: 7,
  nombre: "Bruno",
  apellido: "Tealdi",
  dni: "44966422",
  fechaNacimiento: "1993-07-02",
  calle: "Av. Rivadavia",
  numeroCalle: "4820",
  codigoArea: "11",
  numeroTelefono: "22786604",
  email: "bruno@correo.com",
  tituloHabilitante: "Profesor Nacional de Tenis",
  especialidad: "Tenis infantil",
  valorPorHora: 8500,
  permisoPersonalAutorizado: false,
  certificacion: {
    numeroCertificacion: "CERT-001",
    entidadEmisora: "FAT",
    fechaEmision: "2024-01-01",
    fechaVencimiento: "2028-01-01",
    archivoTituloUrl: null,
  },
};

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/profesores/editar/7"]}>
      <Routes>
        <Route path="/profesores/editar/:id" element={<EditarProfesorPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe("EditarProfesorPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("carga los datos del profesor y los muestra en el formulario", async () => {
    obtenerProfesor.mockResolvedValueOnce(profesorMock);
    renderPage();

    expect(await screen.findByDisplayValue("Bruno")).toBeInTheDocument();
    expect(screen.getByDisplayValue("CERT-001")).toBeInTheDocument();
  });

  it("si no cambió nada, no llama a actualizarProfesor", async () => {
    obtenerProfesor.mockResolvedValueOnce(profesorMock);
    renderPage();

    await screen.findByDisplayValue("Bruno");
    await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

    expect(actualizarProfesor).not.toHaveBeenCalled();
  });

  it("al cambiar el nombre, manda solo ese campo", async () => {
    obtenerProfesor.mockResolvedValueOnce(profesorMock);
    actualizarProfesor.mockResolvedValueOnce(profesorMock);
    renderPage();

    await screen.findByDisplayValue("Bruno");
    await userEvent.type(screen.getByDisplayValue("Bruno"), "o");
    await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

    expect(actualizarProfesor).toHaveBeenCalledWith("7", { nombre: "Brunoo" });
  });

  it("si cambia la certificación, manda el objeto completo", async () => {
    obtenerProfesor.mockResolvedValueOnce(profesorMock);
    actualizarProfesor.mockResolvedValueOnce(profesorMock);
    renderPage();

    await screen.findByDisplayValue("CERT-001");
    await userEvent.type(screen.getByDisplayValue("CERT-001"), "-2");
    await userEvent.click(screen.getByRole("button", { name: "Guardar cambios" }));

    expect(actualizarProfesor).toHaveBeenCalledWith(
      "7",
      expect.objectContaining({ certificacion: expect.objectContaining({ numeroCertificacion: "CERT-001-2" }) })
    );
  });

  it("si falla la carga, muestra el estado de error", async () => {
    obtenerProfesor.mockRejectedValueOnce(new Error("404"));
    renderPage();

    expect(await screen.findByText("No se encontraron los datos del profesor.")).toBeInTheDocument();
  });
});
