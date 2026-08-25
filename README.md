# ⛏️ InfinityPickaxes

<p align="center">
  <b>Leveling pickaxes for Paper 26.2 (Java 25) with EcoEnchants integration, interactive book sockets, and duplicate quarantine.</b>
</p>

---

## ✨ Características Principales

* ⛏️ **Picos Administrados Irrompibles:** Los Infinity Pickaxes generados por el plugin son irrompibles por defecto.
* 🔄 **Conversión Vanilla Opcional:** La conversión automática está desactivada por defecto para que un pico cualquiera no entre al sistema accidentalmente.
* 📈 **Sistema de Nivel Progresivo (0 a 100):** Gana experiencia minando bloques configurables (`blocks.yml`). Incluye protección anti-exploit para bloques colocados por jugadores.
* 🔮 **Integración Nativa con EcoEnchants:** EcoEnchants 2026.33 es la única fuente de encantamientos, efectos, niveles máximos, nombres y descripciones.
* 📖 **Mejora Interactiva por Libros:** Arrastra y suelta un libro del mismo nivel en el socket para subirlo de nivel.
* 🛡️ **Protección contra Duplicados:** Los UUID duplicados quedan en cuarentena hasta que un administrador conserva y reasigna el ejemplar legítimo.
* 🎨 **Soporte para TexturePacks & Bridges:**
  * Compatible de forma nativa con `CustomModelData` y fuentes de texturas GUI en títulos.
  * Compatible con **zMenu**, **EcoMenus** y **DeluxeMenus** mediante bridge de comandos y PlaceholderAPI.

---

## 📋 Comandos y Permisos

| Comando | Permiso | Descripción |
| :--- | :--- | :--- |
| `/pickaxe` | `infinitypickaxes.use` | Abre el menú interactivo del pico sostenido en la mano. |
| `/pickaxe give <jugador> [nivel]` | `infinitypickaxes.admin` | Entrega un Infinity Pickaxe a un jugador con nivel inicial. |
| `/pickaxe setlevel <jugador> <nivel>` | `infinitypickaxes.admin` | Modifica directamente el nivel del pico sostenido. |
| `/pickaxe addxp <jugador> <cantidad>` | `infinitypickaxes.admin` | Añade experiencia de minado al pico del jugador. |
| `/pickaxe reload` | `infinitypickaxes.admin` | Recarga todas las configuraciones, menús y registros en caliente. |
| `/pickaxe duplicate ...` | `infinitypickaxes.admin.duplicates.*` | Audita, pone en cuarentena y resuelve UUID duplicados. |

---

## 🛡️ Alcance de la detección de duplicados

La detección es heurística: pone en cuarentena UUID que aparecen simultáneamente en inventarios de jugadores conectados, cofres de Ender, almacenamientos físicos abiertos, contenedores anidados o ítems arrojados. No mantiene una revisión canónica y no puede detectar dos copias que nunca sean observables al mismo tiempo, como un inventario desconectado y un cofre que permanece cerrado.

Los inventarios GUI arbitrarios de otros plugins no se escanean. Las solicitudes automáticas relevantes se agrupan para evitar ejecutar numerosos escaneos globales durante una ráfaga de eventos.

---

## 🧩 Placeholders (PlaceholderAPI)

* `%infinitypickaxes_level%` — Nivel actual del pico.
* `%infinitypickaxes_max_level%` — Nivel máximo (100).
* `%infinitypickaxes_xp%` — XP acumulada en el nivel actual.
* `%infinitypickaxes_required_xp%` — XP requerida para subir de nivel.
* `%infinitypickaxes_xp_percent%` — Porcentaje de avance de nivel.
* `%infinitypickaxes_xp_bar%` — Barra visual de progreso `[■■■■□□□□]`.
* `%infinitypickaxes_blocks_mined%` — Total de bloques minados.
* `%infinitypickaxes_blocks_mined_formatted%` — Bloques minados con separador de miles.
* `%infinitypickaxes_enchant_count%` / `%infinitypickaxes_max_sockets%` — Sockets ocupados / máximos.
* `%infinitypickaxes_enchant_level_<enchant_id>%` — Nivel numérico de un encantamiento.
* `%infinitypickaxes_enchant_roman_<enchant_id>%` — Nivel en números romanos (ej. `XXV`).
* `%infinitypickaxes_is_holding%` — Si sostiene un Infinity Pickaxe (`true`/`false`).

---

## 🛠️ Compilación

Requiere **Java 25** y un servidor **Linux**. SQLite está incluido en el JAR con binarios nativos para Linux glibc y musl; no descarga dependencias durante el arranque. El wrapper de Gradle descarga la versión correcta automáticamente:

```bash
./gradlew clean build
```

El archivo compilado se genera en `build/libs/InfinityPickaxes-2.0.0-SNAPSHOT.jar`.
