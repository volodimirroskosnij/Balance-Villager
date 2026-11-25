# Balance Villager (`NO VILLAGER`)

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-success?style=for-the-badge&logo=minecraft)
![Fabric Loader](https://img.shields.io/badge/Fabric_Loader-0.20.1+-dbd0b4?style=for-the-badge)
![Fabric API](https://img.shields.io/badge/Fabric_API-0.92.12+1.20.1-blue?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

**Balance Villager** — мод для **Minecraft 1.20.1 (Fabric)**, який додає глобальний серверний режим **`NO VILLAGER`**, що керується спеціальним предметом **Тотем Жителя** (`balance-villager:villager_totem`). Коли режим увімкнено, жителі (`minecraft:villager`) повністю зникають зі світу та не можуть з'явитися жодним способом.

* **Розробники**: [@volodimirroskosnij](https://github.com/volodimirroskosnij) та [@partyArc](https://github.com/partyArc)
* **Репозиторій**: [github.com/volodimirroskosnij/Balance-Villager](https://github.com/volodimirroskosnij/Balance-Villager)
* **Ліцензія**: [MIT](LICENSE)

---

## Вимоги та версії

| Компонент | Версія |
| :--- | :--- |
| **Minecraft** | `1.20.1` |
| **Fabric Loader** | `0.20.1` або новіший |
| **Fabric API** | `0.92.12+1.20.1` (обов'язково) |
| **Java** | `17` або новіша |
| **Мапінги коду** | Official Mojang Mappings |

---

## Предмет: Тотем Жителя (`Villager Totem`)

Керування режимом здійснюється через предмет **`Тотем Жителя (NO VILLAGER)`**:

### Як отримати предмет:
* **Командою в чаті**:
  ```mcfunction
  /give @s balance-villager:villager_totem
  ```
* **У творчому режимі (Creative)**: вкладка **«Інструменти та приладдя»** (*Tools & Utilities*).

### Як працює тотем:
* Натисни **ПКМ (Праву кнопку миші)** з тотемом у руці, щоб увімкнути або вимкнути режим `NO VILLAGER`.
* **Динамічна зміна текстури**:
  * **Режим ВИМКНЕНО**: тотем виглядає як звичайний житель, над хотбаром виводиться зелене повідомлення:
    `Режим NO VILLAGER: ВИМКНЕНО (спавн жителів дозволено)`
  * **Режим УВІМКНЕНО**: у жителя на тотемі з'являються роги, насуплені брови та червоні очі, а над хотбаром виводиться червоне повідомлення:
    `Режим NO VILLAGER: УВІМКНЕНО (видалено жителів: X)`
* Стан тотема автоматично синхронізується між сервером та всіма клієнтами (`sync_no_villager_state`), тому текстура предмета змінюється миттєво у руці, інвентарі, скринях та рамках.

---

## Логіка роботи режиму `NO VILLAGER`

Коли режим **`NO VILLAGER` увімкнено**, мод перекриває всі можливі шляхи появи та існування жителів у світі:

1. **Миттєве очищення світу (`discard()`)**:
   * У момент увімкнення мод проходить по всіх завантажених вимірах сервера (`Overworld`, `Nether`, `The End`) і миттєво видаляє всіх жителів через `villager.discard()` (без звуку смерті, дропу чи штрафів репутації).
2. **Очищення старих (далеких) чанків (`ServerEntityEvents.ENTITY_LOAD`)**:
   * Якщо житель був збережений у далекому вивантаженому чанку ще до ввімкнення режиму, то під час підвантаження цього чанка гравцем житель автоматично видаляється через `entity.discard()`.
3. **Повне блокування спавну (`ServerLevelSpawnMixin` & `EntityTypeSpawnMixin`)**:
   * **Яйце призову, спавнери, роздавачі**: перехоплюється `EntityType#create(...)` ще до створення сутності в пам'яті — житель не з'являється, **не видає звуку («Хмм»)** при кліку, а в режимі Виживання яйце призову не витрачається даремно.
   * **Команда `/summon minecraft:villager`, розмноження, портали**: перехоплюється `ServerLevel#addEntity(Entity)` і скасовує додавання жителя у світ.
4. **Генерація нових сіл та структур (`addWorldGenChunkEntities`)**:
   * Під час генерації нових чанків потік сутностей `Stream<Entity>` фільтрується у `ServerLevel#addWorldGenChunkEntities`, тому всі нові села та підвали іглу генеруються одразу без жителів.
5. **Лікування зомбі-жителя**:
   * Якщо вилікувати `ZombieVillager` зіллям слабкості та золотим яблуком при активному режимі `NO VILLAGER`, у момент завершення конвертації новий житель блокується, а сам зомбі-житель зникає (`discard()`).
6. **Збереження стану та `O(1)` оптимізація (`NoVillagerState`)**:
   * Стан зберігається глобально у `SavedData` (`world/data/balance_villager_no_villagers.dat`) і не скидається після перезапуску світу чи сервера.
   * Для максимальної продуктивності стан кешується у `volatile boolean` у оперативній пам'яті (`O(1)` читання без звернень до `DimensionDataStorage` під час генерації чанків та спавну мобів).

---

## Структура проєкту

```text
Balance Villager/
├── src/
│   ├── client/
│   │   └── java/balance/villager/client/
│   │       └── BalanceVillagerClient.java          # Клієнтський пакет синхронізації та ItemProperties предикат текстури
│   └── main/
│       ├── java/balance/villager/
│       │   ├── BalanceVillager.java                # Реєстрація предмета, подій, мережі та логіки очищення
│       │   ├── event/
│       │   │   └── NoVillagerToggleCallback.java   # Кастомна подія перемикання режиму (Fabric Event)
│       │   ├── mixin/
│       │   │   ├── EntityTypeSpawnMixin.java       # Блокування створення жителя та його звуку при спавні
│       │   │   └── ServerLevelSpawnMixin.java      # Блокування addEntity та фільтрація генерації сіл
│       │   └── state/
│       │       └── NoVillagerState.java            # Збереження стану в SavedData + O(1) кеш у пам'яті
│       └── resources/
│           ├── assets/balance-villager/
│           │   ├── icon.png                        # Іконка мода
│           │   ├── lang/
│           │   │   ├── en_us.json                  # Англійська локалізація
│           │   │   └── uk_ua.json                  # Українська локалізація
│           │   ├── models/item/
│           │   │   ├── villager_totem.json         # Модель звичайного тотема + overrides
│           │   │   └── villager_totem_active.json  # Модель активного (рогатого) тотема
│           │   └── textures/item/
│           │       ├── villager_totem.png          # Текстура тотема (режим ВИМКНЕНО)
│           │       └── villager_totem_active.png   # Текстура тотема (режим УВІМКНЕНО)
│           ├── balance-villager.mixins.json
│           └── fabric.mod.json
├── build.gradle
├── gradle.properties
└── settings.gradle
```

---

## Збірка та запуск для розробників

* **Запуск тестового клієнта**:
  ```zsh
  ./gradlew runClient
  ```
* **Збірка готового `.jar` файлу**:
  ```zsh
  ./gradlew build
  ```
  Готовий мод з'явиться за шляхом: `build/libs/balance-villager-1.0.0.jar`.
