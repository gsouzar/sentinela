# Sentinela

**Publicação:** [GitHub + Vercel + API Java/PostgreSQL no Render](docs/PUBLICAR-GITHUB-VERCEL.md). O modelo gratuito é para demonstração, com limites de disponibilidade e retenção.

**2FA por QR code no site:** ADMIN e usuário podem cadastrar o Google Authenticator em Minha segurança. [Passo a passo](docs/QR-CODE-2FA.md).

**Chatbot da equipe: Groq no Colab.** Configure `GROQ_API_KEY` e `GROQ_MODEL` nos Secrets do notebook. [Roteiro Groq](docs/GROQ-COLAB.md) e [manual completo corrigido](docs/manual/Manual_Sentinela_Groq.pdf). A chamada usa o endpoint oficial Groq e preserva explicação local quando não pode obter uma resposta validada.

**Novo painel do cliente:** Meus sites, confirmação DNS, histórico de disponibilidade/certificado/cabeçalhos e eventos opcionais do backend do cliente. Consulte [MONITORAMENTO.md](docs/MONITORAMENTO.md). Cliente entra em sua área; ADMIN entra na central administrativa. 2FA é opcional para USER e obrigatório para ADMIN (REQUIRE_MFA=true permite exigir de todos). Assinaturas têm controle manual, sem cobrança automática.

Abra **INICIO-AQUI.html** no navegador para um guia de uso completo, ou leia [GUIA-DO-USUARIO.md](docs/GUIA-DO-USUARIO.md). A logo fica em `apps/web/public/sentinela-logo.png`.

**Comece aqui:** [passo a passo completo no Windows e Android Studio](docs/COMO-RODAR.md), [Telegram e Colab](docs/TELEGRAM.md), [revisão de segurança e 2FA](docs/REVISAO-SEGURANCA.md). A política atual permite 2FA opcional para clientes e obrigatório para administradores.

Plataforma acadêmica de análise de links enviados pelo usuário: **Next.js, Java/Spring Boot, Android nativo Java e chatbot Google Colab**. O motor calcula score e evidências; o chatbot explica. Score não é probabilidade e baixo risco não garante segurança.

## Iniciar neste computador

Execute `1-INICIAR-API.cmd` e, em outro terminal, `2-INICIAR-WEB.cmd`. Mantenha os dois terminais abertos e use Ctrl+C para encerrar. Abra http://localhost:3000 e crie sua conta. Não há senha administrativa fixa.

