# Contrato de la API (DIS 06)

La documentación interactiva y siempre actualizada está en `/swagger-ui` (y el JSON en `/v3/api-docs`) con el backend
levantado. Este archivo es el contrato navegable sin levantar nada: formato de error, autenticación, y el detalle de
los endpoints de **Reservas** (BACK 06/07) y **Cobros** (BACK 08), que es lo que hoy dejan terminado esas dos ramas.
El resto de los módulos (socios, canchas, insumos, auth) sigue la misma convención y está documentado en Swagger.

## Autenticación

Todos los endpoints bajo `/api/**` requieren un JWT, salvo `/api/auth/**` (login, registro, refresh) y
`/webhooks/mercadopago`. El token va en el encabezado `Authorization: Bearer <accessToken>` y se obtiene con
`POST /api/auth/login`. Vence a los 30 minutos; `POST /api/auth/refresh` renueva con la cookie de refresh.

## Formato de error

Toda respuesta de error (4xx, 5xx) tiene el mismo cuerpo:

```json
{
  "codigo": "RESERVA_NO_COBRABLE",
  "mensaje": "La reserva está CONFIRMADA: no admite el cobro de la seña",
  "campos": null,
  "fecha": "2026-10-20T19:00:00",
  "ruta": "/api/reservas/10/cobros"
}
```

`campos` solo viene en los 400 de validación (`VALIDACION_FALLIDA`), con un mensaje por cada campo inválido.

| HTTP | Significa |
|---|---|
| 400 | El pedido está mal formado o no pasa las validaciones (`campos` tiene el detalle) |
| 401 | Sin sesión, sesión vencida, o (en el webhook) firma inválida |
| 403 | El rol de la sesión no puede hacer esto |
| 404 | El recurso no existe (o existe pero la persona no puede verlo: nunca se distingue, para no filtrar qué existe) |
| 409 | El pedido choca con el estado actual de algo que ya existe |
| 422 | El pedido es válido pero viola una regla de negocio |
| 429 | Demasiados pedidos desde la misma IP (`Retry-After` dice cuántos segundos esperar) |
| 503 | Una pasarela externa (Mercado Pago) no contestó |

## Reservas (`/api/reservas`)

| Verbo | Ruta | Quién | Éxito | Errores propios |
|---|---|---|---|---|
| POST | `/api/reservas` | SOCIO, EMPLEADO, ADMINISTRADOR | 201 + `Location` | 409 `RESERVA_HORARIO_OCUPADO`/`RESERVA_HORARIO_BLOQUEADO` · 422 `RESERVA_FUERA_DE_PLAZO`, `RESERVA_FECHA_PASADA`, `RESERVA_DURACION_INVALIDA`, `RESERVA_JUGADORES_INVALIDOS`, `RESERVA_FUERA_DE_HORARIO`, `RESERVA_TITULAR_INVALIDO` |
| GET | `/api/reservas/mis-reservas` | SOCIO | 200 página | — |
| GET | `/api/reservas` | EMPLEADO, ADMINISTRADOR | 200 página | — |
| GET | `/api/reservas/{id}` | dueño · personal | 200 | — |
| PATCH | `/api/reservas/{id}` | EMPLEADO, ADMINISTRADOR | 200 | 409 `RESERVA_HORARIO_OCUPADO` · 422 `RESERVA_NO_MODIFICABLE` |
| GET/POST | `/api/reservas/{id}/cancelacion` | dueño (titular) · personal | 200 | 403 (no es el titular) · 422 `RESERVA_NO_CANCELABLE` |
| PUT | `/api/reservas/{id}/insumos` | dueño · personal | 200 | 422 `RESERVA_INSUMOS_INVALIDOS` |
| POST | `/api/reservas/{id}/inicio` | EMPLEADO, ADMINISTRADOR | 200 | 422 `RESERVA_NO_INICIABLE` |
| POST | `/api/reservas/{id}/finalizacion` | EMPLEADO, ADMINISTRADOR | 200 | 422 `RESERVA_NO_FINALIZABLE` |
| GET | `/api/reservas/disponibilidad` | todos | 200 | — |

