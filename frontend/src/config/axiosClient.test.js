import axios from "axios";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axiosClient, { renovarAccessToken } from "./axiosClient";
import { useAuthStore } from "../store/authStore";

// En vez de pegarle al backend, cada pedido pasa por este adapter falso
const adapterOriginal = axiosClient.defaults.adapter;
let adapter;

const ok = (config, data = {}) => Promise.resolve({ data, status: 200, statusText: "OK", headers: {}, config });
const error401 = (config) =>
  Promise.reject(Object.assign(new Error("401"), { config, response: { status: 401, data: {}, headers: {}, config } }));

beforeEach(() => {
  adapter = vi.fn();
  axiosClient.defaults.adapter = adapter;
});

afterEach(() => {
  axiosClient.defaults.adapter = adapterOriginal;
});

describe("axiosClient", () => {
  it("apunta al backend en /api", () => {
    expect(axiosClient.defaults.baseURL).toBe("http://localhost:8080/api");
  });

  it("manda y recibe la cookie del refresh token (withCredentials)", () => {
    expect(axiosClient.defaults.withCredentials).toBe(true);
  });

  it("agrega el token en el encabezado Authorization", async () => {
    useAuthStore.setState({ token: "access" });
    adapter.mockImplementation((config) => ok(config));

    await axiosClient.get("/canchas");

    expect(adapter.mock.calls[0][0].headers.Authorization).toBe("Bearer access");
  });

  it("sin sesión no manda Authorization", async () => {
    adapter.mockImplementation((config) => ok(config));

    await axiosClient.get("/canchas");

    expect(adapter.mock.calls[0][0].headers.Authorization).toBeUndefined();
  });

  it("ante un 401 renueva el token con la cookie (sin mandar ningún token) y reintenta el pedido", async () => {
    useAuthStore.setState({ token: "vencido", user: { rol: "SOCIO" } });
    const refresh = vi.spyOn(axios, "post").mockResolvedValue({ data: { accessToken: "access-nuevo" } });
    adapter
      .mockImplementationOnce((config) => error401(config))
      .mockImplementationOnce((config) => ok(config, { canchas: [] }));

    const respuesta = await axiosClient.get("/canchas");

    expect(refresh).toHaveBeenCalledWith("http://localhost:8080/api/auth/refresh", {}, { withCredentials: true });
    expect(useAuthStore.getState().token).toBe("access-nuevo");
    expect(adapter.mock.calls[1][0].headers.Authorization).toBe("Bearer access-nuevo");
    expect(respuesta.data).toEqual({ canchas: [] });
  });

  it("si varios pedidos fallan con 401 a la vez, se renueva una sola vez", async () => {
    useAuthStore.setState({ token: "vencido", user: { rol: "SOCIO" } });
    let resolverRenovacion;
    const refresh = vi.spyOn(axios, "post").mockReturnValue(
      new Promise((resolve) => {
        resolverRenovacion = () => resolve({ data: { accessToken: "access-nuevo" } });
      })
    );
    adapter.mockImplementation((config) =>
      config.headers.Authorization === "Bearer access-nuevo" ? ok(config) : error401(config)
    );

    const pedidos = Promise.all([axiosClient.get("/canchas"), axiosClient.get("/reservas"), axiosClient.get("/insumos")]);
    await vi.waitFor(() => expect(refresh).toHaveBeenCalledTimes(1));
    resolverRenovacion();
    await pedidos;

    expect(refresh).toHaveBeenCalledTimes(1);
    expect(useAuthStore.getState().token).toBe("access-nuevo");
  });

  it("si la renovación falla, cierra la sesión", async () => {
    useAuthStore.setState({ token: "vencido", user: { rol: "SOCIO" } });
    vi.spyOn(axios, "post").mockRejectedValue(new Error("cookie vencida"));
    adapter.mockImplementation((config) => error401(config));

    await expect(axiosClient.get("/canchas")).rejects.toThrow("cookie vencida");

    expect(useAuthStore.getState().token).toBeNull();
    expect(useAuthStore.getState().user).toBeNull();
  });

  it("si el reintento vuelve a dar 401, no entra en un bucle", async () => {
    useAuthStore.setState({ token: "vencido", user: { rol: "SOCIO" } });
    const refresh = vi.spyOn(axios, "post").mockResolvedValue({ data: { accessToken: "access-nuevo" } });
    adapter.mockImplementation((config) => error401(config));

    await expect(axiosClient.get("/canchas")).rejects.toThrow();

    expect(refresh).toHaveBeenCalledTimes(1);
    expect(adapter).toHaveBeenCalledTimes(2);
  });

  it.each(["/auth/login", "/auth/register", "/auth/refresh", "/auth/logout"])(
    "un 401 en %s no intenta renovar ni cierra la sesión",
    async (ruta) => {
      useAuthStore.setState({ token: "access", user: { rol: "SOCIO" } });
      const refresh = vi.spyOn(axios, "post");
      adapter.mockImplementation((config) => error401(config));

      await expect(axiosClient.post(ruta, {})).rejects.toThrow();

      expect(refresh).not.toHaveBeenCalled();
      expect(useAuthStore.getState().token).toBe("access");
    }
  );

  it("un error que no es 401 se devuelve sin renovar el token", async () => {
    useAuthStore.setState({ token: "access" });
    const refresh = vi.spyOn(axios, "post");
    adapter.mockImplementation((config) =>
      Promise.reject(Object.assign(new Error("500"), { config, response: { status: 500, config } })));

    await expect(axiosClient.get("/canchas")).rejects.toThrow("500");

    expect(refresh).not.toHaveBeenCalled();
    expect(useAuthStore.getState().token).toBe("access");
  });
});

describe("renovarAccessToken", () => {
  it("guarda el access token nuevo y lo devuelve", async () => {
    vi.spyOn(axios, "post").mockResolvedValue({ data: { accessToken: "access-nuevo" } });

    const token = await renovarAccessToken();

    expect(token).toBe("access-nuevo");
    expect(useAuthStore.getState().token).toBe("access-nuevo");
  });

  it("después de terminar, una renovación nueva vuelve a pedirse", async () => {
    const refresh = vi.spyOn(axios, "post").mockResolvedValue({ data: { accessToken: "a" } });

    await renovarAccessToken();
    await renovarAccessToken();

    expect(refresh).toHaveBeenCalledTimes(2);
  });
});
