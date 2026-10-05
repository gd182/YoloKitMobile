<h1 align="center">YoloKitMobile</h1>

<p align="center">Небольшое Android-приложение для распознавания объектов в кадре камеры с помощью YOLO.</p>

<div align="center">
  <img src="https://skillicons.dev/icons?i=kotlin" height="40" alt="Kotlin logo" />
  <img width="12" />
  <img src="https://skillicons.dev/icons?i=cpp" height="40" alt="C++ logo" />
  <img width="12" />
  <img src="https://skillicons.dev/icons?i=cmake" height="40" alt="CMake logo" />
  <img width="12" />
  <img src="https://skillicons.dev/icons?i=gradle" height="40" alt="Gradle logo" />
  <img width="12" />
  <img src="https://skillicons.dev/icons?i=androidstudio" height="40" alt="Android Studio logo" />
</div>

<p align="center"><a href="README.md">English</a> · <b>Русский</b></p>

YoloKitMobile распознаёт объекты в кадре камеры и рисует вокруг них рамки.
Это приложение-пример для [YoloKit](https://github.com/gd182/YoloKit):
камера работает через CameraX, а модели можно переключать прямо в приложении.

Для вычислений используется ncnn с Vulkan. Если Vulkan недоступен, модель
работает на CPU. На поддерживаемых устройствах Snapdragon можно использовать
Qualcomm QNN и NPU Hexagon. На экране отображаются выбранный бэкенд,
разрядность модели, FPS и время обработки.

## Сборка и запуск

Клонируйте проект вместе с библиотекой:

```bash
git clone --recurse-submodules https://github.com/gd182/YoloKitMobile.git
cd YoloKitMobile
```

Если проект уже скачан, выполните `git submodule update --init --recursive`.

Откройте его в Android Studio. Для сборки нужны JDK 17, Android SDK 37,
NDK 28.2.13676358 и CMake 3.22.1. В проекте используется AGP 9.4.0.
Приложение работает на Android 7.0 и новее; собираются версии для
`arm64-v8a`, `armeabi-v7a` и `x86_64`.

Сборка и установка из терминала:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Весов моделей в репозитории нет. Без них приложение соберётся, но при загрузке
модели покажет `load failed`. Для распознавания нужно добавить свои файлы.

## Добавление модели

Скопируйте пример конфигурации:

```bash
cp app/src/main/assets/models.example.json app/src/main/assets/models.json
```

Положите файлы модели в `app/src/main/assets/` и укажите их в `models.json`.
Конфигурация и веса исключены из Git. Если `models.json` отсутствует,
приложение читает `models.example.json`.

У каждой модели есть `id`, `displayName` и `backend` (`ncnn` или `qnn`).
В поле `default` укажите ID модели для запуска. Во время работы её можно
сменить через выпадающий список.

| Настройка | Что указать |
|---|---|
| `param` / `bin` | Имена файлов ncnn в assets |
| `model` | Имя файла контекста QNN в assets |
| `inputName` / `outputName` | Имена тензоров ncnn |
| `targetSize` | Размер входа при экспорте (`imgsz`); по умолчанию 640 |
| `decoded` | Декодирует ли ncnn-граф координаты рамок |
| `bgr` | Использует ли ncnn-модель BGR вместо RGB |
| `confThreshold` / `nmsThreshold` | Пороги уверенности и NMS |
| `labels` | Названия классов в порядке обучения: массив или имя файла в assets |
| `boxesNormalized` | Нормализованы ли координаты рамок QNN |

Для экспорта Ultralytics `format=ncnn` укажите `decoded: true`,
`bgr: false`, `inputName: "in0"` и `outputName: "out0"`.
Значение `targetSize` должно совпадать с `imgsz` при экспорте.
Если они различаются, рамки могут смещаться или иметь неверный размер.

Команды экспорта есть в [README папки assets](app/src/main/assets/README.md),
а поддерживаемые выходы моделей описаны в
[README библиотеки](vendor/YoloKit/README.md#model-requirements).

## QNN

Для QNN нужен файл контекста, собранный под целевое устройство Snapdragon.
Этот бэкенд доступен только на `arm64-v8a`. QNN-модели появляются в списке,
только если телефон может их запускать.

Для сборки с QNN укажите `qnn.sdk.dir` в `vendor/YoloKit/local.properties`
или задайте путь к QAIRT SDK в переменной `QNN_SDK_ROOT`.
Версия SDK должна совпадать с версией QNN runtime в YoloKit.
Без SDK приложение собирается только с ncnn.

Конвертация модели и настройка устройства описаны в [QNN.ru.md](docs/QNN.ru.md).

## Код проекта

В `app/` находятся интерфейс камеры, реестр моделей и отрисовка рамок.
Библиотека детекции лежит в `vendor/YoloKit/` и подключается через Gradle
composite build. В `scripts/` находятся скрипты конвертации моделей,
в `docs/` — документация по QNN.

Кадры CameraX преобразуются в ARGB_8888 Bitmap с учётом поворота.
Подготовку изображения, запуск модели, декодирование координат и NMS
выполняет YoloKit. OpenCV не используется.

## Лицензия

Apache-2.0. Условия для проекта и сторонних компонентов приведены в
[LICENSE](LICENSE) и [NOTICE](NOTICE). У весов моделей свои лицензии.
