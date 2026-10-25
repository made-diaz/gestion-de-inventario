# Feature: autorización por roles

Objetivo: cerrar el agujero de que cualquier token válido podía administrar el sistema.
Rama: `feat/roles`. Runner: `.\\gradlew.bat test`.

## Specs

- **S1.** `ADMIN` puede todo: usuarios, catálogo, ventas, pagos, documentos y movimientos.
- **S2.** `VENDEDOR` puede consultar (`GET`) productos, categorías, clientes, pagos, documentos tributarios y movimientos de stock.
- **S3.** `VENDEDOR` puede registrar (`POST`) ventas, pagos y clientes: es la operación de mostrador.
- **S4.** `VENDEDOR` recibe `403` con `{ estado, mensaje }` en todo `/api/usuarios/**` y en cualquier `POST`/`DELETE` de productos o categorías.
- **S5.** Sin token, un endpoint de administración responde `401`, no `403`: primero se autentica, después se autoriza. Ningún `403` debe revelar si el recurso existe.
- **S6.** Un `403` no debe romper el manejo de errores existente: `/error` queda accesible para que el contenedor siga respondiendo `404` donde corresponde.

## Tasks

- **T1.** Matriz de reglas en `SecurityConfig` con constantes de rol. (S1-S4, S6)
- **T2.** Tests de la matriz: vendedor bloqueado en administración, habilitado en mostrador; admin habilitado. (S3-S5)
- **T3.** Ajuste de los tests existentes que asumían un usuario genérico. (S1)

## Log

- **L1.** Usuario: "hazlo y mejora lo que sigue abierto y lo que se rompe" — merge a `main`, push, y cerrar el advisory de autorización por roles.
- **L2.** Decisión de producto: el vendedor registra clientes y cobra, pero no crea ni borra productos, no toca categorías y no administra usuarios. Si querés otro reparto, se cambia solo en `SecurityConfig`.
