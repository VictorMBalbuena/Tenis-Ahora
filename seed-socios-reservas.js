/**
 * Script de carga de datos de prueba: 8 socios + 2 reservas por cancha por día (durante 4 días).
 *
 * Ejecución manual desde la consola:
 *   node seed-socios-reservas.js
 *
 * Requiere tener el backend corriendo en http://localhost:8080 (o configurar API_URL).
 */

const BASE_URL = process.env.API_URL || "http://localhost:8080";
const ADMIN_USER = {
  email: process.env.ADMIN_EMAIL || "admin@tenisahora.com",
  password: process.env.ADMIN_PASSWORD || "TenisAhora2026!",
};

const NUEVOS_SOCIOS = [
  {
    nombre: "Lucía",
    apellido: "Pérez",
    edad: 26,
    sexo: "FEMENINO",
    dni: "35111001",
    calle: "Av. Corrientes",
    numeroCalle: "1234",
    codigoArea: "11",
    numeroTelefono: "45678901",
    email: "lucia.perez@test.com",
    password: "TenisAhora2026!",
  },
  {
    nombre: "Martín",
    apellido: "Rodríguez",
    edad: 34,
    sexo: "MASCULINO",
    dni: "35111002",
    calle: "San Martín",
    numeroCalle: "450",
    codigoArea: "11",
    numeroTelefono: "45678902",
    email: "martin.rodriguez@test.com",
    password: "TenisAhora2026!",
  },
  {
    nombre: "Valentina",
    apellido: "Rossi",
    edad: 19,
    sexo: "FEMENINO",
    dni: "35111003",
    calle: "Belgrano",
    numeroCalle: "780",
    codigoArea: "11",
    numeroTelefono: "45678903",
    email: "valentina.rossi@test.com",
    password: "TenisAhora2026!",
  },
  {
    nombre: "Tomás",
    apellido: "Benítez",
    edad: 42,
    sexo: "MASCULINO",
    dni: "35111004",
    calle: "Mitre",
    numeroCalle: "1020",
    codigoArea: "11",
    numeroTelefono: "45678904",
    email: "tomas.benitez@test.com",
    password: "TenisAhora2026!",
  },
  {
    nombre: "Sofía",
    apellido: "Morales",
    edad: 16,
    sexo: "FEMENINO",
    dni: "35111005",
    calle: "Rivadavia",
    numeroCalle: "310",
    codigoArea: "11",
    numeroTelefono: "45678905",
    email: "sofia.morales@test.com",
    password: "TenisAhora2026!",
  },
  {
    nombre: "Facundo",
    apellido: "Herrera",
    edad: 55,
    sexo: "MASCULINO",
    dni: "35111006",
    calle: "Sarmiento",
    numeroCalle: "925",
    codigoArea: "11",
    numeroTelefono: "45678906",
    email: "facundo.herrera@test.com",
    password: "TenisAhora2026!",
  },
  {
    nombre: "Camila",
    apellido: "Díaz",
    edad: 12,
    sexo: "FEMENINO",
    dni: "35111007",
    calle: "España",
    numeroCalle: "640",
    codigoArea: "11",
    numeroTelefono: "45678907",
    email: "camila.diaz@test.com",
    password: "TenisAhora2026!",
  },
  {
    nombre: "Lucas",
    apellido: "Giménez",
    edad: 79,
    sexo: "MASCULINO",
    dni: "35111008",
    calle: "Las Heras",
    numeroCalle: "815",
    codigoArea: "11",
    numeroTelefono: "45678908",
    email: "lucas.gimenez@test.com",
    password: "TenisAhora2026!",
  },
];

const HORARIOS_SLOTS = ["10:00", "16:00"];
const CLAVE_SOCIOS = "TenisAhora2026!";

