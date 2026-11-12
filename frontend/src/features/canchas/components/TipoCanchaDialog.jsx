import { useState, useEffect } from "react";
import { AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SelectField } from "@/components/ui/select-field";
import { crearTipoCancha, actualizarTipoCancha } from "../api/tiposCanchaApi";
import { mensajeDeError } from "@/lib/erroresApi";

export function TipoCanchaDialog({ abierto, tipoCancha, onCerrar, onGuardado }) {
  const esModificacion = Boolean(tipoCancha);

  const [formData, setFormData] = useState({
    nombre: "",
    tipoSuperficie: "LADRILLO",
    capacidad: "4",
    descripcion: "",
  });
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    if (tipoCancha) {
      setFormData({
        nombre: tipoCancha.nombre || "",
        tipoSuperficie: tipoCancha.tipoSuperficie || "LADRILLO",
        capacidad: String(tipoCancha.capacidad || 4),
        descripcion: tipoCancha.descripcion || "",
      });
    } else {
      setFormData({
        nombre: "",
        tipoSuperficie: "LADRILLO",
        capacidad: "4",
        descripcion: "",
      });
    }
    setError("");
  }, [tipoCancha, abierto]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  async function handleSubmit(e) {
    e.preventDefault();
    if (!formData.nombre.trim()) {
      setError("El nombre es obligatorio.");
      return;
    }
    setError("");
    setGuardando(true);

    try {
      const payload = {
        nombre: formData.nombre.trim(),
        tipoSuperficie: formData.tipoSuperficie,
        capacidad: Number(formData.capacidad),
        descripcion: formData.descripcion.trim() || null,
      };

      const resultado = esModificacion
        ? await actualizarTipoCancha(tipoCancha.id, payload)
        : await crearTipoCancha(payload);

      onGuardado(resultado);
    } catch (err) {
      setError(mensajeDeError(err, "No se pudo guardar el tipo de cancha."));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Dialog open={abierto} onOpenChange={(v) => !v && onCerrar()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{esModificacion ? "Modificar tipo de cancha" : "Nuevo tipo de cancha"}</DialogTitle>
          <DialogDescription>
            {esModificacion
              ? "Actualiza la superficie y capacidad de este tipo de cancha."
              : "Registra un nuevo tipo de cancha con su superficie y capacidad."}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <p role="alert" className="flex items-start gap-2 rounded-lg bg-destructive/10 p-3 text-sm text-destructive">
              <AlertCircle className="mt-0.5 size-4 shrink-0" />
              {error}
            </p>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="tipo-nombre">Nombre</Label>
            <Input
              id="tipo-nombre"
              name="nombre"
              placeholder="Ej: Polvo de Ladrillo Dobles"
              value={formData.nombre}
              onChange={handleChange}
              autoFocus
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>Superficie</Label>
              <SelectField
                name="tipoSuperficie"
                value={formData.tipoSuperficie}
                onChange={handleChange}
                opciones={[
                  { value: "LADRILLO", label: "Ladrillo" },
                  { value: "CEMENTO", label: "Cemento" },
                  { value: "PASTO", label: "Pasto" },
                ]}
              />
            </div>

            <div className="space-y-1.5">
              <Label>Capacidad</Label>
              <SelectField
                name="capacidad"
                value={formData.capacidad}
                onChange={handleChange}
                opciones={[
                  { value: "2", label: "2 jugadores (Singles)" },
                  { value: "4", label: "4 jugadores (Dobles)" },
                ]}
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="tipo-desc">Descripción (opcional)</Label>
            <Input
              id="tipo-desc"
              name="descripcion"
              placeholder="Ej: Cancha estándar de ladrillo para 4 jugadores"
              value={formData.descripcion}
              onChange={handleChange}
            />
          </div>

          <DialogFooter className="pt-2">
            <Button type="button" variant="outline" onClick={onCerrar} disabled={guardando}>
              Cancelar
            </Button>
            <Button type="submit" disabled={guardando || !formData.nombre.trim()}>
              {guardando ? "Guardando..." : esModificacion ? "Guardar cambios" : "Crear tipo"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

