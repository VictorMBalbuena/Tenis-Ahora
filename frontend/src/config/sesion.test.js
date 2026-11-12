import axios from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { restaurarSesion } from "./sesion";
import { useAuthStore } from "../store/authStore";
import { obtenerMiPerfil } from "@/features/perfil/api/perfilApi";

vi.mock("@/features/perfil/api/perfilApi", () => ({
  obtenerMiPerfil: vi.fn(),
}));

describe("restaurarSesion", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });
  it("sin sesión guardada: no pide nada al backend y deja la sesión inicializada", async () => {
    const refresh = vi.spyOn(axios, "post");

    await restaurarSesion();

    expect(refresh).not.toHaveBeenCalled();
    expect(useAuthStore.getState().inicializado).toBe(true);
    expect(useAuthStore.getState().token).toBeNull();
  });

  it("con una sesión guardada: recupera el access token con la cookie del refresh token", async () => {
    useAuthStore.setState({ user: { email: "socio@tenisahora.com", rol: "SOCIO" } });
    const refresh = vi.spyOn(axios, "post").mockResolvedValue({ data: { accessToken: "access-recuperado" } });

    await restaurarSesion();

    expect(refresh).toHaveBeenCalledWith("http://localhost:8080/api/auth/refresh", {}, { withCredentials: true });
    const estado = useAuthStore.getState();
    expect(estado.token).toBe("access-recuperado");
    expect(estado.user.rol).toBe("SOCIO");
    expect(estado.inicializado).toBe(true);
  });

  it("si la cookie venció: cierra la sesión", async () => {
    useAuthStore.setState({ user: { email: "socio@tenisahora.com", rol: "SOCIO" } });
    vi.spyOn(axios, "post").mockRejectedValue(new Error("401"));

    await restaurarSesion();

    const estado = useAuthStore.getState();
    expect(estado.token).toBeNull();
    expect(estado.user).toBeNull();
    expect(estado.inicializado).toBe(true);
  });

  it("al restaurar sesión sin nombre consulta el perfil para completar los datos", async () => {
    useAuthStore.setState({ user: { email: "mariano@tenisahora.com", rol: "ENTRENADOR" } });
    vi.spyOn(axios, "post").mockResolvedValue({ data: { accessToken: "access-recuperado" } });
    obtenerMiPerfil.mockResolvedValueOnce({
      nombre: "Mariano",
      apellido: "Zabaleta",
      fotoUrl: "https://foto.jpg",
    });

    await restaurarSesion();

    const estado = useAuthStore.getState();
    expect(estado.user.nombre).toBe("Mariano");
    expect(estado.user.apellido).toBe("Zabaleta");
    expect(estado.user.fotoUrl).toBe("https://foto.jpg");
  });
});
