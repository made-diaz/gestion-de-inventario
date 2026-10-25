# Prompt completo para Lovable — Sistema de tienda (estética café/cosméticos asiático)

Cómo usarlo: pega primero el PROMPT MAESTRO + ETAPA 1 en Lovable. Cuando termine y lo revises, pega la ETAPA 2, luego la 3 y la 4, una por una. Antes de la Etapa 3 reemplaza el bloque marcado con [COMPLETAR] por el JSON real de tu POST /api/ventas.

---

## PROMPT MAESTRO + ETAPA 1 (pegar primero)

```
Construye el frontend de un sistema administrativo de tienda (inventario, clientes y punto de venta) con React + Vite + TypeScript + Tailwind + shadcn/ui.

## REGLAS TÉCNICAS (importantes)
- NO uses Supabase ni crees backend, base de datos ni autenticación propios. Todos los datos vienen de una API REST externa ya existente.
- URL base de la API en variable de entorno: VITE_API_URL (por defecto http://localhost:8080).
- Crea un único cliente en src/lib/api.ts (fetch) que:
  - agregue el header Authorization: Bearer <token> en cada request,
  - ante un 401 borre la sesión y redirija a /login con el aviso suave "Tu sesión venció, vuelve a ingresar 🌸",
  - ante un 403 muestre el mensaje amable "No tienes permiso para esta acción" (NO cierres sesión; 401 y 403 son cosas distintas),
  - lea los errores del backend, que siempre vienen como { "estado": number, "mensaje": string }, y muestre ese "mensaje" tal cual en un toast. No inventes textos de error propios, salvo los diálogos 409 descritos abajo.
- Usa React Router, TanStack Query para datos, react-hook-form + zod para formularios.
- Todo el texto de la interfaz en español de Chile. Moneda en pesos chilenos (formato $12.990, sin decimales). Los RUT se muestran con formato 12.345.678-9.
- El servidor calcula precios, IVA y totales de las ventas. El frontend NUNCA recalcula el total oficial: solo puede mostrar un subtotal estimado en el carrito y reemplazarlo por lo que devuelva la API.
- Si un endpoint no existe aún, deja el botón deshabilitado con tooltip "Próximamente" en lugar de inventar la llamada.
- Centraliza todos los tipos en src/types/api.ts. Usa los nombres de campo que devuelva la API tal cual; si dudas de alguno, deja la adaptación en ese solo archivo para corregirlo fácil.

## CONTRATO DE LA API
Auth:
- POST /api/auth/login  body: { "usuario": string, "contrasena": string }
  respuesta: { "token": string, "rol": "ADMIN" | "VENDEDOR", "expiraEnSegundos": number }
  Guarda token, rol y hora de expiración en localStorage. El token dura 2 horas y NO existe refresh.

Recursos (todos requieren token):
- /api/productos               GET (lista), GET /{id}, POST, DELETE /{id}
- /api/categorias              GET, GET /{id}, POST, DELETE /{id}
- /api/clientes                GET, GET /{id}, POST, DELETE /{id}
- /api/ventas                  GET, GET /{id}, POST
- /api/pagos                   GET, POST  body: { "metodoPago": "EFECTIVO", "montoPago": 2380, "venta": { "idVenta": 5 } }
- /api/documentos-tributarios  GET, POST (campos: folio, estadoSii, xml, trackId)
- /api/movimientos-stock       GET, GET /{id}  (solo auditoría, solo lectura)
- /api/usuarios                GET, POST, DELETE /{id}  (SOLO rol ADMIN)

Limitaciones actuales del backend (el frontend debe convivir con ellas):
- NO existen PUT/PATCH: los botones "Editar" van deshabilitados con tooltip "Próximamente".
- NO existe paginación, búsqueda ni filtros en servidor: todo GET devuelve la lista completa. Busca, filtra y pagina en el cliente.
- NO existe endpoint de resumen para dashboard.
- GET /api/ventas todavía no trae el detalle de líneas ni los pagos de cada venta. Si esos campos no vienen, muestra "Detalle no disponible aún" en lugar de fallar.
- Una venta aún no genera su documento tributario automáticamente: si no hay documento asociado, muestra "Sin documento" sin error.

Errores 409 (diálogo explicativo y amable, no error genérico):
- Borrar producto con ventas: "Este producto ya tiene ventas registradas, por eso no se puede eliminar. Así cuidamos tu historial 🌷"
- Borrar categoría con productos: "Esta categoría todavía tiene productos. Muévelos o elimínalos primero 🌿"
- Borrar usuario con ventas: "Este usuario tiene ventas registradas, por eso no se puede eliminar 🌸"

## DISEÑO: "Tienda de cosméticos × café asiático"
Estética kawaii-elegante, dulce y colorida, inspirada en cafés y tiendas de skincare de Japón y Corea, con toques de papelería china. Cálida, femenina y acogedora, aunque sea un sistema administrativo serio. Nada de apariencia corporativa gris.

Paleta (tokens CSS/Tailwind, modo claro):
- Fondo: crema leche #FFF8F3, con degradado muy suave a rosa pétalo #FFEFF3
- Primario: rosa sakura #F4A6B8 (hover #EE8EA6), texto sobre primario #5A2A3A
- Pasteles secundarios: matcha #CFE8C9, durazno #FFD8BE, lavanda #DCD0F5, celeste nube #CDE7F5, mantequilla #FFF1B8
- Texto principal: ciruela suave #4A3340 (nunca negro puro); secundario #8A7080
- Acento dorado discreto #E8C98A
- Peligro: coral suave #F28B82 (no rojo agresivo)

Tipografía (Google Fonts): títulos "Zen Maru Gothic", cuerpo "Nunito".

Componentes:
- Esquinas muy redondeadas (rounded-2xl a rounded-3xl), sombras suaves y difusas con tinte rosado, bordes de 1px en rosa claro.
- Tarjetas tipo "cajita" con color pastel por sección: productos = durazno, clientes = lavanda, ventas = rosa, categorías = matcha, stock = celeste, pagos = mantequilla.
- Botones píldora; el primario en rosa sakura con micro-animación al presionar. Badges y chips en pasteles.
- Tablas con filas espaciadas, cebreado muy sutil, cabecera en crema rosada, hover suave, sin líneas duras.
- Iconos lucide-react de trazo fino. Adornos sutiles y con moderación (pétalos de sakura, nubecitas, estrellitas, puntitos) en SVG propio. NO uses personajes ni mascotas con derechos de autor; si hace falta una mascota, crea una original muy simple (un gatito o una taza de té sonriente).
- Estados vacíos con ilustración SVG simple y tierna + mensaje cariñoso ("Aún no hay productos, ¡agrega el primero! 🌸").
- Toasts amables y sin dramatismo. Transiciones suaves (200–300 ms). El texto siempre debe ser claramente legible sobre los pasteles (contraste suficiente).
- Responsive: usable en tablet y celular (el POS se usará en tablet). Modales se convierten en drawers en móvil.

## ETAPA 1 (hazla ahora; NO construyas las demás todavía)
1. Sistema de diseño completo: tokens, fuentes y componentes shadcn personalizados con este estilo.
2. Pantalla de Login: tarjeta centrada sobre fondo degradado crema-rosa con pétalos decorativos, nombre de la tienda editable en un único archivo de configuración (por ahora "Mi Tiendita"), campos usuario y contraseña, botón "Ingresar", error mostrado con el mensaje del backend.
3. Layout principal (shell): barra lateral colapsable con iconos y secciones coloridas + cabecera con saludo ("Hola, {usuario} 🌸"), chip del rol y botón de cerrar sesión. La sección "Usuarios" solo se muestra a ADMIN; si un VENDEDOR entra a /usuarios por URL, redirígelo con aviso amable.
4. Rutas protegidas (sin token válido o expirado → /login), cliente api.ts con los interceptores 401/403, y páginas con estado vacío bonito para: Inicio, Productos, Categorías, Clientes, Nueva venta, Ventas, Pagos, Documentos, Movimientos de stock, Usuarios.
5. Aviso discreto cuando falten ~10 minutos para que expire el token: "Tu sesión vence pronto".
```

