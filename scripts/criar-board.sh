#!/usr/bin/env bash
# Cria o quadro do GitHub Projects com o backlog de docs/proposta.md.
# Cada história vira uma issue, entra no quadro e recebe Prioridade, Estimativa e Sprint.
#
# Uso (no Codespace ou em qualquer máquina com gh e jq), na raiz do repositório:
#   unset GITHUB_TOKEN                 # no Codespace o token padrão não tem acesso a Projects
#   gh auth login -s project           # uma vez: login com o escopo "project"
#   bash scripts/criar-board.sh
#
# Rode uma vez só: rodar de novo cria um segundo quadro e issues duplicadas.
set -euo pipefail

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
OWNER=${REPO%/*}
TITULO="no-scroll — backlog"

echo "Criando o quadro \"$TITULO\" para $OWNER..."
PROJETO=$(gh project create --owner "$OWNER" --title "$TITULO" --format json)
NUM=$(jq -r .number <<<"$PROJETO")
PID=$(jq -r .id <<<"$PROJETO")
gh project link "$NUM" --owner "$OWNER" --repo "${REPO#*/}"

gh project field-create "$NUM" --owner "$OWNER" --name "Prioridade" \
  --data-type SINGLE_SELECT --single-select-options "P1,P2,P3" >/dev/null
gh project field-create "$NUM" --owner "$OWNER" --name "Estimativa" --data-type NUMBER >/dev/null
gh project field-create "$NUM" --owner "$OWNER" --name "Sprint" \
  --data-type SINGLE_SELECT --single-select-options "Sprint 1,Sprint 2,Sprint 3,Final" >/dev/null

CAMPOS=$(gh project field-list "$NUM" --owner "$OWNER" --format json)
campo() { jq -r --arg n "$1" '.fields[] | select(.name == $n) | .id' <<<"$CAMPOS"; }
opcao() { jq -r --arg n "$1" --arg o "$2" '.fields[] | select(.name == $n) | .options[] | select(.name == $o) | .id' <<<"$CAMPOS"; }

# historia "título" "critérios (uma linha por item)" prioridade estimativa sprint
historia() {
  local corpo url item
  corpo=$(printf '## Critérios de aceitação\n\n%s\n' "$(sed 's/^/- [ ] /' <<<"$2")")
  url=$(gh issue create --repo "$REPO" --title "$1" --body "$corpo")
  item=$(gh project item-add "$NUM" --owner "$OWNER" --url "$url" --format json | jq -r .id)
  gh project item-edit --id "$item" --project-id "$PID" --field-id "$(campo Prioridade)" \
    --single-select-option-id "$(opcao Prioridade "$3")" >/dev/null
  gh project item-edit --id "$item" --project-id "$PID" --field-id "$(campo Estimativa)" \
    --number "$4" >/dev/null
  gh project item-edit --id "$item" --project-id "$PID" --field-id "$(campo Sprint)" \
    --single-select-option-id "$(opcao Sprint "$5")" >/dev/null
  echo "  ok: $1"
}

historia "Como usuário, quero cadastrar uma regra para um app para limitar meu uso diário" \
"Limite de 1 a 1440 minutos
Alerta ONCE ou PERIODIC
Intervalo obrigatório se PERIODIC" P1 3 "Sprint 1"

historia "Como usuário, quero listar, editar e remover minhas regras para ajustá-las" \
"Listagem paginada e filtrável por app
404 para id inexistente" P1 2 "Sprint 1"

historia "Como app mobile, quero registrar sessões de uso de um app para acumular o tempo do dia" \
"Sessão ligada a uma regra
Duração positiva
Filtro por data" P1 3 "Sprint 1"

historia "Como usuário, quero saber quanto já usei hoje para saber se passei do limite" \
"Serviço Go calcula usados, restantes e excedido
Chamada gRPC com deadline" P1 5 "Sprint 2"

historia "Como usuário, quero que o status responda rápido porque o app consulta com frequência" \
"Cache com TTL e invalidação ao registrar sessão
Métricas de hit/miss" P2 3 "Sprint 3"

historia "Como usuário, quero me autenticar para que só eu veja e altere minhas regras" \
"JWT com refresh
Teste provando que um usuário não acessa regra de outro" P2 5 "Final"

echo
echo "Pronto: https://github.com/users/$OWNER/projects/$NUM"
echo "Cole esse link na seção 3 de docs/proposta.md."