Una reserva nace `PENDIENTE_PAGO` y solo pasa a `CONFIRMADA` cuando BACK 08 acredita el cobro de la seña — ver abajo.
Solo una reserva `CONFIRMADA` admite `POST .../inicio` (CU14): antes de BACK 08 esto era imposible de probar por API.

## Cobros (`/api/cobros`, `/api/reservas/{id}/cobros`)

El monto de la seña (RD09: 50% del total, con los descuentos vigentes aplicados — RD21) lo calcula siempre el
servidor: ningún request de cobro lleva un monto.

| Verbo | Ruta | Quién | Request | Éxito | Errores propios |
|---|---|---|---|---|---|
| POST | `/api/reservas/{id}/cobros` | EMPLEADO, ADMINISTRADOR · SOCIO (su propia reserva, como titular) | `CobroRequest` | 201 + `Location: /api/cobros/{id}` | 409 `COBRO_YA_REGISTRADO` · 422 `RESERVA_NO_COBRABLE`, `MEDIO_DE_PAGO_INVALIDO`, `CUOTAS_INVALIDAS` · 400 `NUMERO_DE_OPERACION_REQUERIDO` |
| GET | `/api/cobros/{id}` | EMPLEADO, ADMINISTRADOR · dueño | — | 200 `CobroResponse` | — |
| GET | `/api/cobros/{id}/recibo` | EMPLEADO, ADMINISTRADOR · dueño | — | 200 `ReciboResponse` | 404 `RECIBO_NO_EMITIDO` |
| POST | `/api/cobros/{id}/qr` | EMPLEADO, ADMINISTRADOR · dueño | — | 200 `QrResponse` | 422 `COBRO_NO_ADMITE_QR` · 503 `PASARELA_NO_DISPONIBLE` |
| POST | `/webhooks/mercadopago` | público (Mercado Pago) | payload de MP + header `x-signature` | 200 inmediato | 401 firma inválida |

**`CobroRequest`**

```json
{ "medioPago": "DEBITO | CREDITO | QR", "numeroOperacion": "000123", "cuotas": 1, "aprobado": true }
```

`numeroOperacion` es obligatorio para `DEBITO`/`CREDITO` (lo que devuelve la terminal del club). `cuotas` solo
aplica a `CREDITO` (1 a 12 por defecto, configurable por `app.cobros.max-cuotas`). `aprobado` es lo que cargó quien
cobra según lo que mostró la terminal física: en falso el cobro queda `RECHAZADO` y se puede reintentar con otro
medio.

**`CobroResponse`**

```json
{
  "id": 7, "codigo": "C-000007", "reservaId": 10, "concepto": "SEÑA", "estado": "PENDIENTE",
  "medioPago": "QR", "cuotas": null, "montoBase": 8000.00, "montoFinal": 7200.00,
  "fechaExpiracionQr": "2026-10-20T19:10:00", "numeroOperacion": null, "fecha": "2026-10-20T19:00:00",
  "qrData": "https://www.mercadopago.com.ar/checkout/v1/redirect?..."
}
```

`qrData` solo viene completo en la respuesta de la operación que generó el QR (este alta con medio QR, o
`POST .../qr`): no se persiste, así que una consulta posterior (`GET /api/cobros/{id}`) no lo repite — para eso
está `POST /api/cobros/{id}/qr`, que genera uno nuevo.

**Ciclo de vida de un cobro:** `PENDIENTE` → `PAGADO` (acreditado, termina en un recibo y confirma la reserva) o
`RECHAZADO`/`VENCIDO` (se puede reintentar: otro medio, u otro QR). `ANULADO` es un estado final administrativo.

## Webhook de Mercado Pago

`POST /webhooks/mercadopago` está fuera de `/api` a propósito: no es parte de la API del front (por eso no lleva
CORS ni límite por IP) y no usa JWT — su autenticación es la firma HMAC-SHA256 del header `x-signature`, validada
contra `MP_WEBHOOK_SECRET`. Si la firma no es válida responde 401 sin procesar nada. Si es válida, responde 200 de
inmediato y acredita el pago en otro hilo (para no hacer esperar a Mercado Pago); por eso existe también una
conciliación periódica que recupera los pagos cuyo aviso se haya perdido.
