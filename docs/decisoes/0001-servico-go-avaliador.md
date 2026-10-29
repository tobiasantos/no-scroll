# ADR-0001 — O serviço Go avalia o uso do dia e dispara os alertas

**Estado:** Aceita
**Data:** 2026-10-29

## Contexto

O no-scroll guarda, por app, um limite diário (`Setup`) e as sessões de uso (`Session`). O
produto existe para avisar o usuário, como um despertador, quando ele passa do limite. A
Sprint 2 pede um segundo serviço, em Go, com uma responsabilidade que justifique um processo à
parte: E/S que espera por terceiros, concorrência, processamento pesado ou agendamento. A
disciplina também pede que o segundo serviço não divida tabelas com a API.

Na proposta, o serviço Go (`avaliador`) só calculava usados, restantes e excedido. Isso é uma
soma: rodaria em microssegundos dentro da API e não justifica um serviço.

## Decisão

O serviço Go (`services/evaluator`) recebe o uso do dia de um app (nome, limite e os minutos
de cada sessão), calcula usados, restantes e excedido e, se o limite foi ultrapassado, dispara
o alerta em todos os canais (despertador, push e e-mail) ao mesmo tempo, uma goroutine por
canal, respeitando o prazo de quem chamou. Ele não acessa o banco: tudo chega na requisição.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Só avaliar (soma de minutos) no Go | Responsabilidade sem motivo para ficar fora da API: é uma conta, sem E/S nem espera |
| Alertar dentro da API (Kotlin) | Os canais externos (push, e-mail) são lentos e podem falhar; dentro da API, um canal travado prende a requisição de quem registrou a sessão, e uma falha de provedor vira erro da API |
| Go lendo as sessões direto do banco | Dois serviços dividindo as mesmas tabelas não podem mudar de forma independente; a API é a dona dos dados |

## Consequências

- A API continua dona dos dados e só manda ao Go o necessário; o Go pode mudar de canais (ou
  ganhar um canal real) sem mexer na API.
- Os canais rodam em paralelo: o tempo do alerta é o do canal mais lento, não a soma. Com prazo
  curto, os canais lentos desistem e os rápidos (o despertador) entregam.
- Mais um processo para subir, testar e observar, e uma chamada de rede entre os dois. A API
  precisa tratar o Go fora do ar: o registro da sessão não pode depender do alerta.
- Os canais são simulados (esperam um tempo e registram no log). Ligar a um provedor real de
  push ou e-mail fica para depois.

## Como verificar

`go test ./...` em `services/`: o teste de concorrência (três canais em ~400 ms, não 600 ms), o
de prazo (com 100 ms, só o despertador entrega) e o de entrada inválida (`errors.Is` com
`ErrInvalidUsage`). Que o Go não toca o banco se vê no `go.mod`, sem driver de banco.
