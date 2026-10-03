# SuperOptimizer 26.4

Клиентский Fabric-мод для Minecraft 26.4 Snapshot 2 (`26.4-snapshot-2` / локальная метка Fabric `26.4-alpha.2`).

## Что реально реализовано

- Консервативное entity culling через уже рассчитанный Minecraft список `LevelRenderer.visibleSections()`.
- Консервативное block-entity culling только для заранее разрешённых локальных vanilla-типов.
- 3×3×3 соседняя секция считается видимой: объект рендерится, если он может пересекать границу секции.
- Большие entity, glowing entity, entity с outline/name tag/score/leash не cull-ятся.
- При наличии Iris culling автоматически блокируется по умолчанию.
- При наличии отдельного Entity Culling culling SuperOptimizer автоматически блокируется по умолчанию.
- Ограниченный CPU worker pool подготовлен для независимых CPU-задач; он не пытается заменить GPU.
- Русская конфигурация и меню по F8.
- Shaderpack и GLSL-файлы Super Duper Vanilla не входят в проект и не изменяются.

## Почему архитектура сделана именно так

Minecraft 26.4 использует LevelRenderState, где entity и block-entity уже представлены как render-state объекты, а LevelRenderer отправляет их через submitEntities и submitBlockEntities. SuperOptimizer вмешивается только в этот слой и не заменяет Vulkan renderer.

Для visibility используется уже существующий список visibleSections, который формируется самим Minecraft. Это позволяет не запускать второй собственный occlusion graph и не читать изменяемый world state из worker threads.

## Vulkan / Iris

SuperOptimizer не вызывает OpenGL или Vulkan напрямую. Iris не объявляется Vulkan-совместимым. При обнаружении Iris culling автоматически выключается, чтобы не вмешиваться в shader-specific render passes.

## Проверки CI

Каждый build:

1. Собирается на Java 25.
2. Использует реальный Minecraft client jar 26.4.
3. Проверяет API LevelRenderer/EntityRenderDispatcher/секций.
4. Проверяет production JAR и Java 25 bytecode.
5. Проверяет, что старого MinecraftClientTickMixin нет.
6. Принудительно загружает LevelRenderer через game classloader, чтобы Mixin-transform был принят.
7. Запускает headless client smoke test.

Headless smoke test подтверждает загрузку Fabric, initializer и Mixin-transform. Он не заменяет тест на реальном Windows/Vulkan драйвере.

## Сборка

Требуется Java 25.

    gradle --no-daemon clean build

Production JAR находится в `build/libs/`.