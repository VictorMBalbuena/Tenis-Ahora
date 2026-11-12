# Perfil y configuración

Pantallas para que cada persona vea y edite **sus propios** datos: `/perfil` y
`/configuracion`.

Existen porque hoy nadie puede editarse a sí mismo. `/usuarios`, `/socios` y
`/empleados` exigen rol `EMPLEADO` o `ADMINISTRADOR`, así que un socio o un
profesor no tiene por dónde cambiar su teléfono ni su foto.

Las rutas están protegidas sin `allowedRoles`: cualquiera que inicie sesión
entra a las suyas.

## Contrato que espera el front

Nada de esto existe todavía en el backend. Todo cuelga de `/me` y **no lleva
id**: el backend resuelve quién es por el token, así una persona no puede pedir
el perfil de otra cambiando un número en la URL.

Los errores se esperan con el formato de siempre (DIS 06):
`{ codigo, mensaje, campos, fecha, ruta }`.

### `GET /me`

```json
{
  "id": 12,
  "nombre": "Ana",
  "apellido": "Gómez",
  "dni": "30123456",
  "fechaNacimiento": "1990-05-17",
  "calle": "Av. Siempreviva",
  "numeroCalle": "742",
  "codigoArea": "11",
  "numeroTelefono": "22786604",
  "email": "ana@correo.com",
  "fotoUrl": "https://.../fotos/12.webp",
  "rol": "SOCIO",
  "activo": true,
  "fechaAlta": "2026-03-01",
  "numeroSocio": "S-0012"
}
```

`fotoUrl` puede venir `null`: la pantalla muestra las iniciales.
`numeroSocio` solo para socios; si no viene, la fila no se dibuja.

### `PATCH /me`

Solo los campos que cambiaron, entre estos:

`nombre`, `apellido`, `fechaNacimiento`, `calle`, `numeroCalle`, `codigoArea`,
`numeroTelefono`, `email`.

El **DNI no se puede cambiar desde acá** (identifica a la persona en el club y
sale en el carnet) y el **rol tampoco**. Si llegan, conviene rechazarlos con
422 en vez de ignorarlos en silencio.

Devuelve el perfil completo, igual que el `GET`.

Casos a cubrir:

- correo ya usado por otra persona → **409**
- validaciones de formato → **422** con `campos`

### `POST /me/foto`

`multipart/form-data`, un solo campo: `archivo`.

El front ya corta antes de subir: JPG, PNG o WebP y hasta 2 MB. Conviene
validarlo igual del lado del servidor.

```json
{ "fotoUrl": "https://.../fotos/12.webp" }
```

### `DELETE /me/foto`

Borra la foto y deja `fotoUrl` en `null`. Responde 204.

### `PUT /me/password`

```json
{ "passwordActual": "...", "passwordNueva": "..." }
```

Pide la actual a propósito: si alguien se dejó la sesión abierta, que no pueda
cambiarle la contraseña a otra persona.

El front valida que la nueva tenga entre 8 y 72 caracteres (igual que el
registro) y que sea distinta de la actual.

- contraseña actual incorrecta → **422** con `campos.passwordActual`
- responde 204

Si además revoca las otras sesiones al cambiar la contraseña, mejor, pero la
pantalla no lo asume.

### `GET /me/preferencias` y `PUT /me/preferencias`

```json
{
  "recordatorioReservas": true,
  "recordatorioClases": true,
  "novedadesDelClub": false
}
```

Tres booleanos. El `PUT` recibe los tres y devuelve cómo quedaron. Si al
backend le resulta más cómodo no guardarlos todavía, la pantalla completa con
los valores por defecto lo que no venga.

### `POST /auth/logout-all`

Revoca **todos** los refresh tokens de la persona, no solo el de esta pestaña.
Es para cuando se perdió el teléfono. Responde 204 y el front manda al login.

## Lo que no pasa por el backend

El tema (claro / oscuro / el del sistema) se guarda en el navegador, en
`localStorage`, con la clave `tenisahora-tema`. Es una preferencia del
dispositivo y no de la cuenta: el mostrador puede estar en claro y el teléfono
en oscuro. Vive en `src/lib/tema.js`.
