# CLAUDE.md — no-scroll (DIM0547 Web II)

Repositório: https://github.com/tobiasantos/no-scroll · Coorte B (online) · grupo individual

## REGRAS INEGOCIÁVEIS

1. **Fazer o MÍNIMO.** Implementar só o necessário para atender a rubrica da sprint atual
   ([web2-2026-2/docs/RUBRICAS.md](../web2-2026-2/docs/RUBRICAS.md)). Nada de abstrações "para o
   futuro", bibliotecas extras, features fora do backlog ou soluções sofisticadas.
2. **Isto é uma fundação.** O código deve ser básico, simples e fácil de estender depois da
   disciplina. Soluções mais complexas e robustas ficam para depois, não agora.
3. O aluno precisa conseguir **explicar cada linha** (regra de uso de IA da disciplina). Código
   simples e explícito vale mais que código esperto.
4. **Replicar o material da aula.** O código da API segue exatamente os passos de
   [web2-2026-2/exemplos/ktor-tarefas/PASSOS.md](../web2-2026-2/exemplos/ktor-tarefas/PASSOS.md)
   (mesmos arquivos, mesmas bibliotecas, mesmo estilo), trocando só a entidade. O que o
   PASSOS não cobre segue o código das leituras (`web2-2026-2/leituras/`) e, por último, o `../musi/`.
   Nada de soluções próprias distantes do material.
5. Registrar o uso de IA em `docs/uso-de-ia.md` (exigido pela disciplina).
6. **Claude NUNCA aparece como coautor.** Commits sem `Co-Authored-By: Claude`, PRs sem
   "Generated with Claude Code" ou qualquer atribuição ao Claude. Autor único: o aluno.
7. **Sem comentários no código** (Kotlin, Go, Gradle, TOML, YAML). Exceções: `scripts/` pode ter comentários de uso;
   e os `.proto` (Sprint 2) devem ter comentários, exigido pela rubrica. Ao restaurar
   `../_backup-sprint1/`, remover os comentários copiados do exemplo.

## Produto

App de controle de tempo de uso. O usuário cadastra um limite diário por app (TikTok,
Instagram…) e recebe um alerta tipo despertador quando passa do limite.

- **Avaliado na disciplina:** apenas o backend (API Kotlin/Ktor + serviço Go + contratos + infra).
- **App mobile:** fora do escopo da disciplina. Ler o tempo de uso de outros apps exige código
  nativo (Android `UsageStatsManager`; no iOS, Screen Time API com entitlement da Apple).
  Bloquear scroll/interceptar requisições (VPN local, Accessibility Service) está **fora** do MVP.

## Arquitetura (monorepo)

```
api/        Kotlin + Ktor (CRUD, Exposed, Flyway, PostgreSQL)
services/   Go: serviço "avaliador" (gRPC) — Sprint 2
protos/     contratos .proto (buf) — Sprint 2
docs/       proposta.md, uso-de-ia.md
mise.toml   tasks build, test, lint, up, ci
docker-compose.yml
.github/workflows/ci.yml
```

Pacotes da API (leitura pte1, seção 7.7): `domain/` (sem Ktor/Exposed/JDBC), `adapters/web/`
(rotas), `adapters/persistence/` (memória e Exposed), `Application.kt` (montagem). Teste de arquitetura (Konsist ou ArchUnit) garante a regra.

## Domínio (mínimo)

Código (classes, rotas, tabelas) em inglês; documentação em português.

- **Setup** (regra de um app): `id`, `appPackage` (ex. `com.instagram.android`), `appName`,
  `dailyLimitMinutes` (1–1440), `alertMode` (`ONCE` | `PERIODIC`),
  `alertIntervalMinutes` (obrigatório só se `PERIODIC`)
- **Session** (uso de um app): `id`, `setupId` (FK → Setup, 1:N), `startedAt`, `durationMinutes`
- `User` só entra no bloco final (autenticação/anti-BOLA), via nova migração.

Rotas:
- `/setups` — CRUD, paginação `?page=&size=` (máx. 100), filtro `?app=`
- `/setups/{id}/sessions` — CRUD aninhado, filtro `?date=`
- `/setups/{id}/status` — Sprint 2: API chama o Go via gRPC

## Serviço Go (Sprint 2)

`avaliador`: RPC unário `AvaliarUso(minutos_limite, minutos[]) → (usados, restantes, excedido)`.
Na Sprint 3 ganha o cache (com métricas hit/miss).

## Roteiro por sprint (só o que a rubrica pede)

| Sprint | Prazo | Entrega mínima |
|---|---|---|
| 0 (atrasada) | 16/09 | Monorepo, CI verde nos dois stacks, `docs/proposta.md` |
| 1 | 02/10 | CRUD de Setup + Session, Flyway, docker compose, teste de arquitetura, Testcontainers, problem details, OpenAPI |
| 2 | 23/10 | Go `avaliador` + proto/buf + chamada gRPC com deadline + teste de integração + arch-go |
| 3 | 20/11 | Neon, cache no Go com métricas, deploy (Northflank/Render), GHCR, logs JSON, health |
| Final | 11/12 | JWT, anti-BOLA, docs/seguranca.md, Semgrep/Renovate, site de docs, referência da API |

## Material da disciplina (seguir antes de inventar)

Em `../web2-2026-2/`:
- CRUD Ktor passo a passo: `exemplos/ktor-tarefas/PASSOS.md` (passos 1–10) e o código do exemplo
- Leituras: `leituras/web2-s1-pte1.md` (rotas, JSON, status, problem+json, camadas, Koin) e
  `leituras/web2-s1-pte2.md` (Exposed, Flyway, Testcontainers, OpenAPI)
- Slides: 03 (estrutura do monorepo, CI, mise), 04 (CRUD), 05 (persistência, testes, OpenAPI)
- Referência completa: `../musi/` (CI em `.github/workflows/ci.yml`). O slide 05 diz que o
  `musi/api-ktor` tem teste ArchUnit, mas a cópia local (30/08) ainda não tem: fazer `git pull`.
- O exemplo `ktor-tarefas` NÃO tem teste de arquitetura.

## Estado

- Sprint 0: API só com o Passo 1 do PASSOS.md (servidor mínimo).
- Sprint 1 já adiantada (PASSOS 1–10 para Setup) guardada FORA do repo em
  `../_backup-sprint1/`. Restaurar só quando o aluno disser que começou a Sprint 1.

## Processo (grupo de 1 pessoa)

- Toda mudança por PR com descrição e checklist, fechando uma issue (`Closes #N`).
- Commits espalhados pelas semanas (não concentrar num dia).
- CI verde em `main` sempre.
