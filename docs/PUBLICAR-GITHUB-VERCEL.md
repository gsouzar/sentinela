# Publicar Sentinela: GitHub + Vercel + Render

Arquitetura preparada: código no repositório `gsouzar/sentinela`, site Next.js na Vercel, API Java 21 e PostgreSQL no Render. A API é mantida como serviço para preservar seu agendador de monitoramento. Vercel também oferece containers em beta, mas transferir a API para Functions exige adequar o trabalho em segundo plano à escala das funções.

## 1. Enviar o código ao GitHub

O repositório deve existir e estar acessível à conta que fará o envio. Pode ser privado. Não é necessário enviar senha, chave ou token por chat.

Há uma cópia limpa pronta em `outputs/Sentinela-GitHub`. Abra o PowerShell nessa pasta (não na pasta do projeto em uso):

```powershell
git status
git remote -v
git push -u origin main
```

Se o Git solicitar login, conclua a autorização na janela oficial do GitHub. Não use `--force`. Se o repositório remoto já contiver arquivos, pare e integre o histórico antes de enviar.

A cópia preparada exclui banco local, `.runtime`, `.env`, dependências, builds e executáveis de distribuição. Os fontes Java, Next.js, Android, notebooks e documentação estão incluídos. Não arraste toda a pasta em uso para a página de upload.

Para executar uma cópia clonada localmente, primeiro gere o JAR (requer Java 21): na raiz, execute `.\services\api\mvnw.cmd -f services/api/pom.xml package`, crie a pasta `dist` e copie `services/api/target/sentinela-api-1.0.0.jar` para `dist/sentinela-api.jar`. Depois use os CMD habituais. No Render, o Dockerfile já faz essa compilação.