---

## ETAPA 2 — Productos, Categorías y Clientes

```
Construye las pantallas de Productos, Categorías y Clientes con el mismo sistema de diseño.

Productos (color durazno):
- Tabla con buscador por nombre/código (en el cliente), filtro por categoría, orden por columna y paginación en cliente (10–20 por página).
- Muestra stock con chip de color: stock bajo (≤5) en durazno intenso, sin stock en coral suave, normal en matcha.
- Botón "Nuevo producto" abre un modal (drawer en móvil) con formulario validado con zod (nombre, precio, stock, categoría, etc., según los campos que devuelva la API) y hace POST /api/productos.
- Borrar con confirmación (DELETE). Si responde 409, muestra el diálogo explicativo definido en las reglas.
- Botón "Editar" visible pero deshabilitado con tooltip "Próximamente".

Categorías (color matcha):
- Lista en tarjetas pequeñas tipo etiqueta con nombre y cantidad de productos (calculada en el cliente), crear en modal, borrar con confirmación y diálogo 409.
- Editar deshabilitado con "Próximamente".

Clientes (color lavanda):
- Tabla con buscador por nombre o RUT (en el cliente). El RUT se muestra formateado 12.345.678-9 y se valida con dígito verificador al crear.
- Crear en modal (POST /api/clientes), borrar con confirmación.
- Editar deshabilitado con "Próximamente".

En todas: skeletons suaves al cargar, estado vacío tierno, y errores con el "mensaje" del backend en un toast.
```

