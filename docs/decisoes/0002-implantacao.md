# ADR-0002 — A API vai ao ar no Render, com o banco no Neon; o serviço Go fica no compose

**Estado:** Aceita
**Data:** 2026-11-26

## Contexto

A Sprint 2 pede a API e um banco gerenciado implantados numa plataforma de container, com as
migrações aplicadas por task ou CI, a connection string fora do repositório, health check e a API
respondendo quando o serviço Go não está acessível. O serviço Go no ar é opcional (nota de 15/10
da rubrica), e desde 19/11 a URL pública não é exigida: a implantação é avaliada pelas evidências.

O CI já publica as duas imagens no GitHub Container Registry a cada push na `main`
(`ghcr.io/tobiasantos/no-scroll/api` e `.../evaluator`, com a etiqueta do commit e `latest`).

Limites do plano gratuito do Render, consultados pelo professor em 15/10/2026 (comentários do
modelo `exemplos/implantacao/render.yaml`):

- serviço privado (`pserv`) não tem instância gratuita;
- um web service gratuito envia, mas não recebe, tráfego pela rede privada;
- o endereço público dos web services não encaminha gRPC;
- o `preDeployCommand` só existe nos planos pagos.

## Decisão

- **A API** roda no Render como web service gratuito, a partir da imagem do GHCR
  (`render.yaml` na raiz, Blueprint do Render), com `healthCheckPath: /health`. A porta vem de
  `PORT`, que o Render define.
- **O banco** é um PostgreSQL gerenciado no Neon. `DB_URL`, `DB_USER` e `DB_PASSWORD` entram como
  segredos no painel do Render (`sync: false` no `render.yaml`); nunca são escritos no repositório.
- **As migrações** são aplicadas por task: `mise run db:migrate` roda `api migrate`, que aplica as
  migrações Flyway no banco das variáveis de ambiente e termina, sem subir o servidor (o comando
  `migrar` do Passo 18). No plano gratuito, sem `preDeployCommand`, a API também migra ao subir,
  como no compose; o Flyway só aplica o que falta.
- **O serviço Go** não vai ao ar: roda junto da API no `docker compose`, onde a integração gRPC é
  avaliada e testada no CI. No Render, a API sobe sem `EVALUATOR_ADDRESS`; a chamada ao Go falha
  dentro do prazo de 1 s e a sessão é registrada mesmo assim (degradação, ADR-0001).

## Alternativas consideradas

- **Serviço Go como `pserv` no Render (plano starter).** É o modelo do professor e daria a
  integração completa no ar, mas é pago.
- **Serviço Go como web service gratuito no Render.** Não funciona: o endereço público não
  encaminha gRPC, e o web service gratuito não recebe tráfego da rede privada.
- **Migração no `preDeployCommand`.** Seria o ideal, uma etapa antes de trocar a versão no ar,
  mas só existe nos planos pagos. A task faz o mesmo, rodada por quem implanta.
- **Migração num job do CI.** Exigiria guardar a connection string do Neon também nos segredos do
  GitHub, um segundo lugar para manter. A task é mais simples e suficiente para a rubrica.
- **Banco no próprio Render.** O PostgreSQL gratuito do Render expira; o Neon é o banco gerenciado
  indicado pela disciplina.

## Consequências

- A API no ar faz o CRUD completo, mas não dispara alertas: sem o serviço Go acessível, cada
  sessão registrada gera um aviso `not evaluated` no log. É o comportamento esperado de
  degradação, e não um erro.
- O serviço web gratuito do Render dorme sem uso; a primeira requisição depois disso demora.
- A imagem da API no GHCR precisa estar **pública** para o Render baixá-la sem credencial.
- Quem implanta precisa rodar `mise run db:migrate` com as variáveis do Neon antes de uma versão
  com migração nova, ou confiar na migração na subida.

## Como verificar

1. `mise run db:migrate` com as variáveis do Neon: o log do Flyway mostra as migrações aplicadas
   (ou "Schema ... is up to date").
2. No painel do Render, o serviço `no-scroll-api` com a imagem do GHCR, os três segredos e o
   health check em `/health` verde.
3. `curl <url-do-render>/health` devolve `200`; `POST /setups/{id}/sessions` devolve `201` e o log
   do Render mostra `not evaluated: UNAVAILABLE`.
