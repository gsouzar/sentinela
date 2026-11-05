# Manual operacional completo

Revisado em 04/11/2026. Não é necessário abrir Android Studio e Colab para o site funcionar: cada cliente/integração tem seu próprio processo. A API Java e o banco são compartilhados pelos clientes da mesma instalação.

## 1. Usar a instalação publicada

1. Abra https://sentinela-woad.vercel.app.
2. Clique Criar conta para novo usuário; informe nome, email e senha de pelo menos 12 caracteres. Para conta existente, escolha Entrar.
3. Se já houver 2FA ativo, informe o código atual do Google Authenticator; aguarde o próximo se acabou de usar o anterior.
4. Use Analisar link para avaliação pontual ou Meus sites para monitorar um domínio próprio.
5. Para ADMIN, use as credenciais da conta administrativa provisionada. Não existe senha pública/fixa e cadastro comum não dá administração.

As contas publicadas ficam no PostgreSQL da nuvem; o H2 do seu computador é outro banco. Um usuário local não aparece automaticamente no site publicado. Na hospedagem gratuita, a primeira chamada após inatividade pode demorar enquanto a API desperta; não repetir cadastro várias vezes. Consulte os logs se a falha persistir.

## 2. Configurar Google Authenticator

1. Entre na conta e abra Minha segurança.
2. Escolha configurar autenticação de dois fatores e confirme a senha solicitada.
3. No celular, abra Google Authenticator → botão + → Ler código QR.
4. Aponte para o QR exibido no Sentinela. Se estiver no mesmo celular, use o segredo manual com cuidado.
5. Digite no Sentinela o código de seis dígitos mostrado no Authenticator e confirme a ativação.
6. Guarde os códigos de recuperação fora do celular e do repositório.
7. Saia e teste um novo login com senha e código atual.

Ler o QR só cadastra o segredo no aplicativo. A conta fica protegida quando o Sentinela confirma o código. Cada conta possui segredo próprio; não reaproveitar QR entre ADMIN e USER. Ajustar relógio do celular para data/hora automáticas se houver divergência. Não enviar QR/segredo/códigos por chat.

## 3. Rodar localmente no Windows

Instale Git para obter o código, JDK 21, Node.js 24 e, para Python, um interpretador real (não apenas o atalho da Microsoft Store). Baixe/clone o repositório para uma pasta de trabalho. Na cópia da equipe, a pasta usada anteriormente era `C:\Users\gugu2\OneDrive\Documentos\sentinela`; outras pessoas devem usar o próprio caminho.

### Caminho com inicializadores (ZIP que contém os binários)

Os CMDs dependem de `dist/sentinela-api.jar`, entregue nos pacotes com binários. Um clone limpo do GitHub não contém `dist/`: nesse caso, use o caminho manual Maven abaixo. Não interpretar ausência do jar no clone como falha da API.

1. Abra a pasta do projeto no Explorador.
2. Execute `1-INICIAR-API.cmd` e mantenha a janela aberta.
3. Execute `2-INICIAR-WEB.cmd` e mantenha a segunda janela aberta.
4. Espere a API informar que iniciou e o web mostrar o endereço; abra http://localhost:3000.
5. Cadastre uma conta local. Para encerrar, Ctrl+C em cada terminal.

Os scripts podem usar ferramentas portáteis existentes no computador de desenvolvimento; não presumir que elas existem em outro computador. O script da API usa H2 persistente e modo demonstrativo. Preserve banco e `.runtime/encryption.key` locais juntos, com custódia separada. Não apagar `.runtime` para “reiniciar o login” de contas com 2FA.

### Caminho manual e reproduzível

No PowerShell, a partir da raiz do repositório:

```powershell
java -version
node --version
npm --version
$segredo = Read-Host 'ENCRYPTION_KEY persistente desta instalacao' -AsSecureString
$env:ENCRYPTION_KEY = [System.Net.NetworkCredential]::new('', $segredo).Password
$env:WEB_ORIGIN = 'http://localhost:3000'
$env:SECURE_COOKIES = 'false'
$env:DEMO_MODE = 'false'
cd services/api
.\mvnw.cmd spring-boot:run
```

Em outro terminal, na raiz:

```powershell
cd apps/web
npm ci
npm run dev
```

API padrão: `http://127.0.0.1:8080/api/v1`. Sem DATABASE_URL o banco é H2 em `services/api/data`. Para PostgreSQL local, definir DATABASE_URL no formato JDBC, DATABASE_USER e DATABASE_PASSWORD no terminal da API. Não apontar testes para o PostgreSQL publicado.

