import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { jwtDecode } from "jwt-decode";
import { InscribirTorneoDialog } from "../InscribirTorneoDialog";
import * as torneosApi from "../../api/torneosApi";
import { useAuthStore } from "@/store/authStore";

vi.mock("../../api/torneosApi", () => ({
  inscribirTorneo: vi.fn(),
  buscarCompaneroPorDni: vi.fn(),
}));

vi.mock("jwt-decode", () => ({
  jwtDecode: vi.fn(() => ({ sub: "42", rol: "SOCIO" })),
}));

/** Quien está usando la pantalla. El id y el rol salen del token, como en la aplicación. */
function sesionDe(sub, rol) {
  jwtDecode.mockReturnValue({ sub, rol });
  useAuthStore.setState({ token: "mock-jwt-token", user: { rol } });
}

describe("InscribirTorneoDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sesionDe("42", "SOCIO");
  });

  const torneoSingle = {
    id: 1,
    nombre: "Torneo Primavera Singles",
    categoria: "MASCULINO",
    modalidad: "SINGLE",
    costoInscripcion: 5000,
  };

  const torneoDobles = {
    id: 2,
    nombre: "Torneo Dobles",
    categoria: "FEMENINO",
    modalidad: "PAREJA",
    costoInscripcion: 8000,
  };

  function abrir(torneo, onInscripto = vi.fn()) {
    render(
      <InscribirTorneoDialog
        open={true}
        torneo={torneo}
        onClose={vi.fn()}
        onInscripto={onInscripto}
      />
    );
    return onInscripto;
  }

  /** Escribe un DNI en ese campo y toca su botón "Buscar". */
  async function buscarPorDni(campo, dni, socio) {
    torneosApi.buscarCompaneroPorDni.mockResolvedValueOnce(socio);
    const input = screen.getByLabelText(campo);
    fireEvent.change(input, { target: { value: dni } });
    fireEvent.click(input.parentElement.querySelector("button"));
    await screen.findByText(`${socio.nombre} ${socio.apellido}`);
  }

  const confirmar = () =>
    fireEvent.click(screen.getByRole("button", { name: /confirmar inscripción/i }));

  it("renderiza datos del torneo y arancel", () => {
    abrir(torneoSingle);

    expect(screen.getByText("Torneo Primavera Singles")).toBeInTheDocument();
    expect(screen.getByText(/\$5\.?000/i)).toBeInTheDocument();
  });

  it("el socio se inscribe a sí mismo: el jugador 1 sale de su token", async () => {
    torneosApi.inscribirTorneo.mockResolvedValueOnce({ id: 10, torneoId: 1, jugador1Id: 42 });
    const onInscripto = abrir(torneoSingle);

    // Al socio no se le pregunta quién se inscribe: es él.
    expect(screen.queryByLabelText(/primer jugador|DNI del socio/i)).not.toBeInTheDocument();
    expect(screen.getByText(/singles no requiere compañero/i)).toBeInTheDocument();

    confirmar();

    await waitFor(() => {
      expect(torneosApi.inscribirTorneo).toHaveBeenCalledWith(1, expect.objectContaining({
        jugador1Id: 42,
        parejaId: null,
      }));
      expect(onInscripto).toHaveBeenCalled();
    });
  });

  it("en modalidad PAREJA hay que buscar al compañero antes de confirmar", async () => {
    const onInscripto = abrir(torneoDobles);

    confirmar();

    expect(torneosApi.inscribirTorneo).not.toHaveBeenCalled();
    expect(await screen.findByRole("alert")).toHaveTextContent(/buscá por dni al compañero/i);
    expect(onInscripto).not.toHaveBeenCalled();
  });

  it("la pareja se manda con el id que devolvió la búsqueda, no con lo que se tipeó", async () => {
    torneosApi.inscribirTorneo.mockResolvedValueOnce({ id: 11, torneoId: 2 });
    const onInscripto = abrir(torneoDobles);

    // Se escribe un DNI; el id del socio lo resuelve el backend y nunca se le pide a nadie.
    await buscarPorDni(/compañero/i, "30111222", { id: 99, nombre: "Ana", apellido: "Paz" });
    expect(torneosApi.buscarCompaneroPorDni).toHaveBeenCalledWith("30111222");

    confirmar();

    await waitFor(() => {
      expect(torneosApi.inscribirTorneo).toHaveBeenCalledWith(2, expect.objectContaining({
        jugador1Id: 42,
        parejaId: 99,
      }));
      expect(onInscripto).toHaveBeenCalled();
    });
  });

  it("un DNI que no es de ningún socio muestra el motivo y no inscribe", async () => {
    // Lo que contesta la API: el formato único de error (DIS 06).
    torneosApi.buscarCompaneroPorDni.mockRejectedValueOnce({
      response: { status: 404, data: { codigo: "PERSONA_NO_ENCONTRADA", mensaje: "No hay un socio activo con ese DNI" } },
    });
    abrir(torneoDobles);

    const input = screen.getByLabelText(/compañero/i);
    fireEvent.change(input, { target: { value: "30111222" } });
    fireEvent.click(input.parentElement.querySelector("button"));

    expect(await screen.findByRole("alert")).toHaveTextContent(/no hay un socio activo con ese dni/i);
    expect(torneosApi.inscribirTorneo).not.toHaveBeenCalled();
  });

  it("el empleado inscribe socios: se le piden los dos DNI", async () => {
    sesionDe("2", "EMPLEADO");
    torneosApi.inscribirTorneo.mockResolvedValueOnce({ id: 12, torneoId: 2 });
    const onInscripto = abrir(torneoDobles);

    // El empleado no es socio: su propio id no sirve como jugador 1.
    await buscarPorDni(/primer jugador/i, "20000001", { id: 20, nombre: "Lucas", apellido: "Fernández" });
    await buscarPorDni(/segundo jugador/i, "20000002", { id: 21, nombre: "Martín", apellido: "Gómez" });

    confirmar();

    await waitFor(() => {
      expect(torneosApi.inscribirTorneo).toHaveBeenCalledWith(2, expect.objectContaining({
        jugador1Id: 20,
        parejaId: 21,
      }));
      expect(onInscripto).toHaveBeenCalled();
    });
  });

  it("el empleado no puede inscribir a un socio como compañero de sí mismo", async () => {
    sesionDe("2", "EMPLEADO");
    abrir(torneoDobles);

    const mismo = { id: 20, nombre: "Lucas", apellido: "Fernández" };
    torneosApi.buscarCompaneroPorDni.mockResolvedValue(mismo);

    const primero = screen.getByLabelText(/primer jugador/i);
    fireEvent.change(primero, { target: { value: "20000001" } });
    fireEvent.click(primero.parentElement.querySelector("button"));
    await screen.findByText("Lucas Fernández");

    const segundo = screen.getByLabelText(/segundo jugador/i);
    fireEvent.change(segundo, { target: { value: "20000001" } });
    fireEvent.click(segundo.parentElement.querySelector("button"));
    await waitFor(() => expect(screen.getAllByText("Lucas Fernández")).toHaveLength(2));

    confirmar();

    expect(await screen.findByRole("alert")).toHaveTextContent(/tiene que ser otro socio/i);
    expect(torneosApi.inscribirTorneo).not.toHaveBeenCalled();
  });

  it("sin buscar al titular, el empleado no puede confirmar", async () => {
    sesionDe("2", "EMPLEADO");
    abrir(torneoSingle);

    confirmar();

    expect(await screen.findByRole("alert")).toHaveTextContent(/buscá por dni al socio que se inscribe/i);
    expect(torneosApi.inscribirTorneo).not.toHaveBeenCalled();
  });
});
