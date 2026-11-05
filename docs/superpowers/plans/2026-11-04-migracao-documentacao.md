# Migração e documentação de continuidade

**Objetivo:** migrar o PostgreSQL Render para o destino Neon existente, preservando contas, histórico e 2FA, e entregar documentação suficiente para outra pessoa ou IA operar o projeto.

**Arquitetura:** manter Next.js/Vercel, Java 21/Spring Boot/Render, JDBC e Flyway. Trocar somente a conexão PostgreSQL após backup, ensaio e validação. Preservar ENCRYPTION_KEY e todas as integrações atuais.

**Escopo autorizado:** solicitação de 04/11/2026 neste chat; apresentação em 15/12/2026. Acesso externo temporário autorizado somente para o IP do computador /32, com TLS e remoção ao terminar. Nenhum plano pago ou exclusão da origem.

## Etapas verificáveis

- [x] Conferir recursos e HEAD remoto antes de alterar arquivos.
- [x] Obter conexões sem imprimir credenciais; restringir armazenamento local e excluir do Git.
- [x] Fazer backup consistente e restaurar ensaio em destino isolado/vazio; comparar esquema, contagem e conteúdo de todas as tabelas, incluindo Flyway, usuários, hashes e segredos MFA cifrados.
- [x] Interromper gravações na origem, repetir cópia final, validar e mudar somente DATABASE_* no Render; preservar ENCRYPTION_KEY.
- [x] Verificar API e operações autenticadas no destino; documentar os testes que dependem do responsável.
- [x] Remover acesso temporário à origem; manter backup e caminho de retorno, sem rollback automático após novas gravações no destino.
- [x] Escrever contexto completo, mapa da API/dados/configuração, manual operacional, migração/retorno e matriz de testes; corrigir descrições antigas conflitantes.
- [x] Publicar documentação no GitHub com proteção contra atualização concorrente e conferir publicação (testes pertinentes concluídos).
- [x] Atualizar estado local e automação para impedir uma segunda migração indevida.

## Critérios de parada

Sem backup validado, sem conectividade segura, diferença nos dados, erro de restauração ou dúvidas sobre gravações concorrentes: manter origem e não efetuar troca. O endpoint /health retorna UP sem consultar o banco; sua resposta isolada não prova migração nem integridade.

## Riscos que serão conferidos

Segredo MFA perdido; URL JDBC em formato incorreto; role/owner/ACL diferentes entre provedores; sessões e monitor gerando novas linhas durante a cópia; rollback descartando gravações novas; documentação afirmando testes que não foram feitos.
