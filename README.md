# E-Vote System

A secure electronic voting web application built with Spring Boot + Thymeleaf + MySQL.

## Deploy to Railway

1. Push this folder to a GitHub repository
2. Go to [railway.app](https://railway.app) → New Project → Deploy from GitHub
3. Add a **MySQL** plugin from Railway's service catalog
4. Railway auto-injects `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD` — no config needed
5. Run `schema.sql` once via Railway's MySQL shell or a DB client

## Local Development

```bash
# Requires Java 17+ and Maven
cd evote-web
mvn spring-boot:run
```

Open: http://localhost:8080

## Default Login

| Role  | Username | Password  |
|-------|----------|-----------|
| Admin | admin    | admin123  |
| Voter | (set by admin or self-register) | |
