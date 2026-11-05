# Estado atual confirmado — 04/11/2026

Data de referência: America/Sao_Paulo. Apresentação prevista: **15/12/2026**. Este relatório substitui o estado de infraestrutura anterior; não contém credenciais.

## Produção após a migração

| Componente | Instalação |
|---|---|
| Repositório privado | https://github.com/gsouzar/sentinela |
| Site Next.js / Vercel | https://sentinela-woad.vercel.app |
| API Java / Render | https://sentinela-api-doxm.onrender.com |
| Base Android/Telegram | https://sentinela-api-doxm.onrender.com/api/v1 |
| API Render ID | srv-dbjs6eg473hc73aopg1g, My Workspace, Virginia, Free |
| Deploy de corte confirmado live | dep-dblu5md9fdbs73b78nl0 |
| Commit de aplicação no corte | df9b3df83b73518e27ff337594a2b4b4c1c13e6f |
| Banco de produção | PostgreSQL 17.11 no Neon, plano gratuito |
| Projeto Neon | sparkling-rice-29813860, Sentinela continuidade TCC |
| Branch / banco / role | br-holy-hall-b72vqt3b / sentinela / sentinela_owner |
| Endpoint direto da API | ep-green-fire-b7bgvsje.c-13.us-east-1.aws.neon.tech |
| Região / compute | aws-us-east-1, min/max 0.25 CU |
| Origem antiga preservada | Render dpg-dbjs4q7f3r2c73d09ne0-a; não é mais produção |

**Migração concluída:** dump final restaurado e comparado antes de retomar a API; configuração Render mudou para Neon; logs Flyway mostram conexão ao host Neon e validação das duas migrações; teste autenticado pela URL Vercel gravou no destino e não apareceu na origem. ENCRYPTION_KEY e chaves das integrações não foram incluídas na atualização do ambiente e foram preservadas.

Não houve mudança de domínio da API. APK e bot existentes continuam com a mesma base; Colab e Telegram ainda precisam de seus runtimes/chaves/processos ativos.

## Integridade e dados

Houve ensaio em branch isolada `br-dry-term-b7s8d7xn` e nova cópia final com API suspensa e sem conexões escritoras. Restauração transacional, sem ACL/owner do provedor anterior. Esquema de colunas, contagem e SHA-256 de linhas ordenadas com COLLATE C coincidiram nas 12 tabelas antes de abrir gravações no destino.

| Tabela | Linhas na cópia final |
|---|---:|
| analyses | 1 |
| app_users | 4 |
| audit_events | 24 |
| chat_messages | 1 |
| consumed_refresh | 0 |
| flyway_schema_history | 2 |
| monitored_sites | 2 |
| rate_buckets | 25 |
| recovery_codes | 24 |
| sessions | 3 |
| site_checks | 0 |
| site_events | 0 |

Esses números são fotografia do corte, não métricas que devam permanecer constantes. A operação posterior pode mudar sessões, auditoria, limites e dados de negócio. O teste técnico usou uma conta temporária identificada em example.invalid; cadastro, dados MFA, sessões e auditoria dessa conta foram removidos após validação. Confirmado ao terminar: 4 contas originais, 1 análise, 2 sites e 2 migrações Flyway no Neon. Nenhuma conta original foi removida.

## Origem e backups

Banco Render antigo continua preservado, com `default_transaction_read_only=on` para novas conexões e acesso externo novamente bloqueado (`ipAllowList=[]`). A regra temporária /32 foi removida. **Não apontar a API de volta a ele** sem plano de cópia reversa: ele não recebe as novas gravações do Neon. Continua sujeito à expiração informada em 01/12/2026.

Backup final local fora do repositório: `work/migration-private/final-20261104.dump.dpapi`, na pasta de trabalho deste chat. Dump lógico antes da cifragem: 28.541 bytes; SHA-256 `6f5d8ea4a49c469c0f201bb300ed841efdba0beb8598cb12e4eaf957b706b97d`. Cifrado com Windows DPAPI, decifragem comparada ao original e versão em claro removida. Diretório com permissões restritas ao perfil Windows. Não é arquivo para compartilhar com outra IA e não está no Git.

