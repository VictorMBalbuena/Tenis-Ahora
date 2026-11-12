import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { PasoJugadores } from "../PasoJugadores";
import axiosClient from "@/config/axiosClient";
import { useAuthStore } from "@/store/authStore";

vi.mock("@/config/axiosClient", () => ({
  default: {
    get: vi.fn(),
  },
}));

describe("PasoJugadores", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: null });
  });
  it("permite alternar entre Single (1 acompañante) y Dobles (3 acompañantes)", () => {
    render(
      <PasoJugadores
        valor={{ modalidad: "SINGLE", acompaniantes: [{ nombre: "", apellido: "" }] }}
        onVolver={vi.fn()}
        onSiguiente={vi.fn()}
      />
    );
    expect(screen.getAllByPlaceholderText(/apellido/i)).toHaveLength(1);

    const btnDobles = screen.getByRole("button", { name: /dobles/i });
    fireEvent.click(btnDobles);
    expect(screen.getAllByPlaceholderText(/apellido/i)).toHaveLength(3);
  });

  it("bloquea el botón continuar si faltan datos de los invitados", () => {
    const onSiguiente = vi.fn();
    render(
      <PasoJugadores
        valor={{ modalidad: "SINGLE", acompaniantes: [{ nombre: "", apellido: "" }] }}
        onVolver={vi.fn()}
        onSiguiente={onSiguiente}
      />
    );
    const btnContinuar = screen.getByRole("button", { name: /continuar/i });
    fireEvent.click(btnContinuar);
    expect(onSiguiente).not.toHaveBeenCalled();
    expect(screen.getByText(/completá nombre y apellido/i)).toBeInTheDocument();
  });

  it("permite continuar cuando los datos de los acompañantes son válidos", () => {
    const onSiguiente = vi.fn();
    render(
      <PasoJugadores
        valor={{ modalidad: "SINGLE", acompaniantes: [] }}
        onVolver={vi.fn()}
        onSiguiente={onSiguiente}
      />
    );

    const inputNombre = screen.getByPlaceholderText(/nombre/i);
    const inputApellido = screen.getByPlaceholderText(/apellido/i);
    fireEvent.change(inputNombre, { target: { value: "Martín" } });
    fireEvent.change(inputApellido, { target: { value: "Jaite" } });

    const btnContinuar = screen.getByRole("button", { name: /continuar/i });
    fireEvent.click(btnContinuar);

    expect(onSiguiente).toHaveBeenCalledTimes(1);
    expect(onSiguiente).toHaveBeenCalledWith({
      modalidad: "SINGLE",
      acompaniantes: [
        expect.objectContaining({
          nombre: "Martín",
          apellido: "Jaite",
        }),
      ],
    });
  });

  it("ejecuta onVolver al hacer clic en el botón volver", () => {
    const onVolver = vi.fn();
    render(
      <PasoJugadores
        valor={{ modalidad: "SINGLE" }}
        onVolver={onVolver}
        onSiguiente={vi.fn()}
      />
    );

    const btnVolver = screen.getByRole("button", { name: /volver/i });
    fireEvent.click(btnVolver);
    expect(onVolver).toHaveBeenCalledTimes(1);
  });

  it("conserva datos ya cargados al cambiar entre Single y Dobles", () => {
    render(
      <PasoJugadores
        valor={{
          modalidad: "SINGLE",
          acompaniantes: [{ nombre: "Gabriela", apellido: "Sabatini", documento: "123" }],
        }}
        onVolver={vi.fn()}
        onSiguiente={vi.fn()}
      />
    );

    expect(screen.getByDisplayValue("Gabriela")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Sabatini")).toBeInTheDocument();

    const btnDobles = screen.getByRole("button", { name: /dobles/i });
    fireEvent.click(btnDobles);

    expect(screen.getByDisplayValue("Gabriela")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Sabatini")).toBeInTheDocument();
    expect(screen.getAllByPlaceholderText(/apellido/i)).toHaveLength(3);
  });

  it("inicializa con 3 acompañantes si el draft inicial indica modalidad DOBLES", () => {
    render(
      <PasoJugadores
        valor={{ modalidad: "DOBLES" }}
        onVolver={vi.fn()}
        onSiguiente={vi.fn()}
      />
    );

    expect(screen.getAllByPlaceholderText(/apellido/i)).toHaveLength(3);
  });

  it("limpia el mensaje de error cuando el usuario edita los campos", () => {
    render(
      <PasoJugadores
        valor={{ modalidad: "SINGLE", acompaniantes: [{ nombre: "", apellido: "" }] }}
        onVolver={vi.fn()}
        onSiguiente={vi.fn()}
      />
    );

    const btnContinuar = screen.getByRole("button", { name: /continuar/i });
    fireEvent.click(btnContinuar);
    expect(screen.getByText(/completá nombre y apellido/i)).toBeInTheDocument();

    const inputNombre = screen.getByPlaceholderText(/nombre/i);
    fireEvent.change(inputNombre, { target: { value: "David" } });

    expect(screen.queryByText(/completá nombre y apellido/i)).not.toBeInTheDocument();
  });

  it("exige buscar por DNI al acompañante socio y no asigna dummies", async () => {
    axiosClient.get.mockResolvedValueOnce({ data: { id: 15, nombre: "Ana", apellido: "Paz" } });
    const onSiguiente = vi.fn();
    render(
      <PasoJugadores
        valor={{ modalidad: "SINGLE", acompaniantes: [] }}
        onVolver={vi.fn()}
        onSiguiente={onSiguiente}
      />
    );

    fireEvent.click(screen.getByRole("button", { name: /socio del club/i }));

    const btnContinuar = screen.getByRole("button", { name: /continuar/i });
    fireEvent.click(btnContinuar);

    expect(onSiguiente).not.toHaveBeenCalled();
    expect(screen.getByText(/buscá por dni a cada acompañante socio/i)).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText(/dni sin puntos/i), { target: { value: "40111222" } });
    fireEvent.click(screen.getByRole("button", { name: /^buscar$/i }));

    await screen.findByText(/ana paz quedó como acompañante/i);
    expect(axiosClient.get).toHaveBeenCalledWith("/reservas/jugadores", { params: { dni: "40111222" } });

    fireEvent.click(btnContinuar);

    expect(onSiguiente).toHaveBeenCalledWith(
      expect.objectContaining({
        acompaniantes: [expect.objectContaining({ personaId: 15, tipo: "SOCIO" })],
      })
    );
  });

  describe("Personal del club (EMPLEADO / ADMINISTRADOR) - DNI titular", () => {
    it("si es personal, exige verificar el DNI del socio titular antes de continuar", async () => {
      useAuthStore.setState({ user: { rol: "EMPLEADO" } });
      const onSiguiente = vi.fn();

      render(
        <PasoJugadores
          valor={{
            modalidad: "SINGLE",
            acompaniantes: [{ nombre: "Carlos", apellido: "Tevez" }],
          }}
          onVolver={vi.fn()}
          onSiguiente={onSiguiente}
        />
      );

      const inputDni = screen.getByPlaceholderText(/dni sin puntos \(ej/i);
      expect(inputDni).toBeInTheDocument();

      const btnContinuar = screen.getByRole("button", { name: /continuar/i });
      fireEvent.click(btnContinuar);

      expect(onSiguiente).not.toHaveBeenCalled();
      expect(
        screen.getByText(/indicá y verificá el dni de un socio titular activo/i)
      ).toBeInTheDocument();
    });

    it("verifica el DNI exitosamente y permite avanzar con titular resuelto", async () => {
      useAuthStore.setState({ user: { rol: "EMPLEADO" } });
      axiosClient.get.mockResolvedValueOnce({
        data: {
          contenido: [
            {
              id: 3,
              nombre: "Martín",
              apellido: "Jaite",
              dni: "30123456",
              activo: true,
            },
          ],
        },
      });

      const onSiguiente = vi.fn();
      render(
        <PasoJugadores
          valor={{
            modalidad: "SINGLE",
            acompaniantes: [{ nombre: "Carlos", apellido: "Tevez" }],
          }}
          onVolver={vi.fn()}
          onSiguiente={onSiguiente}
        />
      );

      const inputDni = screen.getByPlaceholderText(/dni sin puntos \(ej/i);
      fireEvent.change(inputDni, { target: { value: "30123456" } });

      const btnVerificar = screen.getByRole("button", { name: /verificar/i });
      fireEvent.click(btnVerificar);

      await waitFor(() => {
        expect(screen.getByText(/socio activo/i)).toBeInTheDocument();
        expect(screen.getByText(/Martín Jaite/i)).toBeInTheDocument();
      });

      const btnContinuar = screen.getByRole("button", { name: /continuar/i });
      fireEvent.click(btnContinuar);

      expect(onSiguiente).toHaveBeenCalledWith(
        expect.objectContaining({
          titularId: 3,
          titularNombre: "Martín Jaite",
          titularDni: "30123456",
        })
      );
    });

    it("muestra error si el DNI no existe en el sistema e impide avanzar", async () => {
      useAuthStore.setState({ user: { rol: "EMPLEADO" } });
      axiosClient.get.mockResolvedValueOnce({
        data: { contenido: [] },
      });

      render(
        <PasoJugadores
          valor={{
            modalidad: "SINGLE",
            acompaniantes: [{ nombre: "Carlos", apellido: "Tevez" }],
          }}
          onVolver={vi.fn()}
          onSiguiente={vi.fn()}
        />
      );

      const inputDni = screen.getByPlaceholderText(/dni sin puntos \(ej/i);
      fireEvent.change(inputDni, { target: { value: "99999999" } });

      const btnVerificar = screen.getByRole("button", { name: /verificar/i });
      fireEvent.click(btnVerificar);

      await waitFor(() => {
        expect(
          screen.getByText(/no se encontró ningún socio registrado con el dni ingresado/i)
        ).toBeInTheDocument();
      });
    });
  });
});
