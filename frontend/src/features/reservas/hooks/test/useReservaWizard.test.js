import { renderHook, act } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import { useReservaWizard, DRAFT_INICIAL } from "../useReservaWizard";

describe("useReservaWizard", () => {
  it("inicializa con valores por defecto en paso 1 y modal cerrado", () => {
    const { result } = renderHook(() => useReservaWizard());

    expect(result.current.pasoActual).toBe(1);
    expect(result.current.draft).toEqual(DRAFT_INICIAL);
    expect(result.current.reservaCreada).toBeNull();
    expect(result.current.modalExitoOpen).toBe(false);
  });

  it("permite inicializar con borrador y paso personalizados", () => {
    const customDraft = {
      fecha: "2026-10-30",
      duracionHoras: 2,
      canchaId: 3,
    };
    const { result } = renderHook(() => useReservaWizard(customDraft, 2));

    expect(result.current.pasoActual).toBe(2);
    expect(result.current.draft.fecha).toBe("2026-10-30");
    expect(result.current.draft.canchaId).toBe(3);
    expect(result.current.draft.duracionHoras).toBe(2);
  });

  it("avanzarPaso actualiza el draft y suma 1 al paso actual hasta el límite de 4", () => {
    const { result } = renderHook(() => useReservaWizard());

    act(() => {
      result.current.avanzarPaso({ fecha: "2026-10-25", canchaId: 1 });
    });

    expect(result.current.pasoActual).toBe(2);
    expect(result.current.draft.fecha).toBe("2026-10-25");
    expect(result.current.draft.canchaId).toBe(1);

    act(() => {
      result.current.avanzarPaso({ modalidad: "DOBLES" });
    });
    expect(result.current.pasoActual).toBe(3);
    expect(result.current.draft.modalidad).toBe("DOBLES");

    act(() => {
      result.current.avanzarPaso({ insumos: [{ insumoId: 10, cantidad: 2 }] });
    });
    expect(result.current.pasoActual).toBe(4);

    // No debe superar el paso 4
    act(() => {
      result.current.avanzarPaso();
    });
    expect(result.current.pasoActual).toBe(4);
  });

  it("retrocederPaso resta 1 al paso actual sin bajar de 1", () => {
    const { result } = renderHook(() => useReservaWizard({}, 3));

    expect(result.current.pasoActual).toBe(3);

    act(() => {
      result.current.retrocederPaso();
    });
    expect(result.current.pasoActual).toBe(2);

    act(() => {
      result.current.retrocederPaso();
    });
    expect(result.current.pasoActual).toBe(1);

    // No baja de 1
    act(() => {
      result.current.retrocederPaso();
    });
    expect(result.current.pasoActual).toBe(1);
  });

  it("irAPaso salta al paso indicado si está entre 1 y 4", () => {
    const { result } = renderHook(() => useReservaWizard());

    act(() => {
      result.current.irAPaso(3);
    });
    expect(result.current.pasoActual).toBe(3);

    // Ignora valores inválidos
    act(() => {
      result.current.irAPaso(5);
    });
    expect(result.current.pasoActual).toBe(3);

    act(() => {
      result.current.irAPaso(0);
    });
    expect(result.current.pasoActual).toBe(3);
  });

  it("reiniciar restablece el draft y estado inicial", () => {
    const { result } = renderHook(() =>
      useReservaWizard({ fecha: "2026-10-28", canchaId: 5 }, 3)
    );

    act(() => {
      result.current.reiniciar();
    });

    expect(result.current.pasoActual).toBe(1);
    expect(result.current.draft).toEqual(DRAFT_INICIAL);
    expect(result.current.reservaCreada).toBeNull();
    expect(result.current.modalExitoOpen).toBe(false);
  });

  it("abrirExito guarda la reserva y abre el modal, cerrarExito lo cierra", () => {
    const { result } = renderHook(() => useReservaWizard());
    const reserva = { id: 99, numero: "R-000099" };

    act(() => {
      result.current.abrirExito(reserva);
    });

    expect(result.current.reservaCreada).toEqual(reserva);
    expect(result.current.modalExitoOpen).toBe(true);

    act(() => {
      result.current.cerrarExito();
    });

    expect(result.current.modalExitoOpen).toBe(false);
    expect(result.current.reservaCreada).toEqual(reserva);
  });
});