O script da API usa Java 21; neste espaço de trabalho também encontra o JDK portátil preparado durante o desenvolvimento. O web precisa de Node.js 24 LTS. Fora deste computador, instale [JDK 21](https://adoptium.net/temurin/releases/?version=21) e [Node.js LTS](https://nodejs.org/). Na primeira execução o web instala dependências e compila, se necessário.

A chave local de MFA fica em `.runtime/`, fora do pacote e do Git. Preserve essa chave enquanto houver MFA configurado. O inicializador não instala serviços nem mantém processos ocultos.

O comando da API habilita modo de demonstração, com base sintética identificada para `banc0-seguranca-login.test`. Para executar sem essa fonte, use a execução manual abaixo com `DEMO_MODE=false`.

## Execução manual

API, com Java 21:
```powershell
cd services/api
$env:ENCRYPTION_KEY="defina-um-segredo-longo-aleatorio-e-persistente"
$env:DEMO_MODE="true" # apenas apresentação
.\mvnw.cmd spring-boot:run
```
Sem banco configurado, usa H2 persistente em `services/api/data/`. O jar pronto está em `dist/sentinela-api.jar`. Para PostgreSQL, configure `DATABASE_URL=jdbc:postgresql://host:5432/sentinela`, `DATABASE_USER` e `DATABASE_PASSWORD`.

Web, em outro terminal:
```powershell
cd apps/web
npm ci
npm run dev
```
Use `localhost:3000`. O Next.js faz proxy para a API em `127.0.0.1:8080`; os cookies ficam na mesma origem.

## Docker e PostgreSQL

Copie `.env.example` para `.env`, preencha senha do banco e chave de criptografia e execute `docker compose up --build -d`. O banco e a API ficam na rede interna, e somente o web é publicado em `127.0.0.1:3000`.

O Compose local usa HTTP e `SECURE_COOKIES=false`. Em hospedagem, configure HTTPS, `SECURE_COOKIES=true` e `WEB_ORIGIN` com a origem pública exata. Nunca publique a porta PostgreSQL. Consulte `docs/VERIFICACAO.md` para saber quais verificações foram executadas.

## Android Studio

Abra **a pasta `apps/android`** no Android Studio. O app é Java nativo, sem WebView, com compile/target SDK 35 e mínimo API 26. Aguarde Gradle Sync e execute em um emulador. O APK de depuração compilado está em `dist/Sentinela-debug.apk`.

No debug, a API é `http://10.0.2.2:8080/api/v1`, acessível pelo emulador. Para celular físico/release, configure um endpoint HTTPS:
```sh
./gradlew assembleRelease -PapiBase=https://api.seu-dominio.example/api/v1
```
Em Linux/macOS, use `bash gradlew` ou dê permissão de execução ao wrapper.

Release bloqueia HTTP. O debug permite HTTP somente para `10.0.2.2`, `localhost` e `127.0.0.1`. Tokens são criptografados com Android Keystore. O APK entregue é para avaliação; não é assinado para publicação em loja.

## Google Colab

Importe `notebooks/Sentinela_Chatbot_Colab.ipynb` no [Colab](https://colab.research.google.com/) e execute as células em ordem. A explicação local funciona sem chave. Groq e ngrok são opcionais; as credenciais vêm do painel Secrets.

Para conectar o notebook: configure `COLAB_CHAT_URL=https://seu-tunel/explain` e `COLAB_CHAT_KEY` com a mesma chave do notebook, e reinicie a API. Se o Colab parar, o sistema usa explicação local. O score permanece exclusivamente no Java.

## Integrações e administração

`GOOGLE_SAFE_BROWSING_KEY` e `URLHAUS_KEY` habilitam fontes reais. Sem chaves, cada fonte aparece como não configurada, e a análise é parcial. A [URLhaus exige Auth-Key](https://urlhaus.abuse.ch/api/); [Safe Browsing](https://developers.google.com/safe-browsing/reference/rest) é destinado a uso não comercial. Consultas com credenciais reais não foram executadas nesta entrega.

Para criar a primeira conta administrativa em banco novo, configure `ADMIN_EMAIL` e `ADMIN_PASSWORD` (mínimo 16 caracteres) antes de iniciar. O bootstrap não promove um email já cadastrado. Remova a senha do ambiente depois do provisionamento e ative MFA pela interface.

## Testar e apresentar

- API: `cd services/api` e `mvnw.cmd test`.
- Web: `cd apps/web` e `npm ci`, `npm test`, `npm run build`.
- Android: `cd apps/android` e `gradlew.bat assembleDebug lintDebug`.
- Python: `pip install -r notebooks/requirements.txt` e `python -m unittest discover -s notebooks/tests`.
- Dataset rotulado: `python scripts/avaliar_dataset.py entrada.csv saida.json`.

Consulte `docs/ARQUITETURA.md`, `docs/SEGURANCA.md`, `docs/ROTEIRO-TCC.md`, `docs/VERIFICACAO.md` e `docs/openapi.json`.

A equipe dos documentos tem quatro frentes: backend/API/banco, motor/cybersecurity, mobile e web/chatbot. A atividade identifica Gustavo de Souza Rodrigues, Gabriel Herrera, Gustavo Pereira e João Eduardo Melnic. Não foi inventada uma atribuição individual além dos documentos.

Limites: não há monitoramento de sites de clientes, recuperação de senha por email, detecção geográfica de login ou bloqueio universal da navegação. Não há garantia de nota ou segurança absoluta. O relatório distingue execução real, exemplo sintético e integração dependente de credenciais.