DPAPI depende do usuário/máquina Windows. Antes de perder esse perfil, transferir o backup por processo privado e protegido e guardar ENCRYPTION_KEY separadamente. Para recuperar o dump local com o mesmo perfil, usar `.NET System.Security.Cryptography.ProtectedData.Unprotect` com `DataProtectionScope.CurrentUser`, gravar somente em pasta privada fora do repo e conferir o SHA-256 acima antes de restaurar. O ensaio comprovou a restauração do conteúdo; a cifragem foi validada por round-trip exato dos bytes.

Branch de ensaio foi conservada, com compute suspenso; contém cópia privada e não deve ser compartilhada/publicada. Nenhum recurso de origem foi excluído e nenhum plano pago foi contratado.

## Validações executadas nesta operação

- Java/H2: 38 testes, zero falhas/erros.
- Web: 4 testes e build de produção Next.js/TypeScript concluídos.
- Python: 23 testes; transportes Groq/Telegram simulados.
- Android: assembleDebug e lintDebug, BUILD SUCCESSFUL.
- Migração: esquema e conteúdo das 12 tabelas iguais antes do corte.
- Público via Vercel: health 200; anônimo em me/admin 401; mutação sem CSRF 403; cadastro inválido com CSRF 400; cadastro técnico 201; leituras autenticadas de me/histórico/sites/dashboard 200; USER em admin 403.
- MFA técnico: setup/ativação TOTP 200; login sem fator 428; login com código de recuperação de uso único 200; revogação de sessões 200. Código TOTP calculado para conta técnica; não foi leitura física de QR no celular.
- Prova de destino: conta técnica existiu no Neon e não na origem; removida após teste. Logs da instância live confirmaram host Neon e Flyway V1/V2.

Durante a retomada, um teste iniciou antes de a API estar pronta e sofreu timeout. Foi repetido após deploy live e passou. Duas chamadas de alteração de ambiente retornaram erro de deploy por serviço suspenso, embora variáveis tivessem sido salvas; valores foram conferidos e foi feito novo deploy após retomar. A origem foi colocada em somente leitura para impedir escrita pela instância antiga durante a transição.

A conexão fornecida pelo conector Neon era pooled; o restore pequeno transacional concluiu e teve conteúdo validado. Antes de abrir a produção, a configuração foi normalizada para o host direto e a conexão direta foi conferida. Próximos dumps/restores devem selecionar explicitamente conexão direta, conforme o runbook. O MCP SQL Neon usa search_path vazio: consultas operacionais precisam qualificar `public.app_users`, etc.; erro de relação sem prefixo não significa banco vazio.

## Pendências e limites reais

1. Responsável deve testar **sua conta já existente** e seu Google Authenticator físico após a migração. Não foi solicitada senha/código humano ao agente.
2. Conferir Android em aparelho/emulador e Groq/Telegram com Secrets reais e processos ativos. O responsável relatou testes dessas integrações anteriormente; não foram repetidos com suas chaves nesta migração.
3. Não há amostras/eventos reais na fotografia do corte. Monitoramento requer TXT válido e API ativa; não inventar gráficos/ataques.
4. Render Free pode dormir, e Neon Free tem cotas. Conferir disponibilidade e uso antes da apresentação; este arranjo não garante coleta 24h.
5. Variáveis ADMIN_EMAIL/ADMIN_PASSWORD ainda foram observadas no ambiente durante a conferência. Após confirmar conta administrativa, remover ambas pelo fluxo privado do Render; não recriar conta ou ENCRYPTION_KEY.
6. A automação de 24/11/2026 às 10h foi convertida em verificação de continuidade somente leitura. Ela não repetirá a migração nem sobrescreverá o banco novo; avisará em caso de problema acionável.

## Próxima pessoa ou IA

Comece em [CONTEXTO-COMPLETO.md](CONTEXTO-COMPLETO.md). Receba o código e esta documentação, sem dumps/segredos. Consulte [OPERACAO-COMPLETA.md](OPERACAO-COMPLETA.md), [MAPA-TECNICO.md](MAPA-TECNICO.md), [MIGRACAO-NEON.md](MIGRACAO-NEON.md) e [VALIDACAO-E-TESTES.md](VALIDACAO-E-TESTES.md). Para produção, reconfira o estado dos provedores: este relatório é datado.
