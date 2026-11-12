import { describe, expect, it } from "vitest";
import { useAuthStore } from "./authStore";

describe("authStore", () => {
  it("setLogin guarda el access token y el usuario, y marca la sesión como inicializada", () => {
    useAuthStore.getState().setLogin("access", { email: "socio@tenisahora.com", rol: "SOCIO" });

    const estado = useAuthStore.getState();
    expect(estado.token).toBe("access");
    expect(estado.user).toEqual({ email: "socio@tenisahora.com", rol: "SOCIO" });
    expect(estado.inicializado).toBe(true);
  });

  it("no tiene lugar para un refresh token: ese va en una cookie httpOnly", () => {
    useAuthStore.getState().setLogin("access", { email: "socio@tenisahora.com", rol: "SOCIO" });

    expect(useAuthStore.getState()).not.toHaveProperty("refreshToken");
  });

  it("setToken reemplaza solo el access token", () => {
    useAuthStore.getState().setLogin("access", { email: "socio@tenisahora.com", rol: "SOCIO" });

    useAuthStore.getState().setToken("access-nuevo");

    const estado = useAuthStore.getState();
    expect(estado.token).toBe("access-nuevo");
    expect(estado.user.rol).toBe("SOCIO");
  });

  it("logout borra el token y el usuario", () => {
    useAuthStore.getState().setLogin("access", { email: "socio@tenisahora.com", rol: "SOCIO" });

    useAuthStore.getState().logout();

    const estado = useAuthStore.getState();
    expect(estado.token).toBeNull();
    expect(estado.user).toBeNull();
    expect(estado.inicializado).toBe(true);
  });

  it("en el localStorage solo queda el usuario: ningún token", () => {
    useAuthStore.getState().setLogin("access-secreto", { email: "socio@tenisahora.com", rol: "SOCIO" });

    const crudo = localStorage.getItem("auth-storage");
    const guardado = JSON.parse(crudo);
    expect(guardado.state).toEqual({ user: { email: "socio@tenisahora.com", rol: "SOCIO" } });
    expect(crudo).not.toContain("access-secreto");
    expect(crudo).not.toContain("token");
  });

  it("descarta los tokens que dejó una versión anterior en el localStorage", async () => {
    localStorage.setItem(
      "auth-storage",
      JSON.stringify({
        state: { token: "viejo", refreshToken: "refresh-viejo", user: { email: "socio@tenisahora.com", rol: "SOCIO" } },
        version: 0,
      })
    );

    await useAuthStore.persist.rehydrate();

    const estado = useAuthStore.getState();
    expect(estado.token).toBeNull();
    expect(estado).not.toHaveProperty("refreshToken");
    expect(estado.user).toEqual({ email: "socio@tenisahora.com", rol: "SOCIO" });
  });
});