---

## ETAPA 3 — Nueva venta (POS)

> **[COMPLETAR]** Reemplaza el bloque "Body de POST /api/ventas" por el JSON real de tu backend. El de abajo es solo un ejemplo supuesto.

```
Construye la pantalla "Nueva venta" (POS), optimizada para tablet, en dos columnas.

Columna izquierda — catálogo:
- Buscador grande por nombre o código y chips de categorías para filtrar.
- Grilla de tarjetas de producto coloridas (pastel según categoría) con nombre, precio y stock. Tocar una tarjeta la agrega al carrito. Si no hay stock, la tarjeta se ve apagada y no se puede agregar.

Columna derecha — carrito:
- Líneas con nombre, precio unitario, control de cantidad (+/−) y botón quitar. No permitas superar el stock disponible.
- Selector de cliente OPCIONAL con búsqueda por RUT o nombre, y opción "Sin cliente".
- Selector de tipo de documento (según los valores que acepte la API).
- Muestra un "Subtotal estimado" claramente rotulado como estimado.
- Botón grande "Confirmar venta".

Al confirmar:
- Haz POST /api/ventas. Body de POST /api/ventas: 
  [COMPLETAR con el JSON real. Ejemplo supuesto:
   { "tipoDocumento": "BOLETA", "cliente": { "idCliente": 3 }, "detalles": [ { "producto": { "idProducto": 1 }, "cantidad": 2 } ] } ]
- NO envíes precios ni totales: el servidor los calcula.
- Con la respuesta, muestra una pantalla/modal de éxito tipo ticket con los totales EXACTOS que devolvió la API (neto, IVA, total), con un detalle bonito y un botón "Nueva venta" que limpia el carrito.
- Si la API responde error (por ejemplo stock insuficiente), muestra el "mensaje" del backend tal cual y conserva el carrito.
- Después de vender, invalida la caché de productos para refrescar el stock.
```

---

## ETAPA 4 — Ventas, Pagos, Documentos, Movimientos, Usuarios y Dashboard

```
Construye las pantallas restantes.

Ventas (color rosa):
- Tabla con fecha, cliente, usuario, tipo de documento y total. Filtros en el cliente por rango de fechas, cliente y usuario; paginación en cliente.
- Al hacer clic en una fila, abre un detalle tipo ticket (drawer lateral) con encabezado de la tienda, datos de la venta y totales. Si la API no trae las líneas, muestra "Detalle de productos no disponible aún" en lugar de fallar. Si no hay pagos o documento asociados, muestra "Sin pagos registrados" / "Sin documento".

Pagos (color mantequilla): tabla de solo lectura con método, monto y venta asociada. Botón "Registrar pago" que abre un modal con método de pago, monto y selector de venta, y hace POST /api/pagos con el body { "metodoPago", "montoPago", "venta": { "idVenta" } }. Avisa con un texto suave que por ahora el pago se registra manualmente.

Documentos tributarios (color celeste): tabla de solo lectura con folio, estado SII (chip de color por estado) y trackId. El XML se ve en un modal con scroll, en fuente monoespaciada y con botón "Copiar".

Movimientos de stock (color celeste nube): tabla de auditoría de solo lectura con buscador, filtro por tipo de movimiento y rango de fechas en el cliente, paginación en cliente. Sin botón de crear.

Usuarios (solo ADMIN): tabla con nombre, rol (chip) y estado. Crear usuario en modal, borrar con confirmación y diálogo 409. Botón "Deshabilitar" y "Cambiar contraseña" visibles pero deshabilitados con "Próximamente".

Inicio / Dashboard: tarjetas grandes y coloridas con "Vendido hoy", "Ventas del día" y "Productos con stock bajo", calculadas EN EL CLIENTE a partir de GET /api/ventas y GET /api/productos (filtrando por la fecha de hoy). Agrega un gráfico de barras simple de ventas de los últimos 7 días con la paleta pastel, y una lista de "Últimas ventas". Muestra un textito discreto: "Cálculo aproximado; pronto vendrá un resumen desde el servidor". Si algún campo no existe, muestra la tarjeta con "—" sin romper.
```

---

## Antes de pegar

1. En Lovable define `VITE_API_URL` con la URL pública del backend (o del túnel ngrok / Cloudflare Tunnel). Lovable no puede llamar a tu `localhost`.
2. En el backend configura CORS para permitir el dominio de Lovable (`*.lovable.app` / `*.lovable.dev`) y `localhost:5173`.
3. Conecta el proyecto a GitHub desde el inicio para poder seguir editando a mano o con Claude Code.
4. Cuando el backend tenga PUT, paginación, detalle de líneas y resumen, pídele a Lovable: "Activa los botones Editar y reemplaza el filtrado en cliente por los parámetros del servidor".
