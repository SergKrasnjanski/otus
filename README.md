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
