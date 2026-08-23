# ⛏️ InfinityPickaxes

<p align="center">
  <b>Advanced Rival-style leveling pickaxes plugin for PaperMC (Java 21) with EcoEnchants addon integration, interactive book sockets, modular perks, and sleek GUIs.</b>
</p>

---

## ✨ Características Principales

* ⛏️ **Picos Irrompibles por Defecto:** Todos los picos generados o convertidos tienen la propiedad `Unbreakable: true`.
* 🔄 **Detección y Conversión Vanilla Automática:** Saca cualquier pico de modo creativo, crafteo o `/give` y el plugin lo convertirá en un *Infinity Pickaxe* al instante manteniendo sus encantamientos existentes.
* 📈 **Sistema de Nivel Progresivo (0 a 100):** Gana experiencia minando bloques configurables (`blocks.yml`). Incluye protección anti-exploit para bloques colocados por jugadores.
* 🔮 **Integración Dinámica con EcoEnchants & Vanilla:**
  * **Eficiencia:** Desbloqueada desde Nivel 0 con soporte escalable hasta **Nivel 25**.
  * **Fortuna:** Desbloqueada desde Nivel 0 con tope inicial en **Nivel 3**.
  * **EcoEnchants:** Soporte nativo para Telepatía, Explosivo, Autofundición, Taladro, Martillo y más.
  * **`level-scaling`:** Límites máximos de encantamiento escalables por nivel de pico configurables.
* 📖 **Mejora Interactiva por Libros:** Arrastra y suelta un libro del mismo nivel en el socket para subirlo de nivel.
* ⚡ **Sistema Modular de Perks:** Desbloqueo de hasta 5 ranuras en niveles 10, 25, 50, 75 y 100 (*Haste Surge*, *AutoSmelt*, *Blast 3x3*, *Fortune Frenzy*, *Void Siphon*).
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
* `%infinitypickaxes_perk_count%` / `%infinitypickaxes_max_perks%` — Perks activos / ranuras disponibles.
* `%infinitypickaxes_enchant_level_<enchant_id>%` — Nivel numérico de un encantamiento.
* `%infinitypickaxes_enchant_roman_<enchant_id>%` — Nivel en números romanos (ej. `XXV`).
* `%infinitypickaxes_has_perk_<perk_id>%` — Estado de un perk (`true`/`false`).
* `%infinitypickaxes_is_holding%` — Si sostiene un Infinity Pickaxe (`true`/`false`).

---

## 🛠️ Compilación

Requiere **Java 21** y **Maven 3.8+**:

```bash
mvn clean package
```

El archivo compilado se generará en `target/InfinityPickaxes-1.0.0.jar`.
