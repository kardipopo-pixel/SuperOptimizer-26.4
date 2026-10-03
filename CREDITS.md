# SuperOptimizer — источники архитектурных идей

SuperOptimizer не встраивает исходники Sodium, Iris, Lithium, Entity Culling или More Culling.

Использованные идеи и публичные интерфейсы:

- Sodium: структура настроек «страница → группа → опции», tooltips, dynamic options и идея отдельной страницы конфигурации.
  https://github.com/CaffeineMC/sodium
- Iris: публичный Iris API для обнаружения активного shader pack, shadow-pass safety и открытия штатного shader screen.
  https://github.com/IrisShaders/Iris
- Lithium: идеи уменьшения лишних аллокаций, кэширования горячих запросов и сокращения работы в hot paths.
  https://github.com/CaffeineMC/lithium
- Entity Culling: идея асинхронного/visibility-based culling для невидимых объектов.
  https://github.com/tr7zw/entityculling
- More Culling: идеи расширенного culling разных типов объектов; исходный код этого проекта сюда не переносился.
  https://github.com/fxmorin/MoreCulling

Лицензии и права каждого внешнего проекта принадлежат их авторам. В частности, More Culling прямо запрещает объединять его код в другие клиенты/моды без разрешения автора, поэтому SuperOptimizer реализует собственный код без копирования его исходников.

Для Iris на 26.4 Snapshot 2 SuperOptimizer не обещает собственный Vulkan shader-loader. Кнопка Iris использует официальный API только при наличии совместимой версии Iris.