function formatearFecha(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function fechaNacimientoParaEdad(edad) {
  const nacimiento = new Date();
  nacimiento.setFullYear(nacimiento.getFullYear() - edad);
  return formatearFecha(nacimiento);
}

async function requestJson(url, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  const res = await fetch(`${BASE_URL}${url}`, { ...options, headers });
  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }
  return { status: res.status, ok: res.ok, data };
}

async function main() {
  console.log("==========================================================");
  console.log(" 🎾 Tenis Ahora — Script de Creación de Datos de Prueba");
  console.log("==========================================================");
  console.log(`🔗 Conectando a: ${BASE_URL}\n`);

  // 1. Iniciar sesión como administrador
  console.log("1️⃣ Iniciando sesión como Administrador...");
  let tokenAdmin = null;
  try {
    const loginRes = await requestJson("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(ADMIN_USER),
    });

    if (!loginRes.ok) {
      console.error("❌ Error al iniciar sesión:", loginRes.data?.message || loginRes.data);
      console.error("Asegúrate de que el backend esté ejecutándose con 'admin@tenisahora.com'.");
      process.exit(1);
    }
    tokenAdmin = loginRes.data.accessToken;
    console.log("   ✅ Sesión iniciada correctamente.");
  } catch (err) {
    console.error("❌ No se pudo conectar con el backend:", err.message);
    console.error("   Asegúrate de que el backend esté corriendo en http://localhost:8080");
    process.exit(1);
  }

  const authHeader = { Authorization: `Bearer ${tokenAdmin}` };

  // 2. Obtener canchas disponibles
  console.log("\n2️⃣ Consultando canchas registradas...");
  let canchas = [];
  const canchasRes = await requestJson("/api/canchas?size=50", { headers: authHeader });
  if (!canchasRes.ok) {
    console.error("❌ No se pudieron consultar las canchas:", canchasRes.data);
    process.exit(1);
  }
  const listaCanchas = Array.isArray(canchasRes.data?.contenido)
    ? canchasRes.data.contenido
    : Array.isArray(canchasRes.data)
      ? canchasRes.data
      : [];
  canchas = listaCanchas.filter((c) => c.activa !== false);
  if (canchas.length === 0) {
    console.error("❌ No se encontraron canchas activas en el backend. No se pueden garantizar las capacidades.");
    process.exit(1);
  }
  console.log(`   ✅ Canchas encontradas (${canchas.length}): ${canchas.map((c) => `${c.identificador} (cap: ${c.capacidad}, id: ${c.id})`).join(", ")}`);

  // 3. Obtener insumos para asociar
  let insumos = [];
  const insumosRes = await requestJson("/api/insumos?size=50", { headers: authHeader });
  if (insumosRes.ok) {
    const lista = Array.isArray(insumosRes.data?.contenido)
      ? insumosRes.data.contenido
      : Array.isArray(insumosRes.data)
        ? insumosRes.data
        : [];
    insumos = lista.filter((i) => i.activo !== false);
    console.log(`   ✅ Insumos activos encontrados: ${insumos.length} (${insumos.map((i) => i.nombre).join(", ")})`);
  } else {
    console.error("❌ No se pudieron consultar los insumos:", insumosRes.data);
    process.exit(1);
  }
  const red = insumos.find((insumo) => insumo.nombre?.toLowerCase().includes("red"));
  if (!red) {
    console.error("❌ No hay una red de tenis activa en el inventario; no se crearán reservas sin ese insumo.");
    process.exit(1);
  }

  // 4. Crear los 8 socios y completar sus datos de perfil
  console.log("\n3️⃣ Registrando y actualizando los 8 socios...");
  const sociosListos = [];

  for (const socioData of NUEVOS_SOCIOS) {
    const { edad, sexo, ...datosRegistro } = socioData;
    const fechaNacimiento = fechaNacimientoParaEdad(edad);
    let socioId;

    // Buscar si ya existe para evitar agotar el límite de peticiones de registro
    const buscarRes = await requestJson(`/api/socios?dni=${socioData.dni}&size=50`, {
      headers: authHeader,
    });
    const encontrado = buscarRes.ok ? buscarRes.data?.contenido?.find((s) => s.dni === socioData.dni) : null;

    if (encontrado?.id) {
      socioId = encontrado.id;
      console.log(`   ℹ️ Socio ya registrado: ${socioData.nombre} ${socioData.apellido} (DNI ${socioData.dni}, id: ${socioId})`);
    } else {
      const regRes = await requestJson("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({ ...datosRegistro, password: CLAVE_SOCIOS }),
      });

      if (regRes.status === 201) {
        socioId = regRes.data.id;
        console.log(`   ➕ Socio creado: ${socioData.nombre} ${socioData.apellido} (DNI ${socioData.dni}, id: ${socioId})`);
      } else {
        console.error(`❌ No se pudo registrar a ${socioData.nombre}: ${regRes.data?.message || JSON.stringify(regRes.data)}`);
        process.exit(1);
      }
    }

    const loginSocio = await requestJson("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email: socioData.email, password: CLAVE_SOCIOS }),
    });
    if (!loginSocio.ok) {
      console.error(`❌ No se pudo iniciar sesión como ${socioData.email}:`, loginSocio.data);
      process.exit(1);
    }

    const perfilRes = await requestJson("/api/me", {
      method: "PATCH",
      headers: { Authorization: `Bearer ${loginSocio.data.accessToken}` },
      body: JSON.stringify({ fechaNacimiento, sexo }),
    });
    if (!perfilRes.ok) {
      console.error(`❌ No se pudo actualizar fecha de nacimiento y sexo de ${socioData.nombre}:`, perfilRes.data);
      process.exit(1);
    }

    console.log(`   ✅ Perfil actualizado: ${socioData.nombre} ${socioData.apellido}, ${edad} años, ${sexo}, nacimiento ${fechaNacimiento}.`);
    sociosListos.push({ id: socioId, ...socioData, fechaNacimiento });
  }

  const sociosValidos = sociosListos;

  // 5. Determinar las 4 fechas a programar
  console.log("\n4️⃣ Calculando los 4 días a programar...");
  const ahora = new Date();
  const horaActualStr = `${String(ahora.getHours()).padStart(2, "0")}:${String(ahora.getMinutes()).padStart(2, "0")}`;

  // Si hoy ya pasaron los horarios disponibles, arrancamos desde mañana
  const slotsHoyFuturos = HORARIOS_SLOTS.filter((h) => h > horaActualStr);
  let offsetDias = 0;
  if (slotsHoyFuturos.length === 0) {
    console.log(`   ℹ️ La hora actual (${horaActualStr}) ya superó los turnos del club de hoy (hasta ${HORARIOS_SLOTS[HORARIOS_SLOTS.length - 1]} hs).`);
    console.log("   ℹ️ Se programarán 4 días completos a partir de MAÑANA.");
    offsetDias = 1;
  } else {
    console.log(`   ℹ️ Se programará a partir de HOY (${slotsHoyFuturos.length} turnos restantes hoy) y los siguientes días.`);
  }

  const dias = [];
  for (let i = 0; i < 4; i++) {
    const d = new Date();
    d.setDate(d.getDate() + offsetDias + i);
    dias.push({
      fechaStr: formatearFecha(d),
      esHoy: offsetDias === 0 && i === 0,
    });
  }

  console.log(`   📅 Fechas seleccionadas: ${dias.map((d) => d.fechaStr).join(", ")}`);

  // 6. Generar 2 reservas por cada cancha para cada día
  console.log("\n5️⃣ Generando reservas con una red y cobrando sus señas...");
  let reservasCreadas = 0;
  let reservasOmitidas = 0;
  let socioIndex = 0;

  for (const dia of dias) {
    console.log(`\n--- 📆 Fecha: ${dia.fechaStr} ---`);

    for (const cancha of canchas) {
      const capacidad = Number(cancha.capacidad);
      const cantidadJugadores = capacidad >= 4 ? 4 : capacidad >= 2 ? 2 : 0;
      if (cantidadJugadores === 0) {
        console.log(`   ⏭️ [${cancha.identificador}] omitida: capacidad inválida para una reserva de 2 o 4 personas.`);
        reservasOmitidas += HORARIOS_SLOTS.length;
        continue;
      }

      for (let s = 0; s < HORARIOS_SLOTS.length; s++) {
        const horaInicio = HORARIOS_SLOTS[s];

        // Si es hoy y el horario ya pasó, omitirlo para evitar error de negocio
        if (dia.esHoy && horaInicio <= horaActualStr) {
          console.log(`   ⏭️ [${cancha.identificador}] ${horaInicio} omitido porque ya pasó la hora.`);
          reservasOmitidas++;
          continue;
        }

        const titular = sociosValidos[socioIndex % sociosValidos.length];
        const acompaniantes = Array.from({ length: cantidadJugadores - 1 }, (_, index) => {
          const socio = sociosValidos[(socioIndex + index + 1) % sociosValidos.length];
          return {
            personaId: socio.id,
            nombre: socio.nombre,
            apellido: socio.apellido,
            documento: socio.dni,
          };
        });
        socioIndex = (socioIndex + cantidadJugadores) % sociosValidos.length;
        const pedidoInsumos = [{ insumoId: red.id, cantidad: 1 }];

        const payloadReserva = {
          canchaId: cancha.id,
          fecha: dia.fechaStr,
          horaInicio: horaInicio,
          duracionHoras: 1,
          titularId: titular.id,
          acompaniantes,
          insumos: pedidoInsumos,
        };

        const resReserva = await requestJson("/api/reservas", {
          method: "POST",
          headers: authHeader,
          body: JSON.stringify(payloadReserva),
        });

        if (resReserva.status === 201) {
          const cod = resReserva.data?.codigo || resReserva.data?.numero || `R-${resReserva.data?.id}`;
          const cobroRes = await requestJson(`/api/reservas/${resReserva.data.id}/cobros`, {
            method: "POST",
            headers: authHeader,
            body: JSON.stringify({ medioPago: "EFECTIVO" }),
          });
          if (!cobroRes.ok || cobroRes.data?.estado !== "PAGADO") {
            console.error(`   ❌ [${cancha.identificador}] ${horaInicio} - ${cod} creada, pero no se pudo abonar la seña:`, cobroRes.data);
            process.exit(1);
          }
          console.log(`   ✅ [${cancha.identificador}] ${horaInicio} - ${cod} | ${cantidadJugadores} jugadores | 1 red | seña PAGADA en efectivo.`);
          reservasCreadas++;
        } else if (resReserva.status === 409) {
          console.log(`   ⚠️ [${cancha.identificador}] ${horaInicio} ya tiene reserva en ese horario.`);
          reservasOmitidas++;
        } else {
          console.log(`   ❌ [${cancha.identificador}] ${horaInicio} error: ${resReserva.data?.message || JSON.stringify(resReserva.data)}`);
          reservasOmitidas++;
        }
      }
    }
  }

  console.log("\n==========================================================");
  console.log(" 🎉 PROCESO FINALIZADO");
  console.log("==========================================================");
  console.log(`👥 Socios listos: ${sociosValidos.length}`);
  console.log(`📅 Reservas creadas: ${reservasCreadas}`);
  if (reservasOmitidas > 0) {
    console.log(`⚠️ Turnos omitidos o ya ocupados: ${reservasOmitidas}`);
  }
  console.log("\nYa podés ingresar al frontend en http://localhost:5173/reservas para verlas.");
}

main().catch((err) => {
  console.error("Error inesperado en script:", err);
  process.exit(1);
});
