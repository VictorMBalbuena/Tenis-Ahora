import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { Tema, CLAVE } from "@/lib/tema";
import { useAuthStore } from "@/store/authStore";
import {
  cambiarMiPassword,
  cerrarTodasMisSesiones,
  guardarMisPreferencias,
  obtenerMisPreferencias,
} from "../../api/perfilApi";
import { ConfiguracionPage } from "../ConfiguracionPage";

vi.mock("../../api/perfilApi", () => ({
  cambiarMiPassword: vi.fn(),
  obtenerMisPreferencias: vi.fn(),
  guardarMisPreferencias: vi.fn(),
  cerrarTodasMisSesiones: vi.fn(),
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={["/configuracion"]}>
      <ConfiguracionPage />
    </MemoryRouter>,
  );
}

describe("ConfiguracionPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    document.documentElement.classList.remove("dark");
    useAuthStore.setState({ token: "un-token", user: { id: 12, email: "ana@correo.com", rol: "SOCIO" } });
    obtenerMisPreferencias.mockResolvedValue({
      recordatorioReservas: true,
      recordatorioClases: true,
      novedadesDelClub: false,
    });
  });

  describe("contraseña", () => {
    it("cambia la contraseña mandando la actual y la nueva", async () => {
      cambiarMiPassword.mockResolvedValueOnce(undefined);

      renderPage();

      await userEvent.type(screen.getByLabelText("Contraseña actual"), "ClaveVieja1");
      await userEvent.type(screen.getByLabelText("Contraseña nueva"), "ClaveNueva-1");
      await userEvent.type(screen.getByLabelText("Repetir la nueva"), "ClaveNueva-1");
      await userEvent.click(screen.getByRole("button", { name: /Cambiar contraseña/ }));

      await waitFor(() =>
        expect(cambiarMiPassword).toHaveBeenCalledWith({
          passwordActual: "ClaveVieja1",
          passwordNueva: "ClaveNueva-1",
        }),
      );
      expect(await screen.findByText("Se cambió tu contraseña.")).toBeInTheDocument();
      expect(screen.getByLabelText("Contraseña actual")).toHaveValue("");
    }, 30000);

    it("no manda nada si las dos nuevas no coinciden", async () => {
      renderPage();

      await userEvent.type(screen.getByLabelText("Contraseña actual"), "ClaveVieja1");
      await userEvent.type(screen.getByLabelText("Contraseña nueva"), "ClaveNueva-1");
      await userEvent.type(screen.getByLabelText("Repetir la nueva"), "otra-clave-1");
      await userEvent.click(screen.getByRole("button", { name: /Cambiar contraseña/ }));

      expect(await screen.findByText("Las dos contraseñas no coinciden.")).toBeInTheDocument();
      expect(cambiarMiPassword).not.toHaveBeenCalled();
    });

    it("no deja repetir la contraseña que ya tenía", async () => {
      renderPage();

      await userEvent.type(screen.getByLabelText("Contraseña actual"), "LaMisma-123");
      await userEvent.type(screen.getByLabelText("Contraseña nueva"), "LaMisma-123");
      await userEvent.type(screen.getByLabelText("Repetir la nueva"), "LaMisma-123");
      await userEvent.click(screen.getByRole("button", { name: /Cambiar contraseña/ }));

      expect(await screen.findByText("Tiene que ser distinta de la actual.")).toBeInTheDocument();
      expect(cambiarMiPassword).not.toHaveBeenCalled();
    });

    it("si la actual está mal, muestra lo que dice el backend", async () => {
      cambiarMiPassword.mockRejectedValueOnce({
        response: {
          status: 422,
          data: { mensaje: "La contraseña actual no es correcta.", campos: { passwordActual: "No coincide." } },
        },
      });

      renderPage();

      await userEvent.type(screen.getByLabelText("Contraseña actual"), "equivocada1");
      await userEvent.type(screen.getByLabelText("Contraseña nueva"), "ClaveNueva-1");
      await userEvent.type(screen.getByLabelText("Repetir la nueva"), "ClaveNueva-1");
      await userEvent.click(screen.getByRole("button", { name: /Cambiar contraseña/ }));

      expect(await screen.findByText("La contraseña actual no es correcta.")).toBeInTheDocument();
      expect(screen.getByText("No coincide.")).toBeInTheDocument();
    }, 30000);

    it("el ojito deja ver la contraseña escrita", async () => {
      renderPage();

      expect(screen.getByLabelText("Contraseña actual")).toHaveAttribute("type", "password");
      await userEvent.click(screen.getByLabelText("Mostrar contraseña actual"));
      expect(screen.getByLabelText("Contraseña actual")).toHaveAttribute("type", "text");
    });

    it("dice qué tan fuerte es la contraseña nueva", async () => {
      renderPage();

      await userEvent.type(screen.getByLabelText("Contraseña nueva"), "12345678");
      expect(await screen.findByText("Seguridad: Débil")).toBeInTheDocument();
    });
  });

  describe("apariencia", () => {
    it("al elegir oscuro, lo aplica y lo guarda en este dispositivo", async () => {
      renderPage();

      await userEvent.click(screen.getByRole("radio", { name: /Oscuro/ }));

      expect(document.documentElement.classList.contains("dark")).toBe(true);
      expect(localStorage.getItem(CLAVE)).toBe(Tema.OSCURO);
    });

    it("al volver a claro, saca el modo oscuro", async () => {
      renderPage();

      await userEvent.click(screen.getByRole("radio", { name: /Oscuro/ }));
      await userEvent.click(screen.getByRole("radio", { name: /Claro/ }));

      expect(document.documentElement.classList.contains("dark")).toBe(false);
      expect(localStorage.getItem(CLAVE)).toBe(Tema.CLARO);
    });

    it("arranca con el tema que estaba guardado", async () => {
      localStorage.setItem(CLAVE, Tema.OSCURO);

      renderPage();

      expect(screen.getByRole("radio", { name: /Oscuro/ })).toBeChecked();
    });

    it("no muestra avisos internos sobre el modo oscuro", () => {
      renderPage();

      expect(screen.queryByText(/El modo oscuro recién arranca/)).not.toBeInTheDocument();
    });
  });

  describe("notificaciones", () => {
    it("muestra las preferencias que trae el backend", async () => {
      renderPage();

      expect(await screen.findByText("Recordarme mis reservas de cancha")).toBeInTheDocument();
      expect(screen.getByRole("checkbox", { name: /reservas de cancha/ })).toBeChecked();
      expect(screen.getByRole("checkbox", { name: /Novedades y torneos/ })).not.toBeChecked();
    });

    it("sin cambios no deja guardar", async () => {
      renderPage();

      await screen.findByText("Recordarme mis reservas de cancha");
      expect(screen.getByRole("button", { name: /Guardar preferencias/ })).toBeDisabled();
      expect(screen.getByText("No hay cambios sin guardar.")).toBeInTheDocument();
    });

    it("guarda las tres preferencias juntas", async () => {
      guardarMisPreferencias.mockResolvedValueOnce({
        recordatorioReservas: true,
        recordatorioClases: true,
        novedadesDelClub: true,
      });

      renderPage();

      await userEvent.click(await screen.findByRole("checkbox", { name: /Novedades y torneos/ }));
      await userEvent.click(screen.getByRole("button", { name: /Guardar preferencias/ }));

      await waitFor(() =>
        expect(guardarMisPreferencias).toHaveBeenCalledWith({
          recordatorioReservas: true,
          recordatorioClases: true,
          novedadesDelClub: true,
        }),
      );
      expect(await screen.findByText("Se guardaron tus preferencias.")).toBeInTheDocument();
    });

    it("si no se pudieron cargar, lo dice", async () => {
      obtenerMisPreferencias.mockRejectedValueOnce({ response: undefined });

      renderPage();

      expect(await screen.findByText(/No se pudo conectar con el servidor/)).toBeInTheDocument();
    });
  });

  describe("sesiones", () => {
    it("pide confirmación antes de cerrar todas las sesiones", async () => {
      renderPage();

      await userEvent.click(screen.getByRole("button", { name: /Cerrar sesión en todos los dispositivos/ }));

      expect(await screen.findByText(/volver a iniciar sesión/)).toBeInTheDocument();
      expect(cerrarTodasMisSesiones).not.toHaveBeenCalled();
    });

    it("al confirmar, cierra las sesiones y limpia la de esta pestaña", async () => {
      cerrarTodasMisSesiones.mockResolvedValueOnce(undefined);

      renderPage();

      await userEvent.click(screen.getByRole("button", { name: /Cerrar sesión en todos los dispositivos/ }));
      await userEvent.click(await screen.findByRole("button", { name: "Cerrar todas" }));

      await waitFor(() => expect(cerrarTodasMisSesiones).toHaveBeenCalled());
      expect(useAuthStore.getState().token).toBeNull();
    });

    it("si falla, lo muestra y no cierra la sesión local", async () => {
      cerrarTodasMisSesiones.mockRejectedValueOnce({ response: { status: 500 } });

      renderPage();

      await userEvent.click(screen.getByRole("button", { name: /Cerrar sesión en todos los dispositivos/ }));
      await userEvent.click(await screen.findByRole("button", { name: "Cerrar todas" }));

      expect(await screen.findByText(/No se pudieron cerrar las sesiones|Ocurrió un error/)).toBeInTheDocument();
      expect(useAuthStore.getState().token).toBe("un-token");
    });
  });
});
