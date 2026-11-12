import { useState } from "react";
import { ArrowRight, Eye, EyeOff, Lock, Mail } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { jwtDecode } from "jwt-decode";
import { motion, AnimatePresence } from "motion/react";
import axiosClient from "../../config/axiosClient";
import { useAuthStore } from "../../store/authStore";
import { obtenerMiPerfil } from "@/features/perfil/api/perfilApi";
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { PelotaTenisIcon } from "@/components/ui/PelotaTenisIcon";
import bgImage from "../../assets/fondo-tenis.jpeg";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mostrarPassword, setMostrarPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);

  // Estados para errores
  const [error, setError] = useState("");
  const [emailError, setEmailError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const navigate = useNavigate();
  const setLogin = useAuthStore((state) => state.setLogin);

  const validateEmail = (value) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!value) {
      setEmailError("El correo electrónico es obligatorio.");
      return false;
    } else if (!emailRegex.test(value)) {
      setEmailError("Ingresá un formato válido (ej: nombre@correo.com).");
      return false;
    }

    setEmailError("");
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!validateEmail(email)) {
      return;
    }

    setIsLoading(true);

    try {
      const { data } = await axiosClient.post("/auth/login", { email, password });
      const decoded = jwtDecode(data.accessToken);

      // El sub del token es el id de la persona: sin el, las pantallas que
      // filtran por "lo mio" (mis clases, mis entrenamientos) no tienen por
      // que filtrar.
      setLogin(data.accessToken, { id: Number(decoded.sub), email, rol: decoded.rol });

      try {
        const perfil = await obtenerMiPerfil();
        if (perfil) {
          useAuthStore.getState().setUsuario({
            nombre: perfil.nombre,
            apellido: perfil.apellido,
            fotoUrl: perfil.fotoUrl ?? null,
          });
        }
      } catch {
        // Silencioso: si /me falla, el login no se interrumpe
      }

      navigate("/dashboard");
    } catch (err) {
      console.error("Error en login:", err);
      const status = err.response?.status;
      if (status === 401) {
        setError("Correo o contraseña incorrectos.");
      } else if (status === 429) {
        setError(err.response?.data?.mensaje ?? "Demasiados intentos. Esperá unos minutos antes de volver a intentar.");
      } else {
        setError("No se pudo conectar con el servidor. Intentá nuevamente.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="grid min-h-svh lg:h-svh lg:overflow-hidden lg:grid-cols-2 bg-background font-sans selection:bg-foreground selection:text-background">
      {/* Columna Izquierda: Formulario Sobrio y Espacioso (Linear-Clean) */}
      <div className="flex flex-col justify-between p-8 sm:p-10 lg:p-12 overflow-y-auto lg:overflow-hidden h-full">
        {/* Cabecera con Isotipo de Pelota de Tenis */}
        <header className="shrink-0">
          <Link to="/" className="inline-flex items-center gap-2.5 text-foreground hover:opacity-85 transition-opacity">
            <PelotaTenisIcon className="size-8" />
            <span className="text-base font-semibold tracking-tight">
              Tenis Ahora
            </span>
          </Link>
        </header>

        {/* Contenedor central sin scroll forzado en escritorio */}
        <div className="flex flex-1 items-center justify-center py-8 my-auto">
          <motion.div
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
            className="w-full max-w-sm"
          >
            <div className="mb-6 space-y-1">
              <h1 className="text-2xl font-semibold tracking-tight text-foreground">
                Iniciar sesión
              </h1>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Ingresá tus credenciales para acceder a tu cuenta.
              </p>
            </div>

            {/* Error global con AnimatePresence rápido */}
            <AnimatePresence mode="wait">
              {error && (
                <motion.div
                  key="login-error"
                  initial={{ opacity: 0, y: -4 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -4 }}
                  transition={{ duration: 0.15 }}
                  className="mb-5"
                >
                  <div className="p-3 bg-destructive/10 text-destructive text-sm rounded-lg border border-destructive/20 text-center font-medium">
                    {error}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            <form onSubmit={handleSubmit} noValidate>
              <FieldGroup className="gap-5">
                <Field data-invalid={Boolean(emailError)}>
                  <FieldLabel htmlFor="login-email" className="text-xs font-medium text-foreground/85">
                    Correo electrónico
                  </FieldLabel>
                  <div className="relative flex items-center mt-1">
                    <Mail className="absolute left-3.5 size-4 text-muted-foreground/70 pointer-events-none" />
                    <Input
                      id="login-email"
                      type="email"
                      placeholder="nombre@correo.com"
                      autoComplete="email"
                      value={email}
                      onBlur={(e) => validateEmail(e.target.value)}
                      onChange={(e) => {
                        setEmail(e.target.value);
                        if (emailError) validateEmail(e.target.value);
                        setError("");
                      }}
                      disabled={isLoading}
                      aria-invalid={Boolean(emailError)}
                      className="pl-10 pr-3.5 h-10 rounded-lg border border-border bg-background focus-visible:border-foreground/40 transition-colors"
                    />
                  </div>
                  <AnimatePresence>
                    {emailError && (
                      <motion.div
                        initial={{ opacity: 0, y: -2 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -2 }}
                        transition={{ duration: 0.12 }}
                      >
                        <FieldDescription className="text-xs font-medium text-destructive mt-1.5">
                          {emailError}
                        </FieldDescription>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </Field>

                <Field>
                  <div className="flex items-center justify-between">
                    <FieldLabel htmlFor="login-password" className="text-xs font-medium text-foreground/85">
                      Contraseña
                    </FieldLabel>
                    <a href="#" className="text-xs text-muted-foreground hover:text-foreground transition-colors underline-offset-4 hover:underline">
                      ¿Olvidaste tu contraseña?
                    </a>
                  </div>
                  <div className="relative flex rounded-lg border border-border focus-within:ring-2 focus-within:ring-ring focus-within:border-ring bg-background overflow-hidden mt-1 h-10">
                    <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 size-4 text-muted-foreground/70 pointer-events-none" />
                    <Input
                      id="login-password"
                      type={mostrarPassword ? "text" : "password"}
                      placeholder="Tu contraseña"
                      autoComplete="current-password"
                      value={password}
                      onChange={(e) => {
                        setPassword(e.target.value);
                        setError("");
                      }}
                      required
                      disabled={isLoading}
                      className="border-0 ring-0 focus-visible:ring-0 focus-visible:border-0 rounded-none pl-10 pr-3 h-full flex-1 bg-transparent shadow-none text-sm"
                    />
                    <button
                      type="button"
                      onClick={() => setMostrarPassword(!mostrarPassword)}
                      className="flex items-center justify-center px-3.5 border-l border-border hover:bg-muted text-muted-foreground hover:text-foreground transition-colors shrink-0"
                      tabIndex={-1}
                      aria-label={mostrarPassword ? "Ocultar contraseña" : "Ver contraseña"}
                    >
                      {mostrarPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                    </button>
                  </div>
                </Field>

                <div className="flex items-center justify-between">
                  <label className="flex items-center gap-2 cursor-pointer select-none text-xs text-muted-foreground hover:text-foreground transition-colors">
                    <input
                      type="checkbox"
                      checked={rememberMe}
                      onChange={(e) => setRememberMe(e.target.checked)}
                      disabled={isLoading}
                      className="size-3.5 rounded border-border text-foreground focus:ring-foreground"
                    />
                    <span>Mantener la sesión</span>
                  </label>
                </div>

                <motion.div whileTap={{ scale: 0.99 }}>
                  <Button
                    type="submit"
                    disabled={isLoading}
                    className="w-full h-10 px-5 py-2.5 rounded-lg font-medium text-sm shadow-none gap-2 mt-1"
                  >
                    {isLoading ? "Ingresando..." : "Ingresar"}
                    {!isLoading && <ArrowRight className="size-4" />}
                  </Button>
                </motion.div>
              </FieldGroup>
            </form>

            <div className="mt-6 text-center text-xs text-muted-foreground">
              ¿No tenés una cuenta?{" "}
              <Link to="/registro" className="text-foreground font-medium hover:underline underline-offset-4">
                Registrate
              </Link>
            </div>
          </motion.div>
        </div>

        {/* Footer simple y discreto */}
        <footer className="shrink-0 text-xs text-muted-foreground/70">
          Al continuar, aceptás nuestros{" "}
          <a href="#" className="underline hover:text-foreground transition-colors">
            Términos de Servicio
          </a>{" "}
          y la{" "}
          <a href="#" className="underline hover:text-foreground transition-colors">
            Política de Privacidad
          </a>.
        </footer>
      </div>

      {/* Columna Derecha: Portada Homogénea (50% en escritorio, h-full fija, overlay sutil) */}
      <div className="relative hidden lg:flex flex-col justify-end p-12 xl:p-16 h-full overflow-hidden bg-[#0a0f0d] text-white">
        <img
          src={bgImage}
          alt="Canchas de Tenis Ahora"
          className="absolute inset-0 h-full w-full object-cover object-center opacity-40 mix-blend-luminosity"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#0a0f0d] via-[#0a0f0d]/50 to-transparent" />

        <div className="relative z-10 max-w-sm space-y-2">
          <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-md bg-white/10 backdrop-blur-xs text-[11px] font-mono tracking-wider uppercase text-white/80">
            <span className="size-1.5 rounded-full bg-[#ccdb38]" />
            Temporada 2026
          </div>
          <h2 className="text-2xl font-medium tracking-tight text-white/95 text-balance">
            Tu próximo set empieza acá.
          </h2>
          <p className="text-sm text-white/60 leading-relaxed text-balance">
            Reservas de cancha y gestión integral para socios y cuerpo técnico.
          </p>
        </div>
      </div>
    </div>
  );
}