### Docker Compose opcional

Instale Docker Desktop. Copie `.env.example` para `.env`, preencha POSTGRES_PASSWORD e ENCRYPTION_KEY com valores próprios, depois `docker compose up --build -d`. Abra localhost:3000; `docker compose logs -f api` ajuda no diagnóstico. Pare com `docker compose down`. Não use `down -v` se precisar preservar dados. O Compose mantém banco/API na rede interna e publica somente web no loopback. Este manual não afirma execução Docker quando o ambiente não a realizou.

## 4. Operar nuvem: Vercel, Render e Neon

O código fica em https://github.com/gsouzar/sentinela. Frontend: raiz `apps/web`, framework Next.js, Node 24. A variável Vercel `API_INTERNAL_URL` deve ser `https://sentinela-api-doxm.onrender.com`, sem `/api/v1`. Após alterá-la, realizar novo build/deploy, pois os rewrites são gerados no build.

API Render: Dockerfile `services/api/Dockerfile.render`, contexto `services/api`. Configurar DATABASE_URL JDBC, DATABASE_USER, DATABASE_PASSWORD, ENCRYPTION_KEY persistente, WEB_ORIGIN exata e SECURE_COOKIES=true. Se houver DATABASE_URL, o script ignora DATABASE_HOST/PORT/NAME para construir a URL: remover inconsistências operacionais.

Neon é o destino da migração registrada em MIGRACAO-NEON.md; conferir ESTADO-ATUAL.md para saber se o corte foi concluído. Para Java persistente com pool pequeno, uma conexão direta simplifica Flyway e operações de sessão; dumps/restore sempre usam conexão direta. Não colar `URI libpq copiada do provedor com credenciais embutidas` diretamente em DATABASE_URL: a aplicação espera `jdbc:postgresql://host:porta/banco?...`, com usuário/senha separados.

Alterações em variáveis Render devem preservar as demais chaves. Não substituir todo o ambiente por uma lista incompleta. Ao mudar só o banco, não recriar ENCRYPTION_KEY, TELEGRAM_API_KEY ou COLAB_CHAT_KEY.

Health em `/api/v1/health` indica que o processo HTTP responde, não comprova banco. Complementar com operação que consulte dados e com logs Flyway/Hikari. Frontend pode responder 200 mesmo com API indisponível.

## 5. Android Studio: abrir, configurar e testar

1. Instale Android Studio. Abra → Open → selecione a pasta **`apps/android`**, não o APK e não apenas a pasta `app`.
2. Espere o Gradle Sync. Em SDK Manager instale Android SDK 35 e ferramentas de build necessárias. O projeto usa AGP 8.9.3, Gradle 8.11.1, Java de código 17, minSdk 26 e target/compileSdk 35. JDK 17/21 deve ser compatível com o Gradle.
3. Para API local, inicie a API Java no computador. Em Device Manager crie um emulador API 35; o debug padrão aponta para `http://10.0.2.2:8080/api/v1`. Esse endereço representa o computador a partir do emulador.
4. Selecione módulo app e dispositivo, clique Run (triângulo verde).
5. Para usar a API publicada, abra o Terminal do Android Studio na raiz `apps/android` e execute:

```powershell
.\gradlew.bat assembleDebug -PapiBase=https://sentinela-api-doxm.onrender.com/api/v1
```

6. Instale o APK de `app/build/outputs/apk/debug/app-debug.apk` no aparelho. Alterar apiBase exige recompilar/reinstalar; não basta mudar uma variável no telefone.
7. Entre/cadastre na mesma API escolhida, faça análise, abra histórico, Meus sites e Sua segurança. Se tiver MFA ativo, use o código atual.
8. Para Run com a base online, definir a propriedade Gradle `apiBase` nas opções locais de build (por exemplo em `gradle.properties` local não compartilhado) ou usar o APK gerado pelo comando. Não colocar chave de backend no aplicativo.

No aparelho físico, `10.0.2.2` não representa o computador. Use HTTPS publicado ou, para teste USB local, `adb reverse tcp:8080 tcp:8080` e build com `-PapiBase=http://127.0.0.1:8080/api/v1`. Release bloqueia HTTP e precisa de assinatura de distribuição; o debug não é artefato pronto para loja. `local.properties` contém caminho do SDK específico da máquina e não deve ir ao Git.

Erros comuns: SDK location not found → configurar SDK no Studio; versão JDK incorreta → Settings → Build Tools → Gradle → Gradle JDK; conexão recusada → conferir API, base embutida e diferença emulador/celular. Não liberar HTTP globalmente como correção de produção. Keystore protege tokens; reinstalar/apagar dados pode encerrar a sessão local.

