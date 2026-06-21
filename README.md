# JWT API — Task Management System

REST API на Spring Boot 4 с JWT-авторизацией, двумя ролями, Liquibase-миграциями и покрытием тестами ≥ 70%.

## Требования

- Java 21+
- Maven 3.9+
- Docker (для запуска интеграционных тестов через Testcontainers)
- PostgreSQL (для локального запуска)

## Запуск

### Создать базу данных PostgreSQL

```sql
CREATE DATABASE jwtapi;
```

### Сборка и запуск

```bash
# Сборка без тестов
mvn clean package -DskipTests

# Запуск
mvn spring-boot:run

# Все тесты
mvn test

# Тесты + проверка покрытия ≥ 70%
mvn verify
```

### Запуск одного теста

```bash
mvn test -Dtest=AuthServiceTest
mvn test -Dtest=AuthServiceTest#register_success
```

Приложение запускается на `http://localhost:8080`.

## Docker

### Сборка образа

Команда одинакова на Linux и Windows — запускать из папки, где лежит `jwt-api/`:

```bash
docker build -t jwt-api:latest jwt-api/
```

Или из самой папки `jwt-api/`:

```bash
docker build -t jwt-api:latest .
```

Сборка трёхступенчатая:
1. **deps** — скачивает Maven-зависимости (слой кэшируется, пока не изменится `pom.xml`).
2. **builder** — компилирует проект и распаковывает JAR по слоям.
3. **runtime** — минимальный образ `distroless/java21`, запуск от non-root пользователя (uid 65532).

### Запуск контейнера

Приложению нужна PostgreSQL. Проще всего запустить обе службы через Docker Compose — команда одинакова на Linux и Windows:

```bash
docker compose up -d
```

Остановить:

```bash
docker compose down
```

После запуска API доступен на `http://localhost:8080`.

#### Альтернатива: запуск через docker run

Если Docker Compose недоступен — однострочные команды без переносов, работают везде.

**Linux / macOS:**
```bash
docker network create jwt-net

docker run -d --name postgres --network jwt-net -e POSTGRES_DB=postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres postgres:16-alpine

docker run -d --name jwt-api --network jwt-net -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/postgres -e SPRING_DATASOURCE_USERNAME=postgres -e SPRING_DATASOURCE_PASSWORD=postgres jwt-api:latest
```

**Windows (CMD и PowerShell):**
```bat
docker network create jwt-net

docker run -d --name postgres --network jwt-net -e POSTGRES_DB=postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres postgres:16-alpine

docker run -d --name jwt-api --network jwt-net -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/postgres -e SPRING_DATASOURCE_USERNAME=postgres -e SPRING_DATASOURCE_PASSWORD=postgres jwt-api:latest
```

### Переменные окружения

| Переменная | По умолчанию | Описание |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/postgres` | JDBC URL базы данных |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Пользователь БД |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Пароль БД |
| `JWT_SECRET` | см. `application.yaml` | Секрет для подписи токенов |
| `JWT_EXPIRATION` | `86400000` | Время жизни токена, мс |

## Kubernetes / Helm

Helm-чарт находится в `infra/chart/`. Поддерживаются два окружения: `dev` и `prod`.

### Требования

- Helm 3+
- `kubectl`, настроенный на нужный кластер
- Docker-образ собран и доступен в registry кластера

### Сборка и публикация образа

```bash
# Собрать образ
docker build -t jwt-api:latest .

# Если используется локальный registry (например, в kind/minikube):
docker tag jwt-api:latest localhost:5000/jwt-api:latest
docker push localhost:5000/jwt-api:latest
```

### Развёртывание в Minikube (пошагово)

Полная инструкция с нуля: от запуска кластера до рабочего API.

#### 1. Запустить Minikube

```bash
minikube start --driver=docker
```

> На Windows рекомендуется драйвер `docker` (Docker Desktop) или `hyperv`.

#### 2. Включить Ingress-аддон

Чарт использует NGINX Ingress Controller:

```bash
minikube addons enable ingress
```

Дождитесь готовности контроллера (~1 минута):

```bash
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s
```

#### 3. Загрузить Docker-образ в Minikube

Minikube использует собственный Docker-демон. Есть два способа:

**Способ А — загрузить готовый образ:**

```bash
# Сначала собрать образ в локальном Docker
docker build -t jwt-api:latest jwt-api/

