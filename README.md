# ⛏️ InfinityPickaxes

<p align="center">
  <b>Leveling pickaxes for Paper 26.2 (Java 25) with EcoEnchants integration, interactive book sockets, and duplicate quarantine.</b>
</p>

---

## ✨ Características Principales

* ⛏️ **Picos Administrados Irrompibles:** Los Infinity Pickaxes generados por el plugin son irrompibles por defecto.
* 🔄 **Conversión Vanilla Opcional:** La conversión automática está desactivada por defecto para que un pico cualquiera no entre al sistema accidentalmente.
* 📈 **Sistema de Nivel Progresivo (0 a 100):** Gana experiencia minando bloques configurables (`blocks.yml`). Incluye protección anti-exploit para bloques colocados por jugadores.
* 🔮 **Integración Nativa con EcoEnchants:** EcoEnchants 2026.33 aporta los encantamientos personalizados; Fortune y Silk Touch se gestionan como excepciones vanilla.
* 📖 **Mejora Interactiva por Libros:** Selecciona un libro del mismo nivel y pulsa el socket correspondiente para subirlo de nivel.
* 🧩 **Progresión de Sockets:** Los niveles 0/10/25/50/75 permiten 3/4/6/8/10 encantamientos gestionados. Efficiency XX es gratuito; Fortune y Silk Touch consumen un socket.
* ✦ **LimitBreak por Hitos:** Se desbloquea al nivel 50 con límites extra configurables por nivel (+1/+3/+5 por defecto).
* 🛡️ **Protección contra Duplicados:** Los UUID duplicados quedan en cuarentena hasta que un administrador conserva y reasigna el ejemplar legítimo.
* 🎨 **Soporte para TexturePacks & Bridges:**
  * Compatible de forma nativa con `CustomModelData` y fuentes de texturas GUI en títulos.
  * Compatible con **zMenu**, **EcoMenus** y **DeluxeMenus** mediante bridge de comandos y PlaceholderAPI.

---

## 📋 Comandos y Permisos

| Comando | Permiso | Descripción |
| :--- | :--- | :--- |
| `/ipickaxe` | `infinitypickaxes.use` | Abre el menú interactivo del pico sostenido en la mano. |
| `/ipickaxe give <jugador> [nivel]` | `infinitypickaxes.admin` | Entrega un Infinity Pickaxe a un jugador con nivel inicial. |
| `/ipickaxe setlevel <jugador> <nivel>` | `infinitypickaxes.admin` | Modifica directamente el nivel del pico sostenido. |
| `/ipickaxe addxp <jugador> <cantidad>` | `infinitypickaxes.admin` | Añade experiencia de minado al pico del jugador. |
| `/ipickaxe reload` | `infinitypickaxes.admin` | Recarga todas las configuraciones, menús y registros en caliente. |
| `/ipickaxe duplicate ...` | `infinitypickaxes.admin.duplicates.*` | Audita, pone en cuarentena y resuelve UUID duplicados. |

---

## 🛡️ Alcance de la detección de duplicados

La detección es heurística: pone en cuarentena UUID que aparecen simultáneamente en inventarios de jugadores conectados, cofres de Ender, almacenamientos físicos abiertos, contenedores anidados o ítems arrojados. No mantiene una revisión canónica y no puede detectar dos copias que nunca sean observables al mismo tiempo, como un inventario desconectado y un cofre que permanece cerrado.

Los inventarios GUI arbitrarios de otros plugins no se escanean. Las solicitudes automáticas relevantes se agrupan para evitar ejecutar numerosos escaneos globales durante una ráfaga de eventos.

## 🔮 Política de encantamientos

`enchants.yml` se sincroniza de forma aditiva al iniciar y recargar: cada EcoEnchant compatible, Fortune y Silk Touch reciben una entrada, pero las ediciones del administrador y las entradas huérfanas nunca se sobrescriben ni eliminan. EcoEnchants y Bukkit conservan autoridad sobre claves reales, objetivos, conflictos nativos, metadatos y máximos nativos.

Los administradores pueden habilitar o deshabilitar sockets, definir el nivel de desbloqueo, reducir el máximo efectivo y añadir conflictos simétricos. Los picos que ya superen una capacidad o política nueva se conservan sin eliminar encantamientos; simplemente no pueden añadir otro socket hasta recuperar capacidad.

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
