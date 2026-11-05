# Migração PostgreSQL Render → Neon e procedimento de retorno

Data operacional: **04/11/2026**, America/Sao_Paulo. Resultado e evidências finais em [ESTADO-ATUAL.md](ESTADO-ATUAL.md). Este runbook não é prova de execução: só considerar o corte concluído quando o relatório o confirmar.

## Escopo e preservação

Somente o banco muda de provedor. Site continua na Vercel, API Java no Render e clientes mantêm as mesmas URLs da API. PostgreSQL de origem e destino são versão principal 17; ferramentas usadas na cópia devem ser compatíveis. A origem gratuita expira em 01/12/2026; apresentação prevista em 15/12/2026.

Preservar todas as 12 tabelas públicas, versões/checksums Flyway, IDs, hashes de senha/tokens, dados cifrados MFA, contador TOTP, códigos de recuperação, análises/chat, propriedade/revisões dos sites e auditoria. Preservar **exatamente a ENCRYPTION_KEY já configurada no Render**. Não recriar usuários nem fazer cadastro em banco vazio para simular migração.

## Pré-requisitos e credenciais

Destino existente: projeto Neon `sparkling-rice-29813860`, branch principal `br-holy-hall-b72vqt3b`, banco `sentinela`, role `sentinela_owner`, região aws-us-east-1. Origem: Render `dpg-dbjs4q7f3r2c73d09ne0-a`, banco `sentinela_db_j0rc`. IDs não são senhas.

Obter URLs pelos fluxos privados dos provedores. Não colocar URI com usuário/senha em comando que ficará no histórico, logs, documentos, `.ipynb` ou Git. Para ferramentas libpq, preferir variáveis de processo ou arquivo pgpass com permissões restritas; para Java, usar JDBC sem senha embutida e credenciais separadas.

Nesta operação, conexões foram guardadas localmente com Windows DPAPI e pasta restrita ao usuário do computador; elas não acompanham o repositório. Esse formato não é um backup portátil de segredos: depende do perfil Windows. A custódia durável em gerenciador de segredos é responsabilidade operacional, separada do banco. Não compartilhar arquivos privados com outra IA como parte do contexto.

Origem bloqueia acesso externo por padrão. Para o computador da cópia, uma regra temporária /32 foi autorizada explicitamente, com TLS. Ao terminar ou abortar, remover essa regra e verificar que o bloqueio original voltou. Nunca abrir `0.0.0.0/0` para contornar falha de acesso. Tráfego interno da API Render não depende dessa liberação.

## Ensaio antes do corte

1. Conferir branch/commit, recurso, versão e se destino está vazio; não sobrescrever banco com dados.
2. Criar branch de ensaio Neon a partir da principal vazia, com compute pequeno. Nesta operação: `ensaio-migracao-2026-11-04`, ID `br-dry-term-b7s8d7xn`.
3. Fazer dump em formato customizado e confirmar que `pg_restore --list` consegue lê-lo.
4. Restaurar em transação única na branch de ensaio, com `--no-owner --no-acl --exit-on-error --single-transaction`. Não aplicar ACL/owner específicos do Render no Neon.
5. Conferir lista de tabelas, colunas, tipos, constraints/índices restaurados, Flyway, contagens e hash do conteúdo de todas as linhas.
6. Comparações de conteúdo precisam ter ordenação determinística independente da locale do provedor, por exemplo `ORDER BY row_to_json(t)::text COLLATE "C"`. Contagens iguais sozinhas não comprovam conteúdo igual.
7. Dados do ensaio são cópia privada de produção: não compartilhar a branch nem usar para testes destrutivos de contas reais. Suspender compute ao terminar; não manter consumo desnecessário. Exclusão posterior de branch/dados precisa seguir a política e a autorização aplicáveis.

Com variáveis libpq já configuradas privadamente para cada conexão, o núcleo dos comandos é:

```text
pg_dump --format=custom --no-owner --no-acl --file backup.dump
pg_restore --list backup.dump
pg_restore --no-owner --no-acl --exit-on-error --single-transaction --dbname BANCO_DESTINO backup.dump
```

Esses comandos não incluem as credenciais. Separar dump, restauração e validação permite interromper com diagnóstico sem alterar conexão da API. Não usar `--clean` indiscriminadamente nem restaurar por cima de produção.

## Corte final sem perder gravações

