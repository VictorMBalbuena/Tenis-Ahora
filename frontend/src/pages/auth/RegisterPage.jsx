import { useState } from "react";
import { ArrowRight, Eye, EyeOff, Lock, Mail } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "motion/react";
import axiosClient from "../../config/axiosClient";
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import bgImage from "../../assets/fondo-tenis.jpeg";
import { PelotaTenisIcon } from "@/components/ui/PelotaTenisIcon";

const soloDigitos = (valor) => valor.replace(/\D/g, "");

export default function RegisterPage() {
  const [nombre, setNombre] = useState("");
  const [apellido, setApellido] = useState("");
  const [dni, setDni] = useState("");
  const [codigoArea, setCodigoArea] = useState("");
  const [numeroTelefono, setNumeroTelefono] = useState("");
  const [calle, setCalle] = useState("");
  const [numeroCalle, setNumeroCalle] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [mostrarPassword, setMostrarPassword] = useState(false);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const navigate = useNavigate();

  const editar = (setter, limpiar = (v) => v) => (e) => {
    setter(limpiar(e.target.value));
    setError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (password !== confirmPassword) {
      setError("Las contraseñas no coinciden.");
      return;
    }

    if (!/^\d{7,8}$/.test(dni)) {
      setError("El DNI debe tener 7 u 8 números.");
      return;
    }

    if (password.length < 8 || password.length > 72) {
      setError("La contraseña debe tener entre 8 y 72 caracteres.");
      return;
    }

    setIsLoading(true);

    try {
      await axiosClient.post("/auth/register", {
        nombre,
        apellido,
        dni,
        calle,
        numeroCalle,
        codigoArea,
        numeroTelefono,
        email,
        password,
      });
      navigate("/login");
    } catch (err) {
      console.error("Error en registro:", err);
      const status = err.response?.status;
      if (status === 409) {
        setError("Ya existe una cuenta con ese DNI o correo electrónico.");
      } else if (status === 429) {
        setError(err.response?.data?.mensaje ?? "Hiciste demasiados pedidos. Probá de nuevo más tarde.");
      } else if (status === 400) {
        const detalle = Object.values(err.response?.data?.campos ?? {})[0];
        setError(detalle ?? "Revisá los datos ingresados.");
      } else {
        setError("Ocurrió un error al intentar crear la cuenta.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="grid min-h-svh lg:h-svh lg:overflow-hidden lg:grid-cols-2 bg-background font-sans selection:bg-foreground selection:text-background">
      {/* Columna Izquierda: Formulario Compacto de 2 Columnas (Cero Scroll en Escritorio) */}
      <div className="flex flex-col justify-between p-6 sm:p-8 lg:py-5 lg:px-8 xl:py-6 xl:px-10 overflow-y-auto lg:overflow-hidden h-full">
        {/* Cabecera con Isotipo de Pelota de Tenis */}
        <header className="shrink-0">
          <Link to="/" className="inline-flex items-center gap-2.5 text-foreground hover:opacity-85 transition-opacity">
            <PelotaTenisIcon className="size-8" />
            <span className="text-base font-semibold tracking-tight">
              Tenis Ahora
            </span>
          </Link>
        </header>

        {/* Contenedor central ajustado para cero scroll en lg: */}
        <div className="flex flex-1 items-center justify-center py-2 my-auto">
          <motion.div
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
            className="w-full max-w-xl"
          >
            <div className="mb-3 space-y-0.5">
              <h1 className="text-xl sm:text-2xl font-semibold tracking-tight text-foreground">
                Crear cuenta
              </h1>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Completá tus datos para habilitar tu cuenta de socio en el club.
              </p>
            </div>

            {/* Error global con AnimatePresence rápido */}
            <AnimatePresence mode="wait">
              {error && (
                <motion.div
                  key="register-error"
                  initial={{ opacity: 0, y: -4 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -4 }}
                  transition={{ duration: 0.15 }}
                  className="mb-2.5"
                >
                  <div className="p-2 bg-destructive/10 text-destructive text-xs rounded-lg border border-destructive/20 text-center font-medium">
                    {error}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            <form onSubmit={handleSubmit} noValidate>
              <FieldGroup className="gap-2.5">
                {/* Fila 1: Nombre (50%) | Apellido (50%) */}
                <div className="grid grid-cols-2 gap-3">
                  <Field>
                    <FieldLabel htmlFor="reg-nombre" className="text-xs font-medium text-foreground/85">
                      Nombre
                    </FieldLabel>
                    <Input
                      id="reg-nombre"
                      type="text"
                      autoComplete="given-name"
                      value={nombre}
                      onChange={editar(setNombre)}
                      required
                      disabled={isLoading}
                      className="h-9 px-3 rounded-lg border border-border bg-background focus-visible:border-foreground/40 transition-colors mt-1"
                    />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="reg-apellido" className="text-xs font-medium text-foreground/85">
                      Apellido
                    </FieldLabel>
                    <Input
                      id="reg-apellido"
                      type="text"
                      autoComplete="family-name"
                      value={apellido}
                      onChange={editar(setApellido)}
                      required
                      disabled={isLoading}
                      className="h-9 px-3 rounded-lg border border-border bg-background focus-visible:border-foreground/40 transition-colors mt-1"
                    />
                  </Field>
                </div>

                {/* Fila 2: DNI (50%) | Teléfono compuesto: Cód. área + Número (50%) */}
                <div className="grid grid-cols-2 gap-3">
                  <Field>
                    <FieldLabel htmlFor="reg-dni" className="text-xs font-medium text-foreground/85">
                      DNI
                    </FieldLabel>
                    <Input
                      id="reg-dni"
                      type="text"
                      inputMode="numeric"
                      maxLength={8}
                      placeholder="30123456"
                      value={dni}
                      onChange={editar(setDni, soloDigitos)}
                      required
                      disabled={isLoading}
                      className="h-9 px-3 rounded-lg font-mono border border-border bg-background focus-visible:border-foreground/40 transition-colors mt-1"
                    />
                  </Field>
                  <div>
                    <div className="grid grid-cols-[82px_1fr] gap-2">
                      <Field>
                        <FieldLabel htmlFor="reg-area" className="text-xs font-medium text-foreground/85">
                          Cód. de área
                        </FieldLabel>
                        <Input
                          id="reg-area"
                          type="text"
                          inputMode="numeric"
                          maxLength={4}
                          placeholder="11"
                          autoComplete="tel-area-code"
                          value={codigoArea}
                          onChange={editar(setCodigoArea, soloDigitos)}
                          required
                          disabled={isLoading}
                          className="h-9 px-2.5 rounded-lg font-mono border border-border bg-background focus-visible:border-foreground/40 transition-colors mt-1"
                        />
                      </Field>
                      <Field>
                        <FieldLabel htmlFor="reg-telefono" className="text-xs font-medium text-foreground/85">
                          Número
                        </FieldLabel>
                        <Input
                          id="reg-telefono"
                          type="tel"
                          inputMode="numeric"
                          maxLength={8}
                          placeholder="12345678"
                          autoComplete="tel-local"
                          value={numeroTelefono}
                          onChange={editar(setNumeroTelefono, soloDigitos)}
                          required
                          disabled={isLoading}
                          className="h-9 px-3 rounded-lg font-mono border border-border bg-background focus-visible:border-foreground/40 transition-colors mt-1"
                        />
                      </Field>
                    </div>
                  </div>
                </div>

                {/* Fila 3: Domicilio: Calle (70%) | Número/Altura (30%) */}
                <div className="grid grid-cols-[7fr_3fr] gap-3">
                  <Field>
                    <FieldLabel htmlFor="reg-calle" className="text-xs font-medium text-foreground/85">
                      Calle
                    </FieldLabel>
                    <Input
                      id="reg-calle"
                      type="text"
                      placeholder="Av. Corrientes"
                      autoComplete="address-line1"
                      value={calle}
                      onChange={editar(setCalle)}
                      required
                      disabled={isLoading}
                      className="h-9 px-3 rounded-lg border border-border bg-background focus-visible:border-foreground/40 transition-colors mt-1"
                    />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor="reg-numero-calle" className="text-xs font-medium text-foreground/85">
                      Número
                    </FieldLabel>
                    <Input
                      id="reg-numero-calle"
                      type="text"
                      inputMode="numeric"
                      maxLength={6}
                      placeholder="1234"
                      value={numeroCalle}
                      onChange={editar(setNumeroCalle, soloDigitos)}
                      required
                      disabled={isLoading}
                      className="h-9 px-3 rounded-lg font-mono border border-border bg-background focus-visible:border-foreground/40 transition-colors mt-1"
                    />
                  </Field>
                </div>

                {/* Fila 4: Correo electrónico (100% de ancho) */}
                <Field>
                  <FieldLabel htmlFor="reg-email" className="text-xs font-medium text-foreground/85">
                    Correo electrónico
                  </FieldLabel>
                  <div className="relative flex items-center mt-1">
                    <Mail className="absolute left-3.5 size-4 text-muted-foreground/70 pointer-events-none" />
                    <Input
                      id="reg-email"
                      type="email"
                      placeholder="nombre@tenisahora.com"
                      autoComplete="email"
                      value={email}
                      onChange={editar(setEmail)}
                      required
                      disabled={isLoading}
                      className="pl-10 pr-3.5 h-9 rounded-lg border border-border bg-background focus-visible:border-foreground/40 transition-colors"
                    />
                  </div>
                </Field>

                {/* Fila 5: Contraseña (50%) | Confirmar contraseña (50%) */}
                <div className="grid grid-cols-2 gap-3">
                  <Field>
                    <FieldLabel htmlFor="reg-password" className="text-xs font-medium text-foreground/85">
                      Contraseña
                    </FieldLabel>
                    <div className="relative flex rounded-lg border border-border focus-within:ring-2 focus-within:ring-ring focus-within:border-ring bg-background overflow-hidden mt-1 h-9">
                      <Lock className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground/70 pointer-events-none" />
                      <Input
                        id="reg-password"
                        type={mostrarPassword ? "text" : "password"}
                        placeholder="Mínimo 8 caracteres"
                        autoComplete="new-password"
                        value={password}
                        onChange={editar(setPassword)}
                        required
                        disabled={isLoading}
                        className="border-0 ring-0 focus-visible:ring-0 focus-visible:border-0 rounded-none pl-9 pr-2.5 h-full flex-1 bg-transparent shadow-none text-sm"
                      />
                      <button
                        type="button"
                        onClick={() => setMostrarPassword(!mostrarPassword)}
                        className="flex items-center justify-center px-3 border-l border-border hover:bg-muted text-muted-foreground hover:text-foreground transition-colors shrink-0"
                        tabIndex={-1}
                        aria-label={mostrarPassword ? "Ocultar" : "Ver"}
                      >
                        {mostrarPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                      </button>
                    </div>
                  </Field>

                  <Field>
                    <FieldLabel htmlFor="reg-confirm" className="text-xs font-medium text-foreground/85">
                      Repetir contraseña
                    </FieldLabel>
                    <div className="relative flex items-center mt-1">
                      <Lock className="absolute left-3.5 size-4 text-muted-foreground/70 pointer-events-none" />
                      <Input
                        id="reg-confirm"
                        type={mostrarPassword ? "text" : "password"}
                        placeholder="Repetí tu contraseña"
                        autoComplete="new-password"
                        value={confirmPassword}
                        onChange={editar(setConfirmPassword)}
                        required
                        disabled={isLoading}
                        className="pl-10 pr-3.5 h-9 rounded-lg border border-border bg-background focus-visible:border-foreground/40 transition-colors"
                      />
                    </div>
                  </Field>
                </div>

                {/* Fila 6: Botón principal de registro */}
                <div className="space-y-2 pt-0.5">
                  <motion.div whileTap={{ scale: 0.99 }}>
                    <Button
                      type="submit"
                      disabled={isLoading}
                      className="w-full h-10 px-5 py-2.5 rounded-lg font-medium text-sm shadow-none gap-2"
                    >
                      {isLoading ? "Creando cuenta..." : "Registrarme"}
                      {!isLoading && <ArrowRight className="size-4" />}
                    </Button>
                  </motion.div>
                  <div className="text-center text-xs text-muted-foreground">
                    ¿Ya tenés una cuenta?{" "}
                    <Link to="/login" className="text-foreground font-medium hover:underline underline-offset-4">
                      Iniciá sesión
                    </Link>
                  </div>
                </div>
              </FieldGroup>
            </form>
          </motion.div>
        </div>

        {/* Footer simple y discreto */}
        <footer className="shrink-0 text-xs text-muted-foreground/70">
          Al registrarte, aceptás nuestros{" "}
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
            Tu juego al siguiente nivel.
          </h2>
          <p className="text-sm text-white/60 leading-relaxed text-balance">
            Sumate al club, reservá turnos en tiempo real y conectate con la comunidad tenística.
          </p>
        </div>
      </div>
    </div>
  );
}
