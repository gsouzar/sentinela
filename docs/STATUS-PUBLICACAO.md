# Sentinela publicado — 01/11/2026

- Site: https://sentinela-woad.vercel.app
- API Java: https://sentinela-api-doxm.onrender.com
- Código: https://github.com/gsouzar/sentinela
- Painel da API: https://dashboard.render.com/web/srv-dbjs6eg473hc73aopg1g
- Painel do banco: https://dashboard.render.com/d/dpg-dbjs4q7f3r2c73d09ne0-a

## Estado confirmado

Frontend Next.js na Vercel: READY, Node 24, raiz apps/web.
API Java 21 no Render: live, plano Free, região Virginia.
PostgreSQL 17 no Render: disponível, plano Free, na mesma região e com acesso externo bloqueado.
As credenciais foram inseridas diretamente pelo responsável no Render e não foram enviadas ao GitHub.

Verificação pelo domínio público:
- Página inicial: HTTP 200.
- /api/v1/health: HTTP 200, status UP.
- /api/v1/me sem sessão: HTTP 401.
- Mutação sem CSRF: HTTP 403.
- Entrada inválida com CSRF válido: HTTP 400.
- Administração sem sessão: HTTP 401.
- Cookie CSRF marcado Secure; cabeçalhos nosniff e X-Frame-Options DENY presentes.

Não foram criadas contas técnicas de teste. Cadastro completo de usuário, login com credenciais reais e leitura física do QR no Google Authenticator devem ser conferidos pelo responsável. A verificação de implantação não significa auditoria completa de segurança.

## Primeiro acesso e administrador

O banco da nuvem é separado do H2 local; contas locais não foram migradas. Cadastre seu usuário no site.

Para provisionar o primeiro ADMIN, siga a etapa 4 de [PUBLICAR-GITHUB-VERCEL.md](PUBLICAR-GITHUB-VERCEL.md): configure temporariamente ADMIN_EMAIL com email ainda não cadastrado e ADMIN_PASSWORD com senha exclusiva de pelo menos 16 caracteres no ambiente Render; aguarde o deploy, entre e configure o QR de 2FA. Depois remova essas duas variáveis e faça novo deploy, preservando ENCRYPTION_KEY.

## Colab, Telegram e mobile

As integrações dependem dos Secrets e processos descritos no guia. Publicar o site não liga automaticamente o Colab ou o bot Telegram. Para o Android e o bot, a base pública da API é:
https://sentinela-api-doxm.onrender.com/api/v1

## Continuidade e custos

Nenhum serviço pago foi contratado. No Free, a API pode dormir após inatividade e o monitor não coleta durante esse período. O Render informou expiração deste banco em 01/12/2026. Planeje exportação ou mudança de plano antes dessa data e preserve a chave ENCRYPTION_KEY.

Os recursos desta instalação foram criados diretamente, sem aplicar Blueprint. O render.yaml é um modelo para novas instalações. Não o aplique à instalação atual sem reconciliar os recursos e os nomes internos de banco/usuário gerados pelo Render.

Referência dos limites Free: https://render.com/docs/free