## 6. Explicador Groq no Colab

1. Abra https://colab.research.google.com → File/Arquivo → Upload notebook e envie `notebooks/Sentinela_Chatbot_Colab.ipynb`.
2. Na lateral, abra Secrets (chave). Cadastre GROQ_API_KEY da sua conta em https://console.groq.com/keys e GROQ_MODEL com ID exato disponível em https://console.groq.com/docs/models. Autorize o notebook a ler os Secrets.
3. Cadastre COLAB_CHAT_KEY com chave própria aleatória de pelo menos 32 caracteres e NGROK_AUTHTOKEN da conta em https://dashboard.ngrok.com/get-started/your-authtoken. Não confundir essas chaves com a chave Groq.
4. Execute as células em ordem: instalação, funções, exemplo, Secrets e diagnóstico. GPU não é necessária, pois o modelo é chamado por API.
5. Confira diagnóstico GROQ/OK. LOCAL significa fallback; verificar status e modelo/cota antes de atribuir resposta à IA.
6. Execute servidor Flask e túnel. Copie apenas a URL exibida como COLAB_CHAT_URL, terminando em `/explain`.
7. No Render → API → Environment, configure COLAB_CHAT_URL e COLAB_CHAT_KEY igual ao Secret Colab. Salve/reimplante e espere a API voltar.
8. No site, faça análise e converse sobre o resultado. COLAB indica passagem pelo notebook; o diagnóstico do notebook informa se o Groq respondeu.

Deixe runtime ligado durante demonstração. Se o Colab cair, o endpoint temporário deixa de existir e a API retorna à explicação local. Após novo túnel, atualizar URL. Erro “Authtoken already set” exige usar notebook atualizado com `PyngrokConfig`, não imprimir o token para depurar.

## 7. Telegram pelo Colab ou computador

1. No Telegram, use o @BotFather oficial → `/newbot`; crie nome/username e guarde TELEGRAM_BOT_TOKEN.
2. Defina IDs numéricos permitidos em TELEGRAM_ALLOWED_USER_IDS. Envie /start ao bot e siga o procedimento de `TELEGRAM.md` para descobrir seu ID sem divulgar token. Não é username nem telefone.
3. Gere TELEGRAM_API_KEY própria, diferente de todas as outras chaves. Configure-a na API Render e no processo Python com exatamente o mesmo valor.
4. No Colab, envie `notebooks/Sentinela_Telegram_Colab.ipynb` e, na célula de upload, selecione `notebooks/sentinela_telegram.py`.
5. Cadastre Secrets TELEGRAM_BOT_TOKEN, TELEGRAM_API_KEY, TELEGRAM_ALLOWED_USER_IDS e SENTINELA_API_BASE=`https://sentinela-api-doxm.onrender.com/api/v1`.
6. Execute todas as células em ordem e deixe a última célula de execução ativa. Nenhum outro programa deve usar o mesmo token em polling.
7. No Telegram, abra conversa privada com o bot e envie `/start`, depois `/analisar https://example.org` e `Por que essa pontuação?`.
8. Para parar, interrompa a célula/runtime. Para executar localmente, defina essas mesmas variáveis no PowerShell e rode `python notebooks/sentinela_telegram.py`.

Se o upload renomear para `sentinela_telegram (1).py`, o notebook atualizado aceita um único arquivo correspondente e salva como canônico. Se não responde, conferir se a última célula está rodando, ID permitido, mesmo segredo nos dois lados, base com `/api/v1`, API disponível e ausência de polling concorrente/webhook. Publicar o site não inicia o bot.

Não executar `/apagar CONFIRMAR` como teste sem intenção de apagar dados daquela identidade. Não enviar tokens em prints de erro; URLs da Bot API incluem o token.

## 8. Preparar apresentação

Um dia antes: conferir site/API, conta ADMIN e USER, relógios TOTP, notebook Groq, túnel atualizado, bot ativo e aplicativo com base correta. Se demonstrar monitoramento, manter TXT do domínio e fazer coleta real antecipadamente. Gráfico vazio é correto se não houve amostras.

Separar demonstração de URL heurística, reputação externa configurada, monitoramento HEAD e eventos do conector. Simulações devem aparecer como simulações. Ter H2 local como ambiente alternativo, com contas próprias e sem fingir que replica automaticamente a nuvem. Não projetar tela de Secrets, QR MFA, códigos de recuperação ou variáveis privadas.
