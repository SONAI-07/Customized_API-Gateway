<div align="center">

# ⚡ Hyper-Reactive API Gateway

**Service discovery, self-healing eviction, smart load balancing, edge rate-limiting, a JWT trust boundary and a live ops dashboard — in one zero-infrastructure JAR.**

*Built for hackathon teams and bootstrapped startups. Honest about what it is — and what it isn't.*

![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green) ![Gateway](https://img.shields.io/badge/Spring%20Cloud%20Gateway-WebFlux-blue) ![Docker](https://img.shields.io/badge/Docker-ready-2496ED) ![License](https://img.shields.io/badge/license-MIT-lightgrey)

</div>

<!-- Replace with your dashboard screenshot before publishing -->
![Live dashboard](docs/dashboard.png)

---

## The 30-Second Pitch

Every microservices project needs a traffic cop that answers four questions: *who is alive, who is least busy, who is allowed in, and who is sending too much?* The default answer is renting heavyweight infrastructure — Eureka, Consul, Kong, Zuul — before you've written a line of product code.

This gateway answers all four **in-house, with zero external dependencies**:

- 🧠 **Built-in service registry** — services self-register over REST. No Eureka/Consul bill.
- ⏱️ **O(1) self-healing** — a hierarchical timing wheel evicts silent instances in seconds, whether you run 10 instances or 60,000.
- ⚖️ **Least-active-connections load balancing** — traffic follows live telemetry, not round-robin guesses.
- 🚦 **Token-bucket rate limiting at the edge** — per-API-key buckets with continuous refill; spam dies before it costs you CPU.
- 🔐 **JWT trust boundary** — tokens validated once at the edge; downstream services ship **zero security code**.
- 📊 **Ops dashboard compiled into the JAR** — a React/TypeScript dashboard served by the gateway itself, plus Prometheus-ready metrics.
- 🐳 **Hackathon-grade DX** — `docker compose up` → dashboard live → drop one SDK file per service → demo.

---

## Why This Exists — And Why It Is *Not* Kong or Zuul

**What this is:** a single-JAR traffic cop for your first 1–15 services; a hackathon accelerator; and a readable, from-scratch implementation of the patterns big gateways hide behind you (timing wheels, token buckets, trust boundaries, reactive filter chains).

**What this is not:** a Kong/Zuul replacement. There is no plugin marketplace, no DB-backed admin API, no multi-tenant policy engine, no battle-hardened TLS/HTTP2 edge tuning — and by design, there shouldn't be. Kong is a highway system. This is the smart traffic light your first product actually needs.

> **The graduation rule:** when your fleet outgrows one gateway node, this project has done its job — because by then you'll know *exactly* why the bigger tools exist.

---

## Request Lifecycle

```
client ─▶ ① CORS ─▶ ② Registry Guard ─▶ ③ Token-Bucket Limiter ─▶ ④ JWT Trust Boundary
             (preflight)   (X-Registry-Token)   (429 when bucket empty)  (strips spoofed headers,
                                                                          injects X-User-Id/Role)
                                                │ accepted
                                                ▼
                          ⑤ Route match — YAML predicates, or zero-config
                             auto-discovery (/<service-name>/**)
                                                ▼
                          ⑥ CustomDiscoveryClient → in-memory registry
                                                ▼
                          ⑦ CustomLoadBalancer → least active connections
                                                ▼
                          ⑧ ConnectionTracker (+1 conn) ─▶ Netty ─▶ your microservice
                                                │
                          ⑨ response ◀──────────┘  (−1 conn)

background:  timing wheel ticks 1×/sec → 90s of silence → O(1) eviction
             dashboard polls /gateway/observability/summary every 3s
```

---

## Quickstart

### Option A — Docker (recommended)
```bash
git clone https://github.com/SONAI-07/Customized_API-Gateway.git && cd Customized_API-Gateway
docker compose up --build
```
| Surface | URL |
|---|---|
| Gateway + live dashboard | http://localhost:8080/dashboard/ |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 *(admin/admin)* |

### Option B — From source (pure-Java experience)
```bash
mvn spring-boot:run
```
The `frontend-maven-plugin` downloads Node, builds the React dashboard and embeds it into the JAR — one command, no npm knowledge required.

### Use it in your project — 4 steps
1. **Run the gateway** (above).
2. **Register each service** — drop [`client-sdk/GatewayAutoRegistrar.java`](client-sdk/GatewayAutoRegistrar.java) into any Spring Boot service and add `@EnableScheduling`. Or register manually:
   ```bash
   curl -X POST http://localhost:8080/registry/register \
     -H "Content-Type: application/json" -H "X-Registry-Token: <your-token>" \
     -d '{"serviceName":"ORDER-SERVICE","instanceID":"order-1","host":"127.0.0.1","port":8081,"weight":1}'
   ```
3. **Route traffic** — zero-config: every registered service is instantly reachable at `/<service-name>/**`. For clean public URLs, add predicates:
   ```yaml
   spring.cloud.gateway.routes:
     - id: orders
       uri: lb://ORDER-SERVICE          # "lb://" → your custom load balancer
       predicates: [Path=/api/orders/**]
       filters: [StripPrefix=2]
   ```
4. **Authenticate users** — issue HS256 JWTs with the gateway's secret (dev shortcut: `GET /auth/dev-token?userId=alice&role=ADMIN`). Clients send `Authorization: Bearer …`; your services simply read `X-User-Id` and `X-User-Role`. No security libraries downstream. Ever.

---

## Observability, Built In

`GET /gateway/observability/summary` returns live totals (received / accepted / blocked-by-reason), per-service instance counts, active connections and the **busiest instance per service**. The embedded dashboard visualizes it in real time; `/actuator/prometheus` feeds Prometheus/Grafana for history and alerting.

---

## Design Tradeoffs (Read Before Production)

| Decision | Why | When you'll outgrow it |
|---|---|---|
| In-memory registry & rate-limit state | Zero infra, nanosecond reads | Multiple gateway replicas → move state to Redis |
| Volatile state (restart wipes registry) | SDK heartbeats re-register services in <30s | Cold-start-critical fleets → add snapshot persistence |
| HS256 shared-secret JWTs | One secret, zero key infrastructure | Org-wide SSO / cross-team issuance → RS256 + JWKS |
| Timing-wheel eviction (90s) over active probes | O(1), zero probe traffic | Sub-second failover needs → add active health probes |
| Dashboard embedded in the JAR | Ships with the product; one artifact to deploy | Design-system-heavy ops consoles → standalone frontend |

---

## Scar Tissue — Bugs That Shaped the Architecture

Every feature was curl-verified; every failure below was reproduced, root-caused and documented:

- **`GlobalFilter` vs `WebFilter`** — gateway filters only run on *matched routes*; `@RestController`s silently bypass them. All edge guards are therefore `WebFilter`s.
- **Bean-name collision** with Resilience4j's internal `rateLimiterRegistry` → renamed ours `TokenBucketRegistry`.
- **Spring Framework 7 relocation** of `PathContainer` → switched to the decade-stable `AntPathMatcher`.
- **Micrometer's `registry.get()` throws** on meters never recorded → `registry.find()`.
- **CORS preflight trap** — `OPTIONS` requests must skip every guard, or browsers fail silently.
- **Jackson vs Lombok** all-args constructors → dedicated `RegistrationRequest` DTO that also blocks clients from spoofing internal fields.

---

## Repository Layout

```
src/main/java/com/apiGateway/
├── registry/        # in-memory registry, REST API, timing-wheel eviction
├── discovery/       # ReactiveDiscoveryClient bridge
├── loadbalancer/    # least-active-connections LB + connection tracker
├── ratelimiter/     # token bucket + idle-bucket garbage collection
├── security/        # JWT trust boundary, registry guard, dev tokens
├── observability/   # Micrometer meters + summary endpoint
└── config/          # CORS, security chain, dashboard routing
dashboard/           # React + TypeScript ops dashboard (compiled into the JAR)
client-sdk/          # drop-in auto-registrar for your microservices
observability/       # prometheus.yml + docker-compose for the metrics stack
```

## Roadmap

- [ ] `gateway-client-spring-boot-starter` — SDK as a one-line Maven dependency
- [ ] Redis-backed shared state for multi-replica gateways
- [ ] RS256 / JWKS support and per-route role policies
- [ ] Per-route circuit breaking (Resilience4j already on the classpath)
- [ ] Pre-provisioned Grafana dashboard JSON
- [ ] Registry snapshotting across restarts

---

## 📖 The Full Build Log

Every feature above has a written post-mortem: requirement → tradeoff → implementation → failure → fix.
**[Read the complete engineering journey on Medium →](https://medium.com/@your-handle)** *(link at publish time)*

*Built in an AI-paired engineering workflow: every design decision argued, every bug reproduced with curl, every fix verified against the live dashboard. The article shows the reasoning chain — not just the result.*

---

<div align="center">

**If this saved your hackathon weekend, give it a ⭐ — and ship something people use.**

MIT License · Built by [SONAI-07](https://github.com/SONAI-07)

</div>