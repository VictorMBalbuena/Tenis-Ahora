import "@testing-library/jest-dom/vitest";
import { afterEach, vi } from "vitest";
import { cleanup } from "@testing-library/react";
import { useAuthStore } from "../store/authStore";

// Mock para window.matchMedia requerido por hooks responsivos (use-mobile de Shadcn)
Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Cada test arranca sin sesión guardada y sin mocks de tests anteriores
afterEach(() => {
  cleanup();
  localStorage.clear();
  useAuthStore.setState({ token: null, user: null, inicializado: false });
  vi.restoreAllMocks();
});

