import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { AlertCircle, ArrowLeft, Save, Trophy } from "lucide-react";
import { Button, buttonVariants } from "@/components/ui/button";
import axiosClient from "@/config/axiosClient";
import { crearTorneo, modificarTorneo, obtenerTorneo } from "../api/torneosApi";
import { AYUDA_TIPO, COMO_SE_JUEGA, OPCIONES_TIPO, TipoCompetencia, tipoDe } from "../model/competencia";
import { mensajeDeError } from "@/lib/erroresApi";

export function TorneoFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const esEdicion = Boolean(id);

  const [canchas, setCanchas] = useState([]);
  const [cargando, setCargando] = useState(esEdicion);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState("");

  const [form, setForm] = useState({
    nombre: "",
    tipo: TipoCompetencia.TORNEO,
    canchaId: "",
    categoria: "MASCULINO",
    modalidad: "SINGLE",
    tipoSuperficie: "LADRILLO",
    fechaInicio: "",
    fechaFin: "",
    costoInscripcion: "0",
  });

  useEffect(() => {
    async function cargarInicial() {
      try {
        const resCanchas = await axiosClient.get("/canchas");
        const listadoCanchas = resCanchas.data?.contenido || resCanchas.data || [];
        setCanchas(Array.isArray(listadoCanchas) ? listadoCanchas : []);

        if (esEdicion) {
          const t = await obtenerTorneo(id);
          setForm({
            nombre: t.nombre || "",
            tipo: tipoDe(t),
            canchaId: t.canchaId ? String(t.canchaId) : "",
            categoria: t.categoria || "MASCULINO",
            modalidad: t.modalidad || "SINGLE",
            tipoSuperficie: t.tipoSuperficie || "LADRILLO",
            fechaInicio: t.fechaInicio || "",
            fechaFin: t.fechaFin || "",
            costoInscripcion: t.costoInscripcion != null ? String(t.costoInscripcion) : "0",
          });
        }
      } catch (err) {
        setError(mensajeDeError(err, "No se pudieron cargar los datos iniciales."));
      } finally {
        setCargando(false);
      }
    }
    cargarInicial();
  }, [id, esEdicion]);

  function handleChange(campo, valor) {
    setForm((prev) => ({ ...prev, [campo]: valor }));
    setError("");
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");

    if (!form.nombre.trim()) {
      setError("El nombre del torneo es obligatorio.");
      return;
    }
    if (!form.fechaInicio || !form.fechaFin) {
      setError("Las fechas de inicio y fin son obligatorias.");
      return;
    }
    if (form.fechaFin < form.fechaInicio) {
      setError("La fecha de fin no puede ser anterior a la fecha de inicio.");
      return;
    }

    const costo = Number(form.costoInscripcion);
    if (Number.isNaN(costo) || costo < 0) {
      setError("El arancel de inscripción debe ser mayor o igual a 0.");
      return;
    }

    const payload = {
      nombre: form.nombre.trim(),
      tipo: form.tipo,
      canchaId: form.canchaId ? Number(form.canchaId) : null,
      categoria: form.categoria,
      modalidad: form.modalidad,
      tipoSuperficie: form.tipoSuperficie,
      fechaInicio: form.fechaInicio,
      fechaFin: form.fechaFin,
      costoInscripcion: costo,
    };

    setGuardando(true);
    try {
      if (esEdicion) {
        await modificarTorneo(id, payload);
      } else {
        await crearTorneo(payload);
      }
      navigate("/torneos");
    } catch (err) {
      setError(mensajeDeError(err, "No se pudo guardar el torneo."));
    } finally {
      setGuardando(false);
    }
  }

  if (cargando) {
    return <div className="p-12 text-center text-sm text-muted-foreground">Cargando formulario...</div>;
  }

  return (
    <div className="space-y-6 p-4 sm:p-6 lg:p-8 max-w-4xl mx-auto">
      <div className="flex items-center gap-3">
        <Link to="/torneos" className={buttonVariants({ variant: "ghost", size: "icon" })}>
          <ArrowLeft className="size-5" />
          <span className="sr-only">Volver a torneos</span>
        </Link>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2">
            <Trophy className="size-6 text-primary" />
            {esEdicion ? "Editar torneo" : "Nuevo torneo"}
          </h1>
          <p className="text-sm text-muted-foreground">
            {esEdicion
              ? "Modificá la configuración del torneo existente."
              : "Definí las reglas, fechas y superficie para el nuevo torneo del club."}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6 rounded-xl border border-border bg-card p-6 shadow-sm">
        {error && (
          <div
            role="alert"
            className="flex items-start gap-2.5 rounded-lg border border-destructive/20 bg-destructive/10 p-3.5 text-sm text-destructive"
          >
            <AlertCircle className="size-5 shrink-0 mt-0.5" />
            <p>{error}</p>
          </div>
        )}

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="space-y-1.5 sm:col-span-2">
            <label htmlFor="nombre" className="text-sm font-semibold">
              Nombre del torneo <span className="text-destructive">*</span>
            </label>
            <input
              id="nombre"
              type="text"
              required
              value={form.nombre}
              onChange={(e) => handleChange("nombre", e.target.value)}
              placeholder="Ej: Torneo Anual Primavera 2026"
              className="flex h-10 w-full rounded-lg border border-input bg-background px-3.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
          </div>

          <div className="space-y-1.5">
            <label htmlFor="tipo" className="text-sm font-semibold">
              Tipo de competencia <span className="text-destructive">*</span>
            </label>
            <select
              id="tipo"
              value={form.tipo}
              onChange={(e) => handleChange("tipo", e.target.value)}
              className="flex h-10 w-full rounded-lg border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {OPCIONES_TIPO.map((opcion) => (
                <option key={opcion.value} value={opcion.value}>
                  {opcion.label} — {COMO_SE_JUEGA[opcion.value]}
                </option>
              ))}
            </select>
            <p className="text-xs text-muted-foreground">{AYUDA_TIPO[form.tipo]}</p>
          </div>

          <div className="space-y-1.5">
            <label htmlFor="categoria" className="text-sm font-semibold">
              Categoría <span className="text-destructive">*</span>
            </label>
            <select
              id="categoria"
              value={form.categoria}
              onChange={(e) => handleChange("categoria", e.target.value)}
              className="flex h-10 w-full rounded-lg border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="MASCULINO">Masculino</option>
              <option value="FEMENINO">Femenino</option>
              <option value="MIXTO">Mixto</option>
            </select>
          </div>

          <div className="space-y-1.5">
            <label htmlFor="modalidad" className="text-sm font-semibold">
              Modalidad <span className="text-destructive">*</span>
            </label>
            <select
              id="modalidad"
              value={form.modalidad}
              onChange={(e) => handleChange("modalidad", e.target.value)}
              className="flex h-10 w-full rounded-lg border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="SINGLE">Singles (Individual)</option>
              <option value="PAREJA">Parejas (Dobles)</option>
            </select>
          </div>

          <div className="space-y-1.5">
            <label htmlFor="tipoSuperficie" className="text-sm font-semibold">
              Superficie <span className="text-destructive">*</span>
            </label>
            <select
              id="tipoSuperficie"
              value={form.tipoSuperficie}
              onChange={(e) => handleChange("tipoSuperficie", e.target.value)}
              className="flex h-10 w-full rounded-lg border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="LADRILLO">Polvo de ladrillo</option>
              <option value="CEMENTO">Cemento</option>
              <option value="PASTO">Césped</option>
            </select>
          </div>

          <div className="space-y-1.5">
            <label htmlFor="canchaId" className="text-sm font-semibold">
              Cancha sede
            </label>
            <select
              id="canchaId"
              value={form.canchaId}
              onChange={(e) => handleChange("canchaId", e.target.value)}
              className="flex h-10 w-full rounded-lg border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">(Sin cancha fija asignada)</option>
              {canchas.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.identificador || `Cancha #${c.id}`}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1.5">
            <label htmlFor="fechaInicio" className="text-sm font-semibold">
              Fecha de inicio <span className="text-destructive">*</span>
            </label>
            <input
              id="fechaInicio"
              type="date"
              required
              value={form.fechaInicio}
              onChange={(e) => handleChange("fechaInicio", e.target.value)}
              className="flex h-10 w-full rounded-lg border border-input bg-background px-3.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
          </div>

          <div className="space-y-1.5">
            <label htmlFor="fechaFin" className="text-sm font-semibold">
              Fecha de fin <span className="text-destructive">*</span>
            </label>
            <input
              id="fechaFin"
              type="date"
              required
              value={form.fechaFin}
              onChange={(e) => handleChange("fechaFin", e.target.value)}
              className="flex h-10 w-full rounded-lg border border-input bg-background px-3.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
          </div>

          <div className="space-y-1.5 sm:col-span-2">
            <label htmlFor="costoInscripcion" className="text-sm font-semibold">
              Costo de inscripción (ARS)
            </label>
            <input
              id="costoInscripcion"
              type="number"
              min="0"
              step="100"
              value={form.costoInscripcion}
              onChange={(e) => handleChange("costoInscripcion", e.target.value)}
              className="flex h-10 w-full rounded-lg border border-input bg-background px-3.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
          </div>
        </div>

        <div className="flex items-center justify-end gap-3 pt-4 border-t border-border">
          <Link to="/torneos" className={buttonVariants({ variant: "outline" })}>
            Cancelar
          </Link>
          <Button type="submit" disabled={guardando} className="gap-2">
            <Save className="size-4" />
            {guardando ? "Guardando..." : esEdicion ? "Guardar cambios" : "Crear torneo"}
          </Button>
        </div>
      </form>
    </div>
  );
}
