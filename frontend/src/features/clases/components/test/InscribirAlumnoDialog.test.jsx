import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { listarUsuarios } from "@/features/usuarios/api/usuariosApi";
import { inscribirAlumno } from "../../api/clasesApi";
import { InscribirAlumnoDialog } from "../InscribirAlumnoDialog";

vi.mock("@/features/usuarios/api/usuariosApi", () => ({
  listarUsuarios: vi.fn(),
}));

vi.mock("../../api/clasesApi", () => ({
  inscribirAlumno: vi.fn(),
}));

const socio = { id: 20, nombre: "Juan", apellido: "López", email: "juan@correo.com", dni: "30123456" };

const baseClase = { nombre: "Tenis inicial", fechaInicio: "2099-11-02", fechaFin: "2100-03-02", estado: "PROGRAMADA" };
const conLugar = { ...baseClase, id: 1, cupoMaximo: 12, cupoOcupado: 4, cupoDisponible: 8 };
const completa = { ...baseClase, id: 2, cupoMaximo: 10, cupoOcupado: 10, cupoDisponible: 0 };
const pasada = { ...baseClase, id: 3, estado: "FINALIZADA", cupoMaximo: 10, cupoOcupado: 2, cupoDisponible: 8 };

function renderDialog(clase, props = {}) {
  return render(
    <InscribirAlumnoDialog abierto clase={clase} onCerrar={() => {}} onInscripto={() => {}} {...props} />,
  );
}

describe("InscribirAlumnoDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    listarUsuarios.mockResolvedValue({ contenido: [socio] });
  });

  it("muestra los lugares ocupados y los que quedan", async () => {
    renderDialog(conLugar);

    expect(await screen.findByText(/4 de 12 lugares/)).toBeInTheDocument();
    expect(screen.getByText("Quedan 8 lugares.")).toBeInTheDocument();
  });

  it("inscribe al socio elegido (CU81)", async () => {
    inscribirAlumno.mockResolvedValueOnce({ id: 99 });
    const onInscripto = vi.fn();

    renderDialog(conLugar, { onInscripto });

    await userEvent.click(await screen.findByText("Juan López"));
    await userEvent.click(screen.getByRole("button", { name: /Inscribir/ }));

    await waitFor(() => expect(inscribirAlumno).toHaveBeenCalledWith(1, 20));
    expect(onInscripto).toHaveBeenCalledWith("Se inscribió a Juan López a la clase.");
  });

  it("sin socio elegido, el botón de inscribir queda deshabilitado", async () => {
    renderDialog(conLugar);

    await screen.findByText("Juan López");
    expect(screen.getByRole("button", { name: /Inscribir/ })).toBeDisabled();
  });

  it("con el cupo completo avisa y no deja elegir a nadie (RD01, RD04)", async () => {
    renderDialog(completa);

    expect(await screen.findByText(/El cupo está completo/)).toBeInTheDocument();
    expect(screen.queryByLabelText("Buscar socio")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Inscribir/ })).toBeDisabled();
    expect(inscribirAlumno).not.toHaveBeenCalled();
  });

  it("si la clase ya pasó, tampoco admite inscripciones", async () => {
    renderDialog(pasada);

    expect(await screen.findByText(/La clase ya pasó/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Inscribir/ })).toBeDisabled();
  });

  it("busca por DNI cuando lo que se escribe son números", async () => {
    renderDialog(conLugar);

    await userEvent.type(await screen.findByLabelText("Buscar socio"), "30123456");

    await waitFor(() => expect(listarUsuarios).toHaveBeenLastCalledWith("SOCIO", { dni: "30123456", size: 8 }));
  });

  it("busca por nombre cuando lo que se escribe es texto", async () => {
    renderDialog(conLugar);

    await userEvent.type(await screen.findByLabelText("Buscar socio"), "López");

    await waitFor(() => expect(listarUsuarios).toHaveBeenLastCalledWith("SOCIO", { nombre: "López", size: 8 }));
  });

  it("si el backend rechaza la inscripción, lo muestra y no avisa de éxito", async () => {
    inscribirAlumno.mockRejectedValueOnce({
      response: { status: 422, data: { mensaje: "La clase se completó mientras cargabas." } },
    });
    const onInscripto = vi.fn();

    renderDialog(conLugar, { onInscripto });

    await userEvent.click(await screen.findByText("Juan López"));
    await userEvent.click(screen.getByRole("button", { name: /Inscribir/ }));

    expect(await screen.findByText("La clase se completó mientras cargabas.")).toBeInTheDocument();
    expect(onInscripto).not.toHaveBeenCalled();
  });
});
