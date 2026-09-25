# Proposta — no-scroll

DIM0547 — Desenvolvimento de Sistemas Web II · 2026.2

## 1. Visão do produto

```
Para pessoas que perdem horas rolando feeds em apps como TikTok e Instagram
Que não conseguem controlar o próprio tempo de uso
O no-scroll é uma API de controle de tempo de uso de apps
Que guarda regras de uso por app e avalia quando o limite foi ultrapassado
Diferente dos controles nativos do celular, que só mostram o tempo gasto
Nosso produto alerta como um despertador, uma vez ou periodicamente, até o usuário parar
```

## 2. MVP

| No MVP                                                         | Fora do MVP                                           |
| -------------------------------------------------------------- | ----------------------------------------------------- |
| CRUD de regras de uso por app (limite diário e tipo de alerta) | App mobile que lê o tempo de uso do celular           |
| Registro de sessões de uso por app                             | Bloquear o scroll ou interceptar requisições dos apps |
| Status do dia: minutos usados, restantes e se passou do limite | Notificações push enviadas pelo servidor              |
| Cache do status, com métricas de acerto                        | Estatísticas e histórico semanal                      |
| Autenticação: cada usuário vê só as suas regras                | Metas, recompensas e gamificação                      |

**Hipótese de valor:** acreditamos que pessoas que querem reduzir o tempo em redes sociais vão
respeitar limites definidos por elas mesmas porque um alerta insistente interrompe o scroll
automático no momento em que ele acontece.

## 3. Backlog

Backlog priorizado no GitHub Projects: [no-scroll — backlog](https://github.com/users/tobiasantos/projects/4).

| Prio | História                                                                               | Critérios de aceitação                                                                       | Estimativa | Sprint |
| ---- | -------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- | ---------- | ------ |
| P1   | Como usuário, quero cadastrar uma regra para um app para limitar meu uso diário        | Limite de 1 a 1440 minutos; alerta `ONCE` ou `PERIODIC`; intervalo obrigatório se `PERIODIC` | 3          | 1      |
| P1   | Como usuário, quero listar, editar e remover minhas regras para ajustá-las             | Listagem paginada e filtrável por app; `404` para id inexistente                             | 2          | 1      |
| P1   | Como app mobile, quero registrar sessões de uso de um app para acumular o tempo do dia | Sessão ligada a uma regra; duração positiva; filtro por data                                 | 3          | 1      |
| P1   | Como usuário, quero saber quanto já usei hoje para saber se passei do limite           | Serviço Go calcula usados, restantes e excedido; chamada gRPC com deadline                   | 5          | 2      |
| P2   | Como usuário, quero que o status responda rápido porque o app consulta com frequência  | Cache com TTL e invalidação ao registrar sessão; métricas de hit/miss                        | 3          | 3      |
| P2   | Como usuário, quero me autenticar para que só eu veja e altere minhas regras           | JWT com refresh; teste provando que um usuário não acessa regra de outro                     | 5          | Final  |

P1 é essencial ao MVP, P2 é importante. Estimativa em pontos.

## 4. Entidades do domínio

| Entidade                             | Atributos principais                                                                                  | Relação     |
| ------------------------------------ | ----------------------------------------------------------------------------------------------------- | ----------- |
| **Setup** (regra de uso de um app)   | `appPackage`, `appName`, `dailyLimitMinutes`, `alertMode` (`ONCE`/`PERIODIC`), `alertIntervalMinutes` | 1:N Session |
| **Session** (uso de um app)          | `setupId`, `startedAt`, `durationMinutes`                                                             | N:1 Setup   |
| **User** (a partir da entrega final) | credenciais                                                                                           | 1:N Setup   |

## 5. Decisão: Kotlin/Ktor

Escolhi Kotlin com Ktor por dois motivos. O primeiro é o mercado: vejo mais vagas pedindo Kotlin,
e aprender a linguagem agora amplia minhas oportunidades. O segundo é interesse pessoal: sempre
quis aprender Kotlin, e o projeto é a chance de fazer isso num sistema real.

Tecnicamente, o Ktor combina com o domínio: a API tem muitas operações curtas de I/O (banco e
chamadas ao serviço Go), e as corrotinas do Kotlin tratam isso sem bloquear threads. Por ser
interoperável com Java, o Kotlin usa bibliotecas maduras como Flyway, HikariCP e Testcontainers.

## 6. Divisão entre o serviço principal e o Go

| Serviço principal (Kotlin/Ktor)                         | Serviço Go (`avaliador`)                                     |
| ------------------------------------------------------- | ------------------------------------------------------------ |
| Entidades Setup, Session e User e suas regras           | Soma as sessões do dia e compara com o limite da regra       |
| Persistência no PostgreSQL e migrações                  | Devolve minutos usados, restantes e se o limite foi excedido |
| Autenticação e autorização                              | Cache do status do dia, com métricas de acerto (Sprint 3)    |
| Orquestração: recebe a requisição e chama o Go por gRPC |                                                              |

**Justificativa:** o status do dia é a consulta mais frequente do sistema, porque o app mobile
pergunta repetidamente se o limite foi ultrapassado. É processamento e pré-computação que se
beneficia de cache, trabalho que a disciplina associa aos serviços Go. Isolar esse cálculo num
serviço leve deixa o serviço principal responsável só pelo CRUD e pela segurança. A comunicação
é por gRPC, com o contrato em `protos/`.

## 7. Equipe

| Nome                   | Matrícula   | Papel                                       |
| ---------------------- | ----------- | ------------------------------------------- |
| Tobias dos Santos Neto | 20220041830 | Desenvolvimento completo (grupo individual) |

## 8. Coorte e integração

- Coorte: **B (online)**
- Integração com outra disciplina: nenhuma