# Затем загрузить в Minikube
minikube image load jwt-api:latest
```

**Способ Б — собрать образ прямо внутри Minikube (не нужна загрузка):**

Linux/macOS:
```bash
eval $(minikube docker-env)
docker build -t jwt-api:latest jwt-api/
```

Windows (PowerShell):
```powershell
& minikube -p minikube docker-env --shell powershell | Invoke-Expression
docker build -t jwt-api:latest jwt-api/
```

#### 4. Задеплоить PostgreSQL

Чарт рассчитан на внешний PostgreSQL с именем сервиса `postgres`. Быстрый способ — официальный Bitnami-чарт:

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

helm upgrade --install postgres bitnami/postgresql \
  --set auth.postgresPassword=postgres \
  --set auth.database=postgres
```

Дождитесь готовности пода:

```bash
kubectl wait --for=condition=ready pod \
  --selector=app.kubernetes.io/name=postgresql \
  --timeout=120s
```

#### 5. Задеплоить приложение через Helm

```bash
helm upgrade --install jwt-api ./infra/chart \
  -f ./infra/chart/values-dev.yaml \
  --set image.repository=jwt-api \
  --set image.tag=latest \
  --set app.datasource.url=jdbc:postgresql://postgres-postgresql:5432/postgres
```

> Bitnami формирует имя сервиса как `<release>-postgresql`, поэтому после `helm install postgres ...` сервис называется `postgres-postgresql`.

Проверить, что поды и Ingress поднялись:

```bash
kubectl get pods
kubectl get ingress
```

#### 6. Настроить доступ к API

Выберите способ в зависимости от того, как запущен Minikube.

---

##### Способ А — port-forward (работает всегда, рекомендуется для Docker Desktop)

При использовании драйвера `docker` (Docker Desktop на Windows/macOS) IP кластера недоступен напрямую с хоста, поэтому самый простой способ — пробросить порт:

```bash
kubectl port-forward svc/jwt-api 8080:8080
```

Оставьте окно терминала открытым. API будет доступен на `http://localhost:8080`.

---

##### Способ Б — Ingress через minikube tunnel (Docker Desktop)

Если нужно проверить работу Ingress:

1. Откройте отдельный терминал **с правами администратора** и запустите (окно не закрывать):

```bash
minikube tunnel
```

2. Добавьте запись в hosts-файл:

**Linux / macOS:**
```bash
echo "127.0.0.1 jwt-api.dev.local" | sudo tee -a /etc/hosts
```

**Windows** — открыть `C:\Windows\System32\drivers\etc\hosts` в Блокноте от имени администратора и добавить строку:
```
127.0.0.1  jwt-api.dev.local
```

API будет доступен на `http://jwt-api.dev.local`.

---

##### Способ В — Ingress по IP кластера (драйвер не docker: virtualbox, hyperv, kvm2)

```bash
minikube ip
```

Добавить IP в hosts-файл:

**Linux / macOS:**
```bash
echo "$(minikube ip) jwt-api.dev.local" | sudo tee -a /etc/hosts
```

**Windows** — открыть `C:\Windows\System32\drivers\etc\hosts` в Блокноте от имени администратора:
```
<minikube-ip>  jwt-api.dev.local
```

API будет доступен на `http://jwt-api.dev.local`.

---

#### 7. Проверить API

Замените `<base-url>` на адрес из выбранного выше способа:
- Способ А: `http://localhost:8080`
- Способ Б/В: `http://jwt-api.dev.local`

Пример — регистрация:

```bash
curl -X POST <base-url>/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"secret123"}'
```

#### Очистить окружение

```bash
helm uninstall jwt-api
helm uninstall postgres
minikube stop
```

---

### Деплой в dev

```bash
helm upgrade --install jwt-api ./infra/chart \
  -f ./infra/chart/values-dev.yaml \
  --set image.repository=localhost:5000/jwt-api \
  --set image.tag=latest
```

Приложение будет доступно по адресу `http://jwt-api.dev.local` (нужно добавить запись в `/etc/hosts`, если Ingress настроен локально).

### Деплой в prod

Секреты передаются через `--set`, а не хранятся в файлах:

```bash
helm upgrade --install jwt-api ./infra/chart \
  -f ./infra/chart/values-prod.yaml \
  --set image.repository=<your-registry>/jwt-api \
  --set image.tag=<git-sha> \
  --set secrets.datasource.username=$DB_USER \
  --set secrets.datasource.password=$DB_PASS \
  --set secrets.jwt.secret=$JWT_SECRET \
  --set secrets.admin.password=$ADMIN_PASS
```

### Полезные команды

```bash
# Проверить сгенерированные манифесты перед деплоем
helm template jwt-api ./infra/chart -f ./infra/chart/values-dev.yaml

# Статус релиза
helm status jwt-api

# История релизов
helm history jwt-api

# Откат к предыдущей версии
helm rollback jwt-api

# Удалить релиз
helm uninstall jwt-api
```

### Структура чарта

