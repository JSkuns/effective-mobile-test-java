# Система управления банковскими картами

### Описание
    RESTful API сервис для управления банковскими картами с JWT-аутентификацией, шифрованием чувствительных данных и
    разграничением прав доступа.

### Основные возможности:
    Аутентификация и авторизация через JWT-токены
    Управление пользователями (регистрация, назначение ролей)
    Управление картами (создание, блокировка, просмотр баланса)
    Переводы между картами с проверкой баланса
    Шифрование номеров карт (AES-256)
    Разделение ролей: USER и ADMIN
    Автоматическая документация через Swagger/OpenAPI

### Технологии
    Технология          Назначение

    Java                Язык программирования
    Spring Boot         Фреймворк
    Spring Security     Безопасность и JWT
    Spring Data JPA     Работа с БД
    PostgreSQL          База данных
    Liquibase           Миграции БД
    JJWT                JWT токены
    Docker              Контейнеризация
    Swagger/OpenAPI     API документация

### Предварительные требования (обязательные)
    JDK 17+ 
    Maven 3.8+
    Docker и Docker Compose
    PostgreSQL 15+

### Быстрый старт
Клонирование репозитория
```bash
#git clone <repository-url>
#cd effective-mobile-test-java
```
Генерация секретного ключа для JWT
```bash
openssl rand -base64 32
```
Настройка конфигурации в
src/main/resources/application.properties

Запуск приложения через Docker
```bash
# Сборка образов
docker compose build --no-cache
# Запуск контейнеров
docker compose up -d
# Просмотр логов
docker compose logs -f
```

### Swagger UI
http://localhost:8080/swagger-ui