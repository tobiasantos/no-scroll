# Uso de IA

| Ferramenta  | Tarefa                                                                              | Data       |
| ----------- | ----------------------------------------------------------------------------------- | ---------- |
| Claude Code | Esqueleto do monorepo (estrutura, mise, CI, compose)                                | 21/09/2026 |
| Claude Code | Rascunho da proposta (docs/proposta.md), revisado por mim                           | 21/09/2026 |
| Claude Code | Script de criação do quadro no GitHub Projects (scripts/criar-board.sh)             | 24/09/2026 |
| Claude Code | CRUD de Setup seguindo o PASSOS.md da aula (Ktor, Exposed, Flyway, testes, OpenAPI) | 24/09/2026 |
| Claude Code | PUT, DELETE e Location no POST de Setup (leitura pte1, seções 6.3 e 6.4)            | 26/09/2026 |
| Claude Code | Entidade Session com rota aninhada /setups/{id}/sessions, seguindo Obra 1:N Anotação do MUSI | 28/09/2026 |
| Claude Code | Teste de arquitetura com ArchUnit, baseado no ArquiteturaTest do MUSI | 30/09/2026 |
| Claude Code | Paginação no SQL e filtros (?page, ?size, ?app, ?date), seguindo a pte2 11.3 e o MUSI | 06/10/2026 |
| Claude Code | Validação no domínio e erros em problem details (Passo 11 do exemplo) e testes unitários do domínio | 09/10/2026 |
| Claude Code | README da Sprint 1 (rotas, arquitetura, testes, checklist) e ajuste da proposta ao calendário de 03/10 | 14/10/2026 |
| Claude Code | Domínio do serviço Go (avaliar o uso e alertar nos canais), testes, demo e ADR-0001, seguindo o Passo 13 do exemplo avisos-go | 29/10/2026 |
| Claude Code | Contrato .proto comentado com buf (lint e geração dos stubs), servidor gRPC do avaliador com health check, reflection e encerramento gracioso, e buf lint no CI, seguindo os passos 14 e 15 do exemplo avisos-go | 11/11/2026 |
| Claude Code | API chama o avaliador em Go por gRPC ao registrar uma sessão: porta no domínio, stubs gerados pelo Gradle, adaptador com prazo, testes com servidor em memória e regra de arquitetura, seguindo o Passo 16 do exemplo ktor-tarefas | 13/11/2026 |
| Claude Code | Dockerfiles da API e do serviço Go, health checks (rota /health e subcomando health do Go) e docker compose com banco, Go e API, seguindo o Passo 17 do exemplo | 15/11/2026 |
| Claude Code | CI sobe o docker compose e espera os serviços saudáveis, e publica as imagens da API e do Go no GHCR na main, seguindo o modelo ci-e-imagens.yml do Passo 18 | 17/11/2026 |
| Claude Code | Implantação: comando migrate e PORT na API, render.yaml (API no Render, banco no Neon, segredos fora do repositório), task db:migrate e ADR-0002, seguindo o Passo 18 do exemplo | 26/11/2026 |
| Claude Code | README da entrega final (o que é, arquitetura, no ar, pré-requisitos, como rodar e testar, exemplos), LICENSE, correção da ADR-0002 e seção 6 da proposta | 28/11/2026 |