1. Avisar sobre a janela breve de indisponibilidade.
2. Suspender temporariamente o serviço API Render para parar clientes e monitor. Manter o banco ligado. A API gratuita não possui o modo de manutenção pago.
3. Confirmar que processos escritores foram encerrados e transações concluíram/foram desfeitas; conferir conexões ativas da aplicação no PostgreSQL. Se houver outro escritor, interrompê-lo também.
4. Fazer novo dump final da origem parada. Não usar uma cópia antiga apenas porque o ensaio passou.
5. Restaurar na branch principal Neon ainda vazia, em transação única. Comparar novamente esquema, todas as contagens e hashes das linhas enquanto a origem permanece sem gravações.
6. Guardar dump de forma protegida, conferir que pode ser decifrado/restaurado e conservar evidências sem conteúdo pessoal. Preservar o banco Render como cópia da hora do corte.
7. Atualizar no ambiente Render apenas as variáveis necessárias: DATABASE_URL JDBC para o host Neon direto com TLS, DATABASE_USER e DATABASE_PASSWORD; alinhar DATABASE_HOST/PORT/NAME se existirem. Usar atualização por mesclagem, **não replace de todo o ambiente**. Não tocar ENCRYPTION_KEY nem chaves de integração. O conector Render pode salvar variáveis e depois retornar erro por não conseguir implantar serviço suspenso: conferir valores salvos, não inferir que nada mudou. Para resguardar a retomada, a origem desta operação recebeu `ALTER DATABASE sentinela_db_j0rc SET default_transaction_read_only = on` depois da cópia validada; conexões novas ficam somente leitura.
8. Retomar a API e implantar/reiniciar com a nova conexão. Conferir logs de Hikari/Flyway sem imprimir segredos; validar versões existentes sem recriar esquema.
9. Verificar página, API, negativa anônima/admin/CSRF e operação real que leia banco. Confirmar que a operação apareceu no Neon e não na origem. O health isolado não faz essa prova.
10. Testar conta normal/ADM, histórico, alteração autorizada e TOTP com o responsável. Testes automatizados com chaves sintéticas não substituem código de um Authenticator físico já vinculado.
11. Remover regra /32, conferir acesso externo bloqueado e registrar estado do destino/API. Suspender compute de ensaio e encerrar receptores/processos locais temporários.
12. Atualizar documentação e automação de 24/11 para não repetir a migração depois de concluída.

## Reversão e limite de segurança

Antes de novas gravações no Neon, se a API não iniciar e o problema não for corrigível rapidamente, restaurar a conexão anterior preservada e retomar a origem. Se foi ativada a proteção de escrita, remover deliberadamente o ajuste `default_transaction_read_only` da origem antes de retomá-la, usando conexão administrativa com transação explicitamente read-write. Não resetar senha nem chave MFA para mascarar erro de configuração.

**Depois que o Neon aceitar gravações, apontar de volta ao Render antigo perde essas alterações.** Nesse caso, abrir nova manutenção, fazer backup do Neon atual, comparar diferenças e planejar cópia reversa validada para destino apropriado. Não restaurar por cima da origem sem backup e autorização para sobrescrita. O Render gratuito continua sujeito à expiração, mesmo guardado como contingência.

Critérios de abortar o corte: falha de dump/listagem/restore, origem ainda recebendo escrita, diferença não explicada nas linhas, chave MFA não preservada, destino incorreto, fonte de credenciais desconhecida ou falha de conexão segura. Registrar como pendência, manter origem e não afirmar sucesso.

## Após a migração

Sem mudança da URL da API, Android, Telegram, Colab e rewrite Vercel não precisam apontar para outro domínio. Eles devem ser testados novamente porque dependem da API e dos dados migrados. A mudança do banco não liga Colab nem polling Telegram automaticamente.

Neon gratuito não deve ser tratado como capacidade ilimitada ou garantia perpétua. Conferir cotas/uso e orçamento no painel, sem contratar plano pago automaticamente. Hikari com conexões ociosas e monitor ativo podem manter compute acordado; API Render dorme por inatividade e não oferece monitoramento contínuo nessa configuração. Escala/continuidade exigem decisão operacional separada.

Referências: [pg_dump/pg_restore no Neon](https://neon.com/docs/import/migrate-from-postgres), [rede privada Render](https://render.com/docs/private-network), [limites Free Render](https://render.com/docs/free).
