# no-scroll

API para controle de tempo de uso de apps: o usuário define regras por app (limite diário e tipo de alerta) e registra sessões de uso.
Projeto da disciplina DIM0547 — Desenvolvimento de Sistemas Web II (UFRN, 2026.2).

Proposta: [docs/proposta.md](docs/proposta.md) · Quadro: [no-scroll — backlog](https://github.com/users/tobiasantos/projects/4)

## Equipe

| Nome                   | Matrícula   | GitHub                                         | Papel                              |
| ---------------------- | ----------- | ---------------------------------------------- | ---------------------------------- |
| Tobias dos Santos Neto | 20220041830 | [@tobiasantos](https://github.com/tobiasantos) | Desenvolvimento (grupo individual) |

Integração com outra disciplina: nenhuma.

## Como rodar

Pré-requisitos: [mise](https://mise.jdx.dev) e Docker.

```sh
mise install      # JDK 25 e Go
mise run up       # PostgreSQL
mise run build
mise run test     # os testes de integração precisam do Docker
mise run run      # API em http://localhost:8080
mise run demo     # cria e lista um setup
```

- Swagger UI: http://localhost:8080/docs
- Especificação OpenAPI: http://localhost:8080/docs/documentation.yaml

## API

Todos os erros saem em `application/problem+json` (RFC 9457).

| Método | Rota | Resposta |
|---|---|---|
| `GET` | `/setups?page=&size=&app=` | `200`, paginado (`size` padrão 20, máximo 100), filtro por nome do app |
| `POST` | `/setups` | `201` com `Location`; `422` se violar uma regra |
| `GET` | `/setups/{id}` | `200`; `404` |
| `PUT` | `/setups/{id}` | `200`; `404`; `422` |
| `DELETE` | `/setups/{id}` | `204`; `404`. Remove também as sessões |
| `GET` | `/setups/{id}/sessions?date=AAAA-MM-DD` | `200`, filtro por dia (UTC); `404` se o setup não existe |
| `POST` | `/setups/{id}/sessions` | `201` com `Location`; `404`; `422` |
| `GET` | `/setups/{id}/sessions/{sessionId}` | `200`; `404` se a sessão não é deste setup |
| `PUT` | `/setups/{id}/sessions/{sessionId}` | `200`; `404`; `422` |
| `DELETE` | `/setups/{id}/sessions/{sessionId}` | `204`; `404` |

## Arquitetura

```
api/src/main/kotlin/br/ufrn/noscroll/
├── Application.kt             montagem: Koin, JSON, StatusPages, rotas
├── domain/                    Setup, Session, Page, regras de validação, portas (sem Ktor nem banco)
└── adapters/
    ├── web/                   rotas, OpenAPI e erros em problem details
    └── persistence/           PostgreSQL (Exposed, Hikari, Flyway) e repositórios em memória
```

- Esquema do banco só por migração Flyway (`api/src/main/resources/db/migration`).
- `ArchitectureTest` (ArchUnit) falha se o domínio depender de Ktor, Koin, Exposed, JDBC, Hikari, Flyway ou `adapters/`.

## Testes

`mise run test` roda, local e no CI:

- **Unitários do domínio** (`ValidationTest`): as regras de validação, sem banco nem HTTP.
- **Rotas** (`RoutesTest`): com os repositórios em memória, sem Docker.
- **Integração** (`SetupsTest`, `SessionsTest`): PostgreSQL real com Testcontainers.
- **Arquitetura** (`ArchitectureTest`): a regra de dependência.

## Estrutura do monorepo

- `api/` — serviço principal em Kotlin/Ktor
- `services/` — microsserviço Go (`avaliador`, Sprint 2)
- `protos/` — contratos gRPC (Sprint 2)
- `docs/` — proposta e registro de uso de IA

## Checklist da Sprint 1

- [x] CRUD de ≥2 entidades com relacionamento (Setup 1:N Session), paginação e filtros
- [x] Teste de arquitetura da regra de dependência rodando no CI
- [x] Migrações Flyway (sem geração automática de schema)
- [x] Testes com Testcontainers verdes local e no CI
- [x] Validação + problem details + OpenAPI
- [ ] Vídeo 5 min

## Vídeos

- Sprint 0: não entregue
- Sprint 1: TODO