Se estiver usando o conector GitHub do ChatGPT/Codex, autorize esse repositório na instalação do aplicativo em [Configurações de aplicativos](https://github.com/settings/installations). Um 404 em repositório privado pode significar falta de acesso do conector.

## 2. Criar API e banco no Render

1. Entre em [Render Dashboard](https://dashboard.render.com/) com sua conta. Se ainda não tiver conta, conclua o cadastro por conta própria.
2. Escolha **New → Blueprint** e conecte o repositório `gsouzar/sentinela`, branch `main`.
3. O Render deve encontrar `render.yaml` na raiz. Confira os dois recursos: `sentinela-api` e `sentinela-db`, na mesma região.
4. O modelo usa plano **Free** para demonstração. Confira os planos e o custo mostrado antes de aplicar. Nenhum serviço pago é contratado apenas por baixar estes arquivos.
5. No campo solicitado **WEB_ORIGIN**, use temporariamente `https://example.invalid`. Este é um marcador que será substituído pelo domínio real da Vercel no passo 4; não é endereço do seu projeto.
6. Aplique o Blueprint e acompanhe o build. A imagem usa `services/api/Dockerfile.render`, Maven e Java 21. O banco é ligado por referências internas; não cole a URL `postgresql://...` diretamente em `DATABASE_URL`, pois o driver Java usa JDBC. O script preparado monta a URL JDBC automaticamente.
7. Quando o serviço estiver pronto, copie a URL HTTPS real da API. Ela pode ter um sufixo; não suponha que será exatamente `sentinela-api.onrender.com`.
8. Abra `https://SUA-API.onrender.com/api/v1/health`. Precisa retornar JSON com HTTP 200. O primeiro acesso ao serviço Free pode demorar enquanto ele acorda.

O Blueprint gera **ENCRYPTION_KEY** no ambiente do Render e conecta as credenciais do PostgreSQL sem colocá-las no código. Preserve essa chave e faça backup seguro: ela protege o 2FA. O banco novo da nuvem começa vazio; as contas do H2 local não são migradas por este procedimento.

**Limites do Free:** o serviço dorme após 15 minutos sem tráfego e o PostgreSQL gratuito expira após 30 dias. Enquanto a API dorme, o monitor não coleta. Para operação contínua, escolha serviço sempre ativo e banco persistente em planos adequados após revisar custos. Não trate essa configuração gratuita como produção permanente.

## 3. Publicar o site na Vercel

1. Entre em [Vercel New Project](https://vercel.com/new).
2. Importe `gsouzar/sentinela`. Se não aparecer, libere o repositório para o aplicativo da Vercel no GitHub.
3. Nome sugerido: `sentinela`. **Framework Preset: Next.js**.
4. Em **Root Directory → Edit**, selecione **apps/web**. Não selecione a raiz do monorepositório.
5. Use **Node.js 24.x**, Install Command `npm ci` e Build Command `npm run build`. Deixe Output Directory no padrão do Next.js. O `vercel.json` dessa pasta já define framework e comandos.
6. Em **Environment Variables**, adicione `API_INTERNAL_URL` = URL HTTPS real da API Render, sem `/api/v1` e sem barra final. Exemplo ilustrativo: `https://SUA-API.onrender.com`. Selecione **Production**. Não use `NEXT_PUBLIC_` e não use localhost.
7. Clique **Deploy**, aguarde **Ready** e copie o domínio de produção real exibido. Se alterar `API_INTERNAL_URL`, faça novo deploy: os rewrites do Next.js usam esse valor durante o build.

As chamadas do navegador continuam em `/api/v1` no domínio da Vercel e o Next.js encaminha à API. Isso mantém os cookies e o CSRF na mesma origem para o navegador. Não é necessário liberar CORS para qualquer origem ou desativar a proteção CSRF.

Não conecte previews de pessoas desconhecidas ao banco de produção. Para previews, use ambiente de teste separado. Mudança de domínio exige revisar WEB_ORIGIN no backend e redeploy do frontend quando mudar a URL da API.

## 4. Conectar o domínio e criar o administrador

1. No Render, abra **sentinela-api → Environment**.
2. Troque **WEB_ORIGIN** pelo domínio real de produção da Vercel, incluindo `https://` e sem barra final. Exemplo ilustrativo: `https://SEU-PROJETO.vercel.app`.
3. Confira **SECURE_COOKIES=true**, **DEMO_MODE=false** e **ENCRYPTION_KEY** preenchida. Salve e aguarde o novo deploy da API.
4. Para provisionar o primeiro administrador, adicione temporariamente **ADMIN_EMAIL** com um email ainda não cadastrado no banco e **ADMIN_PASSWORD** com uma senha exclusiva de pelo menos 16 caracteres. Insira os valores somente no painel de ambiente do Render.
5. Aguarde o deploy, entre no site e configure o Google Authenticator pelo QR code. Guarde os códigos de recuperação.
6. Remova **ADMIN_EMAIL** e **ADMIN_PASSWORD** do ambiente Render e faça novo deploy. A conta permanece no banco. Não remova ENCRYPTION_KEY.
7. Teste uma conta comum separada, com um email diferente. Ela deve abrir a área de cliente; ADMIN tem sua própria central.

## 5. Colab, Groq, Telegram e Android

- **Groq no Colab:** use o notebook e os Secrets `GROQ_API_KEY` e `GROQ_MODEL` descritos em [GROQ-COLAB.md](GROQ-COLAB.md). No Render, configure `COLAB_CHAT_URL` com o túnel HTTPS terminado em `/explain`, e `COLAB_CHAT_KEY` com a chave compartilhada do notebook. Essa chave é diferente da chave Groq. Reinicie a API após mudar o ambiente. A URL do túnel pode mudar e Colab não é serviço permanente; quando indisponível, o projeto preserva explicação local.
- **Telegram:** configure `TELEGRAM_API_KEY` no ambiente Render e o mesmo valor nos Secrets do notebook Telegram, além de `TELEGRAM_BOT_TOKEN`, `TELEGRAM_ALLOWED_USER_IDS` e `SENTINELA_API_BASE=https://SUA-API.onrender.com/api/v1`. Veja [TELEGRAM.md](TELEGRAM.md). O bot só funciona enquanto o processo dele estiver ligado. O token do BotFather não deve ir para a Vercel nem para o GitHub.
- **Android Studio:** em `apps/android/gradle.properties`, adicione `apiBase=https://SUA-API.onrender.com/api/v1`, sem credenciais. Faça Sync Project e gere/reinstale o aplicativo. O APK antigo usa o endereço local até ser recompilado. No notebook Colab e no celular físico, localhost não aponta para o seu computador.
- **Fontes de ameaças:** se quiser usá-las, configure `GOOGLE_SAFE_BROWSING_KEY` e `URLHAUS_KEY` no Render. Sem configuração, a interface informa cobertura parcial. Não altere para DEMO_MODE=true fingindo cobertura real.

## 6. Verificar a publicação

1. No domínio Vercel, abra `/api/v1/health` e confirme HTTP 200. Se a API responder no Render e falhar pela Vercel, confira API_INTERNAL_URL e refaça o deploy do site.
2. Cadastre uma conta de teste e entre. Confirme que ela não acessa administração nem dados de outra conta.
3. Configure o Google Authenticator, guarde códigos de recuperação, saia e faça login com senha + código atual.
4. Faça uma análise pontual. Confira as fontes e a cobertura, sem interpretar baixo score como garantia de segurança.
5. Em Meus sites, use um domínio sob seu controle, confirme o TXT DNS e aguarde amostras reais. Não use domínio de terceiros sem controle da zona DNS.
6. Confira os logs de deploy/runtime se houver falha. Não publique logs com credenciais.
7. Verifique backups, data de expiração do PostgreSQL e estado da instância. Atualizações de código na branch de produção podem disparar novos deploys.

## Referências

[Vercel: projetos e monorepositórios](https://vercel.com/docs/projects) · [Vercel: containers e escala](https://vercel.com/docs/functions/runtimes/container) · [Render: Blueprint](https://render.com/docs/blueprint-spec) · [Render: limites gratuitos](https://render.com/docs/free).

Preparação local não equivale a publicação concluída: confirme o push, os deploys e os testes no endereço real antes de compartilhar com clientes ou com a banca.
