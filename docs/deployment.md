# Deployment

Target: a small Linux server with Docker and Docker Compose, reached from the internet through an existing Cloudflare
Tunnel. Everything runs from `docker-compose.prod.yml`: PostgreSQL, the HAPI FHIR server and this application.

Only the application is published, and only on the host loopback (`127.0.0.1:8082`). The database and the FHIR server
have no published ports. The tunnel is the only way in.

## 1. Secrets

Create a `.env` file next to `docker-compose.prod.yml` (never commit it). All of these are required except the last:

```
HAPI_DB_PASSWORD=<long random value>
COORDINATOR_PASSWORD=<long random value>
GP1_PASSWORD=<long random value>
GP2_PASSWORD=<long random value>
GROQ_API_KEY=<optional, only if the LLM rewriter is turned on>
```

The guest account (`guest` / `guest-demo`) stays public and read only. The staff passwords above replace the demo
ones from `application.yml`, so the values published in the repository do not work on the public server.

## 2. Data

The HAPI database holds the Synthea patients and the simulated demo cases. Copy it from the machine where it was
seeded:

```
docker exec screening-loop-db-1 pg_dump -U hapi -d hapi --no-owner | gzip > hapi.sql.gz
```

On the server, start only the database, restore, then start everything:

```
docker compose -f docker-compose.prod.yml up -d db
gunzip -c hapi.sql.gz | docker compose -f docker-compose.prod.yml exec -T db psql -U hapi -d hapi
docker compose -f docker-compose.prod.yml up -d --build
```

Check `http://127.0.0.1:8082/actuator/health` on the server. The first start builds the image (a few minutes).

## 3. Public hostname

In the Cloudflare Tunnel, add a public hostname that points to `http://localhost:8082` (or to the server address if the
tunnel runs in a container). Cloudflare terminates TLS.

## What the `prod` profile changes

- The session cookie is `Secure`.
- The client address comes from the `CF-Connecting-IP` header, and only when the request arrives from a private address
  (the tunnel), which is Tomcat's trusted-proxy rule. Failed identity attempts are therefore counted per real visitor,
  not for the whole tunnel. Because the application is published only on the loopback, nobody can reach it directly to
  forge that header.
- The FHIR server URL defaults to the Compose service (`http://fhir:8080/fhir`).

## Updating

```
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

The attempt limiter and liveness tokens live in memory, so a restart clears any lock.
