# Конфигурация действующего Jenkins

Файлы взяты из рабочего проекта QA_Item_Bro и действующего окружения Jenkins.

- `Jenkinsfile` задаёт этапы сборки и параметры ALL/API/UI, 1–4 процесса.
- `ci/compose.workers.yaml` описывает отдельные PostgreSQL, backend и frontend для каждого процесса.
- `ci/workers.sh` поднимает стенды, собирает логи и удаляет контейнеры.
- `server/Dockerfile` содержит JDK 27, Docker CLI, Node.js и Allure CLI для Jenkins.
- `server/compose.yaml` запускает Jenkins и Selenoid. Данные Jenkins сохраняются в volume `jenkins-home`.
- `server/selenoid/browsers.json` задаёт образ Chrome 128.0.
- `server/plugins.txt` содержит версии плагинов, установленных в действующем Jenkins.

В текущем окружении уже настроена задача Pipeline из SCM: https://github.com/Kriksson/QA_Item_Bro.git, ветка master, путь Jenkinsfile. Образы item-bro-app:latest, item-bro-ui-frontend:latest и selenoid/vnc_chrome:128.0 подготовлены заранее. Используется сеть item-bro_default.

Dockerfile устанавливает инструменты. Плагины и настройка задачи хранятся в Jenkins home и не создаются автоматически этим Dockerfile. Перечень плагинов приложен отдельно, для переноса действующего окружения.

В server/compose.yaml указан GID Docker socket 964 из текущего стенда. На другом компьютере следует указать группу его Docker socket. Путь к JDK 27 в pipeline: /opt/jdk-27.

Пароли Jenkins, токены и данные Jenkins home в архив не включены.
