import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { asignarInstructor, listarInstructoresDisponibles } from "@/features/actividades/api/asignacionInstructorApi";
import { AsignarInstructorDialog } from "../AsignarInstructorDialog";

vi.mock("@/features/actividades/api/asignacionInstructorApi", () => ({
  listarInstructoresDisponibles: vi.fn(),
  asignarInstructor: vi.fn(),
}));

const particular = {
  id: 1,
  modalidad: "PARTICULAR",
  profesorId: 8,
  profesor: "Gómez, Ana",
};

const grupal = { ...particular, id: 2, modalidad: "GRUPAL" };

function renderDialog(clase, props = {}) {
  return render(
    <AsignarInstructorDialog abierto clase={clase} onCerrar={() => {}} onAsignado={() => {}} {...props} />,
  );
}

describe("AsignarInstructorDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    listarInstructoresDisponibles.mockResolvedValue([
      { id: 8, nombre: "Gómez, Ana", rol: "PROFESOR" },
      { id: 12, nombre: "Díaz, Pablo", rol: "PROFESOR" },
    ]);
  });

  it("dice quién la dicta hoy", async () => {
    renderDialog(particular);

    expect(await screen.findByText(/Hoy la dicta Gómez, Ana/)).toBeInTheDocument();
  });

  it("solo ofrece instructores disponibles para esa clase (RD02, RD03)", async () => {
    renderDialog(particular);

    await waitFor(() => expect(listarInstructoresDisponibles).toHaveBeenCalledWith(1));
  });

  it("asigna el instructor elegido (CU44)", async () => {
    asignarInstructor.mockResolvedValueOnce();
    const onAsignado = vi.fn();

    renderDialog(particular, { onAsignado });

    await userEvent.click(await screen.findByText("Elegí el instructor"));
    await userEvent.click(await screen.findByText("Díaz, Pablo"));
    await userEvent.click(screen.getByRole("button", { name: /Asignar/ }));

    await waitFor(() =>
      expect(asignarInstructor).toHaveBeenCalledWith(1, { rol: "PROFESOR", instructorId: 12 }),
    );
    expect(onAsignado).toHaveBeenCalledWith("Se asignó a Díaz, Pablo a la clase.");
  });

  it("no deja volver a asignar al profesor que ya la dicta", async () => {
    renderDialog(particular);

    await userEvent.click(await screen.findByText("Elegí el instructor"));
    await userEvent.click(await screen.findByText("Gómez, Ana"));

    expect(screen.getByRole("button", { name: /Asignar/ })).toBeDisabled();
    expect(asignarInstructor).not.toHaveBeenCalled();
  });

  it("sin instructor elegido, el botón queda deshabilitado", async () => {
    renderDialog(particular);

    await screen.findByText("Elegí el instructor");
    expect(screen.getByRole("button", { name: /Asignar/ })).toBeDisabled();
  });

  it("en una clase grupal avisa que conviene la modificación completa", async () => {
    renderDialog(grupal);

    expect(await screen.findByText(/Esta clase es grupal/)).toBeInTheDocument();
  });

  it("si el backend rechaza la asignación, lo muestra y no avisa de éxito", async () => {
    asignarInstructor.mockRejectedValueOnce({
      response: { status: 422, data: { mensaje: "El profesor ya tiene otra clase en ese horario." } },
    });
    const onAsignado = vi.fn();

    renderDialog(particular, { onAsignado });

    await userEvent.click(await screen.findByText("Elegí el instructor"));
    await userEvent.click(await screen.findByText("Díaz, Pablo"));
    await userEvent.click(screen.getByRole("button", { name: /Asignar/ }));

    expect(await screen.findByText("El profesor ya tiene otra clase en ese horario.")).toBeInTheDocument();
    expect(onAsignado).not.toHaveBeenCalled();
  });
});
