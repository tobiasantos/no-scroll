# no-scroll

API para controle de tempo de uso de apps: o usuário define regras por app (limite diário e tipo de alerta) e registra sessões de uso.
Projeto da disciplina DIM0547 — Desenvolvimento de Sistemas Web II (UFRN, 2026.2).

Proposta: [docs/proposta.md](docs/proposta.md)

## Equipe

| Nome                   | Matrícula   | GitHub                                         | Papel                              |
| ---------------------- | ----------- | ---------------------------------------------- | ---------------------------------- |
| Tobias dos Santos Neto | 20220041830 | [@tobiasantos](https://github.com/tobiasantos) | Desenvolvimento (grupo individual) |

Coorte: **B (online)**. Integração com outra disciplina: nenhuma.

## Como rodar

Pré-requisitos: [mise](https://mise.jdx.dev) e Docker.

```sh
mise install      # JDK 25 e Go
mise run up       # PostgreSQL
mise run build
mise run test
mise run run      # API em http://localhost:8080
```

## Estrutura

- `api/` — serviço principal em Kotlin/Ktor
- `services/` — microsserviço Go (`avaliador`)
- `protos/` — contratos gRPC
- `docs/` — proposta e registro de uso de IA

## Vídeos

- Sprint 0: TODO
