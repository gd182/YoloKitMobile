# Модели

Положите сюда файлы моделей и укажите их имена в `models.json`.
В репозитории есть пример конфигурации и `labels.txt`; веса нужно добавить отдельно.

Все команды ниже выполняются из корня проекта.

```bash
cp app/src/main/assets/models.example.json app/src/main/assets/models.json
```

`models.json`, `*.param` и `*.bin` исключены из Git. Если локальной
конфигурации нет, приложение использует `models.example.json`.
Без файлов модели оно покажет `load failed`.

## ncnn

Пример экспорта через Ultralytics:

```bash
yolo export model=best.pt format=ncnn imgsz=640 half=True
cp best_ncnn_model/model.ncnn.param best_ncnn_model/model.ncnn.bin app/src/main/assets/
```

В записи модели укажите `param: "model.ncnn.param"` и `bin: "model.ncnn.bin"`.
Для этого экспорта нужны `decoded: true`, `bgr: false`,
`inputName: "in0"`, `outputName: "out0"` и `targetSize: 640`.

Если при экспорте использовался другой `imgsz`, измените `targetSize`.
Названия классов в `labels` должны идти в том же порядке, что и в
`best_ncnn_model/metadata.yaml`. Можно указать массив названий или имя
текстового файла, в котором каждый класс записан с новой строки.

## QNN

Скопируйте собранный файл контекста в эту папку. В записи модели укажите
`backend: "qnn"` и имя файла в `model`, например `model_v81.bin`.
Полную запись можно взять из `models.example.json`.

Инструкция по сборке контекста:
[на русском](../../../../docs/QNN.ru.md) /
[in English](../../../../docs/QNN.md).

Разрядность модели определяется при загрузке; задавать её в конфигурации не нужно.

Условия лицензий приведены в [LICENSE](../../../../LICENSE)
и [NOTICE](../../../../NOTICE).
