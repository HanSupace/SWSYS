# LastSys Node.js + PostgreSQL

This folder is a Node.js/Express rewrite of the Spring Boot LastSys app. It reuses the existing HTML/CSS/JS assets and stores data in PostgreSQL.

## Local Setup

1. Install Node.js 20+ and PostgreSQL.
2. Create a database:

```sql
create database lastsys;
```

3. Copy environment settings:

```powershell
Copy-Item .env.example .env
```

4. Edit `.env`:

```text
DATABASE_URL=postgres://postgres:postgres@localhost:5432/lastsys
SESSION_SECRET=change-this-session-secret
```

5. Install dependencies and initialize the database:

```powershell
npm install
npm run db:init
npm run dev
```

Open:

```text
http://localhost:3000
```

## Koyeb

Use a hosted PostgreSQL database and set these environment variables in Koyeb:

```text
DATABASE_URL=postgres://USER:PASSWORD@HOST:5432/DBNAME
SESSION_SECRET=use-a-long-random-string
NODE_ENV=production
PGSSL=true
```

Koyeb should detect the `Dockerfile` in this folder. The app listens on `PORT`.

## Notes

The main flows are implemented: signup, login, profile actions, daily missions, emotion map records, likes, comments, emotion summary, healing spots, and ranking data storage. Some server-rendered text still comes from the copied Thymeleaf fallback markup, while the interactive parts use the Express APIs.