| Файл | Назначение |
|---|---|
| `infra/chart/values.yaml` | Базовые значения по умолчанию |
| `infra/chart/values-dev.yaml` | Переопределения для dev (меньше ресурсов, HPA выключен) |
| `infra/chart/values-prod.yaml` | Переопределения для prod (TLS, HPA, больше реплик) |
| `templates/configmap.yaml` | Нечувствительные настройки приложения |
| `templates/secret.yaml` | Пароли БД, JWT-секрет, пароль админа |
| `templates/deployment.yaml` | Pod с health-пробами и non-root пользователем |
| `templates/service.yaml` | ClusterIP на порту 8080 |
| `templates/ingress.yaml` | Ingress (nginx) |
| `templates/hpa.yaml` | Автомасштабирование по CPU и памяти |

### Health-эндпоинты (Spring Boot Actuator)

| Эндпоинт | Назначение |
|---|---|
| `GET /actuator/health` | Общее состояние (startupProbe) |
| `GET /actuator/health/liveness` | Живо ли приложение (livenessProbe) |
| `GET /actuator/health/readiness` | Готово ли принимать трафик (readinessProbe) |

Все три эндпоинта открыты без авторизации.

## Первый запуск: admin-пользователь

При старте приложение автоматически создаёт пользователя с ролью ADMIN, если его ещё нет в базе.
Учётные данные задаются в `src/main/resources/application.yaml`:

```yaml
admin:
  username: admin
  email: admin@example.com
  password: admin123
```


## Роли

| Роль  | Описание                                                                   |
|-------|----------------------------------------------------------------------------|
| USER  | CRUD своих задач, просмотр категорий                                       |
| ADMIN | Всё то же самое + управление категориями, просмотр всех задач, управление пользователями и их ролями |

## API

### Аутентификация

#### Регистрация
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"secret123"}'
```

Ответ `201 Created`:
```json
{"token": "eyJhbGciOiJIUzI1NiJ9..."}
```

#### Логин
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret123"}'
```

### Задачи (Tasks) — требует JWT

#### Получить все свои задачи
```bash
curl http://localhost:8080/api/tasks \
  -H "Authorization: Bearer <token>"
```

#### Создать задачу
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Купить молоко","description":"2 литра","status":"OPEN"}'
```

#### Обновить задачу
```bash
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Купить молоко","status":"DONE"}'
```

#### Удалить задачу
```bash
curl -X DELETE http://localhost:8080/api/tasks/1 \
  -H "Authorization: Bearer <token>"
```

#### Фильтр по статусу
```bash
curl "http://localhost:8080/api/tasks?status=OPEN" \
  -H "Authorization: Bearer <token>"
```

### Категории (Categories)

#### Получить все категории (USER + ADMIN)
```bash
curl http://localhost:8080/api/categories \
  -H "Authorization: Bearer <token>"
```

#### Получить категорию по ID (USER + ADMIN)
```bash
curl http://localhost:8080/api/categories/1 \
  -H "Authorization: Bearer <token>"
```

#### Поиск категорий по имени (USER + ADMIN)
```bash
curl "http://localhost:8080/api/categories/search?name=Работа" \
  -H "Authorization: Bearer <token>"
```

#### Создать категорию (только ADMIN)
```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Работа","description":"Рабочие задачи"}'
```

#### Обновить категорию (только ADMIN)
```bash
curl -X PUT http://localhost:8080/api/categories/1 \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Работа","description":"Обновлённое описание"}'
```

#### Удалить категорию (только ADMIN)
```bash
curl -X DELETE http://localhost:8080/api/categories/1 \
  -H "Authorization: Bearer <admin-token>"
```

### Пользователи (Users) — только ADMIN

#### Получить всех пользователей
```bash
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer <admin-token>"
```

#### Получить пользователя по ID
```bash
curl http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer <admin-token>"
```

#### Изменить роль пользователя
```bash
curl -X PUT "http://localhost:8080/api/users/1/role?role=ADMIN" \
  -H "Authorization: Bearer <admin-token>"
```

Допустимые значения параметра `role`: `USER`, `ADMIN`.

#### Удалить пользователя
```bash
curl -X DELETE http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer <admin-token>"
```

> Удаление пользователя каскадно удаляет все его задачи.

## Коды ответов

| Код | Ситуация                                  |
|-----|-------------------------------------------|
| 200 | Успешный запрос                           |
| 201 | Ресурс создан                             |
| 204 | Ресурс удалён                             |
| 400 | Ошибка валидации или некорректные данные  |
| 401 | Токен отсутствует или недействителен      |
| 403 | Недостаточно прав                         |
| 404 | Ресурс не найден                          |

Все ошибки возвращаются в формате [RFC 7807 ProblemDetail](https://datatracker.ietf.org/doc/html/rfc7807).
