# Manual do Sentinela

Edição 31/10/2026 · Windows · Execução local


## 1 Conhecer as partes do projeto

Site Android Studio Colab e integrações

Este manual permite executar uma cópia local do Sentinela em um computador Windows, sem precisar acompanhar a conversa de desenvolvimento. Use o pacote completo da versão com monitoramento de sites. Cada pessoa que instalar sua própria cópia terá banco, usuários e chaves próprios.

| Parte | Função | Precisa de quê |
|---|---|---|

| API Java | Login, banco, regras e monitoramento | JDK 21; terminal aberto |

| Site Next.js | Área do cliente e administração | Node.js 24 LTS; API ligada |

| Android Java | Aplicativo nativo do cliente | Android Studio; API ligada |

| Colab | Estudo e explicação de análises | Conta Google; chaves opcionais |

| Telegram | Bot privado para analisar links | Python; token e IDs autorizados |

| Conector do site | Relatos do backend ou WAF | Domínio confirmado; chave do site |

### Ordem recomendada

Instalar ferramentas → ligar API e site → criar conta → testar análise de links → confirmar domínio para monitoramento → executar Android → testar Colab → configurar integrações opcionais.

> O site e o app funcionam sem Telegram e sem Groq. Monitorar um domínio exige controle do DNS. A URL pública permite verificações externas; eventos internos exigem conector. Nenhum indicador comprova segurança absoluta.

Edição de 31 de outubro de 2026. Roteiro principal: execução local para avaliação e TCC. Hospedagem pública e cobrança automática não fazem parte deste manual.


## 2 Preparar o computador

Faça uma vez em cada computador que receber o projeto

1. Baixe o ZIP completo e use **Extrair tudo** no Explorador. Coloque a pasta em um caminho curto, por exemplo **C:\Projetos\sentinela**. Não execute os CMDs de dentro do ZIP. Prefira uma pasta fora de sincronização de nuvem para dependências e builds.

2. Instale **JDK 21** pelo [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21) ou distribuição equivalente. Configure JAVA_HOME/PATH se o instalador oferecer. O projeto exige Java 21 na API.

3. Instale **Node.js 24 LTS**, com npm, pelo [site oficial](https://nodejs.org/en/download). Reabra os terminais depois da instalação.

4. Para o bot e o conector Python, instale **Python 3.11 ou superior** pelo [site oficial](https://www.python.org/downloads/). Habilite a opção de adicionar ao PATH, quando apresentada. Um atalho que só abre a Microsoft Store não é uma instalação funcional.

5. Instale [Android Studio](https://developer.android.com/studio) quando chegar à etapa Android. A instalação do site não depende dele. Tenha internet para baixar dependências, SDK e imagem do emulador.

### Abrir o terminal no lugar correto

No Explorador, entre na pasta que contém os dois CMDs. Clique na barra de endereço, digite **powershell** e pressione Enter. Confira:

```powershell
java -version
node --version
npm --version
python --version
```

> A pasta correta deve conter apps, services, dist, notebooks, connectors, docs, 1-INICIAR-API.cmd e 2-INICIAR-WEB.cmd. Se houver uma pasta sentinela dentro de outra, entre na pasta interna. PostgreSQL não é necessário no roteiro local: o Java usa H2 persistente.


## 3 Ligar o site e a API

São dois processos e os dois precisam permanecer abertos

1. Na raiz do projeto, dê dois cliques em **1-INICIAR-API.cmd**. Aguarde a mensagem de que o Spring iniciou na porta 8080. Se a janela mostrar erro, corrija-o antes de abrir o site.

2. No navegador do computador, abra **http://127.0.0.1:8080/api/v1/health**. O resultado deve conter **UP**. Essa página confirma que o Java responde.

3. Dê dois cliques em **2-INICIAR-WEB.cmd**. Na primeira execução, ele instala dependências e compila. Isso pode demorar e exige internet. Deixe a janela aberta.

4. Abra **http://localhost:3000**. Use esse endereço durante cadastro e login. Para conferir o proxy, **http://localhost:3000/api/v1/health** também deve mostrar UP.

5. Escolha **Criar conta**, preencha nome, email e senha com pelo menos 12 caracteres. Depois use **Entrar** nas próximas vezes. Não há login ou senha administrativa padrão.

6. A conta comum entra em **Meus sites**. Para um teste que não exige DNS, abra **Analisar link**, envie **https://example.org** e confira resultado e histórico.

### O que o inicializador faz

O CMD da API cria uma chave aleatória em **.runtime/encryption.key** se ela ainda não existe. Preserve essa chave junto do banco, que fica por padrão em **services/api/data**. A fonte sintética de demonstração fica habilitada no CMD e é identificada na interface.

> Para encerrar, use Ctrl+C em cada terminal. Não inicie duas APIs ou dois sites na mesma porta. Ao atualizar código já compilado, encerre o web, abra PowerShell em apps/web, execute npm run build e inicie o web novamente. Depois use Ctrl+F5 no navegador.


## 4 Usar a área do cliente

Meus sites e Analisar link têm funções diferentes

| Menu | O que fazer |
|---|---|

| Meus sites | Cadastrar seus domínios e acompanhar o monitoramento |

| Analisar link | Consultar sinais e reputação de uma URL pontual |

| Visão geral e Histórico | Acompanhar avaliações de links da sua conta |

| Assistente | Entender os resultados das análises de links |

| Minha segurança | Configurar 2FA, trocar senha e encerrar sessões |

1. Em **Meus sites**, clique **Adicionar site**. Informe um nome e a URL pública do domínio sob seu controle. Clique **Salvar site**.

2. O dashboard abre imediatamente. Antes de confirmar o domínio e receber amostras, os indicadores mostram **aguardando** ou **sem medição**.

3. Para uma análise pontual, use **Analisar link**. A pontuação é uma heurística de 0 a 100, não uma porcentagem de chance de golpe. Fontes sem chave aparecem como não configuradas.

4. No painel do domínio, use **Conversar sobre este site** para receber um resumo das medições registradas. Esse resumo é determinístico. A integração Colab descrita adiante pertence ao assistente de análises pontuais.

> O exemplo **https://banc0-seguranca-login.test** serve apenas ao analisador pontual e à fonte sintética de demonstração. Ele não é um domínio real de cliente e não é aceito como destino do monitor de produção.

Trocar a própria senha e gerenciar 2FA são recursos do cliente. Somente a conta ADMIN acessa a central de gestão de usuários e assinaturas.


## 5 Confirmar o domínio e monitorar

Você precisa poder editar a zona DNS do domínio cadastrado

1. No painel do site no Sentinela, copie o **Nome** e o **Valor** do registro de confirmação.

2. Entre no provedor que administra seu DNS. Abra **DNS**, **Gerenciar zona** ou **Registros** e escolha adicionar um registro do tipo **TXT**.

3. O nome completo terá o formato **_sentinela.seudominio.com.br**. Se o provedor já acrescenta seu domínio, digite somente **_sentinela**. No Valor, cole exatamente o texto que o Sentinela exibe, começando por **sentinela=**.

4. Salve o registro, aguarde a propagação e volte ao Sentinela. Clique **Já adicionei: confirmar domínio**. Se ainda não encontrar o TXT, confira nome e valor e tente depois.

5. Após confirmar, clique **Verificar agora**. O painel atualiza a cada 30 segundos. A coleta recorrente é programada aproximadamente a cada cinco minutos, sujeita à fila, enquanto a API está ligada.

6. Mantenha o TXT no DNS. Para mudar o domínio, clique **Editar site**. A alteração da origem exige novo TXT e revoga a chave do conector. O histórico anterior permanece separado por revisão. Use **Pausar** e **Retomar** sem apagar o cadastro.

| Indicador | Como interpretar |
|---|---|

| Disponibilidade | Percentual de amostras HTTP 2xx/3xx; não é medição contínua |

| Tempo da verificação | Inclui DNS, conexão, TLS e cabeçalhos de resposta |

| Certificado | Validade observada em conexão HTTPS validada |

| Achados | Presença ou ausência de cabeçalhos; requer análise do contexto |

| Eventos | Relatos do conector; simulações ficam separadas |

> Uma queda, erro HTTP ou ausência de cabeçalho não comprova ataque. O monitor consulta a raiz em HTTP/HTTPS nas portas padrão, não segue redirecionamentos e não explora vulnerabilidades. Se você não controla o DNS, use o analisador pontual.


## 6 Configurar segurança e administração

2FA é opcional para cliente e obrigatório para ADMIN na política atual

### Ativar o autenticador do cliente

1. Abra **Minha segurança**, confirme a senha e escolha **Configurar segundo fator**.

2. No site, escaneie o QR code em **Google Authenticator → + → Ler QR code**. Vale para ADMIN e cliente. Se necessário, use a chave manual (TOTP, seis dígitos/30 segundos). No Android nativo, use a chave manual.

3. No Sentinela, confirme senha e código atual. Guarde os **oito códigos de recuperação** em local protegido fora do celular. São exibidos uma vez e cada um só funciona uma vez.

4. Nos próximos logins, informe email, senha e TOTP. Se acabou de usar um código, aguarde o próximo. Um código de recuperação pode substituir o TOTP. Ative hora automática no celular.

> Se perder o autenticador e todos os códigos, esta versão não oferece recuperação automática por email. Trocar a senha encerra as sessões. REQUIRE_MFA=true pode exigir 2FA também de todos os clientes.

### Criar a primeira conta administrativa

Escolha um email que ainda não esteja cadastrado nesse banco. Pare a API e execute no PowerShell da raiz:

```powershell
$env:ADMIN_EMAIL=Read-Host 'Email administrativo novo'
$segredo=Read-Host 'Senha nova com 16 caracteres ou mais' -AsSecureString
$env:ADMIN_PASSWORD=[System.Net.NetworkCredential]::new('', $segredo).Password
.\1-INICIAR-API.cmd
```

Entre por **Entrar**, ative o 2FA obrigatório e abra a central administrativa. Situações Inadimplente e Suspenso encerram sessões e pausam novas coletas, preservando dados. Ativo e Avaliação reabilitam acesso. O vencimento é informativo; não há cobrança automática. Após provisionar, pare a API, remova ADMIN_EMAIL e ADMIN_PASSWORD do ambiente e reinicie sem eles.

```powershell
Remove-Item Env:ADMIN_EMAIL,Env:ADMIN_PASSWORD -ErrorAction SilentlyContinue
.\1-INICIAR-API.cmd
```


## 7 Abrir no Android Studio

Importe a pasta do código Android e execute primeiro no emulador

1. Abra Android Studio e escolha **Open**. Se o projeto principal estiver aberto, use **File → Open**.

2. Selecione **C:\Projetos\sentinela\apps\android**. Se recebeu o ZIP Android separado, selecione a pasta **android** extraída. Ela deve conter settings.gradle, build.gradle e gradlew.bat. Não selecione o APK como projeto.

3. Aguarde **Gradle Sync**. No **SDK Manager**, instale Android SDK Platform 35 e as ferramentas solicitadas. A sincronização inicial precisa de internet.

4. Se houver erro de JDK, abra **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** e selecione um JDK compatível 17 ou 21. Não aponte para uma instalação Java antiga.

5. Abra **Tools → Device Manager**, crie um dispositivo virtual de celular, escolha uma imagem API 35, baixe-a e finalize. Inicie o emulador pelo botão de execução do dispositivo.

6. No computador, deixe a API Java ligada na porta 8080. Na barra superior do Studio, selecione o módulo **app** e o emulador. Clique **Run**, o botão com triângulo verde.

### Conferência

O app deve abrir com a marca Sentinela. Entre com uma conta existente da mesma API. O endereço debug padrão é **http://10.0.2.2:8080/api/v1**. Dentro do emulador, 10.0.2.2 representa o computador que executa a API.

> O site não precisa permanecer aberto para o Android funcionar; a API precisa. O app suporta no mínimo API 26. O APK entregue foi compilado e analisado por lint; teste navegação e Keystore no seu emulador ou aparelho antes da apresentação.

Referência: [Build and run your app no Android Studio](https://developer.android.com/studio/run). Os nomes dos menus podem variar com a versão do IDE.


## 8 Usar o app e um celular físico

Conta compartilhada com o site e conexão explícita com a API

Na tela inicial do app, use **Meus sites · monitoramento** para cadastrar, editar, confirmar DNS, pausar e consultar métricas. A geração de chave de backend e o controle administrativo de assinaturas são feitos na área web. A análise pontual, histórico, assistente e segurança da conta continuam disponíveis.

### Celular por USB para teste local

1. No Android, habilite as opções do desenvolvedor e **Depuração USB**. Conecte o cabo e autorize o computador quando o celular perguntar.

2. No terminal do Android Studio, na pasta apps/android, execute os comandos abaixo. Se adb não for encontrado, use o executável dentro de **platform-tools** do SDK, cujo caminho aparece no SDK Manager.

3. Instale o APK gerado em **app/build/outputs/apk/debug/app-debug.apk**, por exemplo pelo terceiro comando. Mantenha a API ligada e repita adb reverse quando reconectar o aparelho.

```powershell
adb reverse tcp:8080 tcp:8080
.\gradlew.bat assembleDebug '-PapiBase=http://127.0.0.1:8080/api/v1'
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> O APK pronto em dist/Sentinela-debug.apk usa o endereço do emulador 10.0.2.2. Para USB com adb reverse, gere a variante acima. Se a assinatura de uma instalação anterior for diferente, não a remova sem considerar os dados locais; use um ambiente de teste separado.

### Distribuição e atualização

Para usar uma API hospedada, configure um endereço HTTPS real e a assinatura de release no Android Studio. Não distribua uma chave de integração de backend dentro do app. O release bloqueia HTTP. O FLAG_SECURE bloqueia capturas de tela do aplicativo para proteger dados sensíveis.

```powershell
.\gradlew.bat assembleRelease '-PapiBase=https://api.seu-dominio.example/api/v1'
```

O endereço acima é um exemplo: substitua por uma API sua, acessível e com certificado válido. O app não hospeda a API.


## 9 Executar o notebook do Colab

Comece pela demonstração independente sem Groq

1. Entre no [Google Colab](https://colab.research.google.com/) com sua conta Google. Escolha abrir notebook e a opção **Upload / Fazer upload**.

2. Selecione **notebooks/Sentinela_Chatbot_Colab.ipynb** do projeto extraído. Não envie o ZIP completo ou o JAR. Esse notebook já contém o código do explicador.

3. Clique **Conectar**. Para esse notebook, um ambiente CPU padrão é suficiente: ele usa Python e, opcionalmente, chama uma API de modelo externa.

4. Execute o primeiro bloco de instalação pelo botão de execução à esquerda. Espere terminar. Execute depois o bloco de funções do explicador.

5. Execute o exemplo em **Demonstração sem credenciais**. A saída deve mostrar uma explicação baseada nos indicadores da amostra. A amostra é sintética.

6. A célula de avaliação calcula métricas de um exemplo. Não apresente seus números como eficácia real contra golpes. Para isso, seria necessário um conjunto rotulado e avaliação independente.

### Não confundir os dois notebooks

| Arquivo | Finalidade |
|---|---|

| Sentinela_Chatbot_Colab.ipynb | Explicador de análises de links e endpoint opcional para a API Java |

| Sentinela_Telegram_Colab.ipynb | Alternativa para executar o bot Telegram no Colab; exige enviar também sentinela_telegram.py e uma API Java HTTPS acessível |

> Rodar o exemplo do notebook não conecta automaticamente o site ao Colab. Para integrar, siga a próxima página. O Colab tem limites de execução e pode encerrar a sessão; não é hospedagem permanente. Consulte a [FAQ oficial](https://research.google.com/colaboratory/faq.html).


## 10 Conectar Colab Groq e site

Configure Secrets no Colab e variáveis no terminal da API Java

1. Para Groq, entre no [Groq Console](https://console.groq.com/keys), abra **API Keys** e gere uma chave para a conta/projeto que vocês usam. As permissões, cotas e condições dependem da conta.

2. No Colab, clique no ícone de chave **Secrets** da barra lateral. Crie **GROQ_API_KEY** com a chave e **GROQ_MODEL** com o ID exato de um modelo de chat disponível na sua conta, compatível com JSON Object Mode. Obtenha o ID no Playground ou na lista de modelos. Autorize o acesso do notebook aos dois Secrets.

3. Gere uma chave compartilhada exclusiva com o comando abaixo. Guarde o valor e crie o Secret **COLAB_CHAT_KEY** com ele. Não use a chave Groq nesse campo.

4. Para o túnel opcional, entre no [painel ngrok](https://dashboard.ngrok.com/), obtenha seu authtoken e crie o Secret **NGROK_AUTHTOKEN**. Execute, em ordem, a célula de Secrets, a do servidor e a do túnel.

5. Copie a URL HTTPS exibida pelo notebook, terminando em **/explain**. Pare a API com Ctrl+C e configure as variáveis no PowerShell da raiz do projeto conforme o segundo bloco. Inicie novamente nesse mesmo terminal.

6. No site, analise um link e faça uma pergunta contextual no assistente de análises. O modo **COLAB** confirma passagem pelo notebook. Se Groq falhar, o próprio notebook pode usar explicação local; COLAB não comprova sozinho geração por Groq.

```powershell
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

```powershell
$env:COLAB_CHAT_URL='https://SEU-TUNEL/explain'
$segredo=Read-Host 'Mesma COLAB_CHAT_KEY definida no Colab' -AsSecureString
$env:COLAB_CHAT_KEY=[System.Net.NetworkCredential]::new('', $segredo).Password
.\1-INICIAR-API.cmd
```

> Antes do túnel, execute a célula de diagnóstico: GROQ / OK confirma resposta Groq validada; LOCAL indica explicação de reserva. A URL SEU-TUNEL é um exemplo: substitua pela URL real. Quando o túnel mudar, atualize a variável e reinicie a API. Ao terminar, execute a célula de encerramento do túnel e finalize o runtime. Não exponha chaves na projeção ou em saídas compartilhadas.


## 11 Ativar as fontes reais de reputação

As duas chaves são de servidor e não vão para o navegador

### Google Safe Browsing

1. Entre no Google Cloud Console, crie ou selecione um projeto e abra **APIs e serviços → Biblioteca**.

2. Procure **Safe Browsing API** e habilite-a. Em **Credenciais**, crie uma chave de API e restrinja-a à API necessária. A chamada parte do Java; uma restrição de referenciador de navegador não é a configuração adequada para esse uso de servidor.

3. Guarde a chave para **GOOGLE_SAFE_BROWSING_KEY**. Ela é diferente da chave Groq. Confira termos e cotas na [documentação oficial da API v4](https://developers.google.com/safe-browsing/v4/get-started).

### URLhaus

Abra a [API URLhaus](https://urlhaus.abuse.ch/api/), clique **Obtain an Auth-Key** e entre no portal abuse.ch indicado pela própria página. Gere seu Auth-Key para **URLHAUS_KEY**. Não use a senha da conta como chave.

### Preencher no terminal da API

Pare a API, mantenha o mesmo PowerShell para preservar outras variáveis e execute:

```powershell
$segredo=Read-Host 'Chave Google Safe Browsing' -AsSecureString
$env:GOOGLE_SAFE_BROWSING_KEY=[System.Net.NetworkCredential]::new('', $segredo).Password
$segredo=Read-Host 'Auth-Key URLhaus' -AsSecureString
$env:URLHAUS_KEY=[System.Net.NetworkCredential]::new('', $segredo).Password
.\1-INICIAR-API.cmd
```

Faça uma **nova análise** de https://example.org e confira as fontes. **Não configurada** indica chave ausente; **Indisponível** indica consulta não concluída. Histórico antigo não se atualiza sozinho. A API pode manter cache por cinco minutos; reiniciar limpa esse cache.

> Essas fontes pertencem à análise pontual de links. Elas não substituem as medições HTTP/TLS do monitor de domínios. Respeite as condições de uso das contas e não apresente a base sintética .test como consulta real.


## 12 Criar e preparar o bot Telegram

Faça esta configuração antes de iniciar o polling

1. No Telegram, abra o [BotFather oficial](https://t.me/BotFather), envie **/newbot** e siga as instruções para nome e username. Guarde o token sem compartilhá-lo.

2. Pelo BotFather, use **/setjoingroups** para desativar participação em grupos. O programa também recusa grupos e responde somente a IDs autorizados em conversa privada.

3. Use **/setuserpic**, selecione seu bot e envie **apps/web/public/sentinela-logo.png** para definir a foto.

4. Abra a conversa com o novo bot e envie **/start** do seu usuário. Antes de ligar o programa, obtenha seu ID numérico com os comandos abaixo. Não execute outra instância de polling ao mesmo tempo.

```powershell
$segredo=Read-Host 'Token recebido do BotFather' -AsSecureString
$env:TELEGRAM_BOT_TOKEN=[System.Net.NetworkCredential]::new('', $segredo).Password
$uri='https://api.telegram.org/bot'+$env:TELEGRAM_BOT_TOKEN+'/getUpdates'
try {
  $resposta=Invoke-RestMethod -Method Post -Uri $uri `
    -ContentType 'application/json' `
    -Body '{"timeout":0,"allowed_updates":["message"]}'
} catch { throw 'Falha ao consultar Telegram. Confira token e conexão.' }
$resposta.result | ForEach-Object { $_.message.from.id } | Select-Object -Unique
```

Anote o ID numérico do seu usuário, não o @username nem o ID do bot. Se não aparecer, envie outra mensagem e consulte de novo. Caso o bot já tenha webhook, remova-o conscientemente antes de mudar para polling; o programa não apaga essa configuração sozinho.

> O token integra a URL da Bot API. Não compartilhe erros brutos do terminal, capturas ou logs que possam contê-lo. Se um token vazar, revogue-o pelo BotFather. Referência: [recursos oficiais e BotFather](https://core.telegram.org/bots/features#botfather).


## 13 Executar o Telegram

Uma chave liga o bot à API e uma lista define quem pode usá-lo

### No PowerShell da API

Gere outra chave aleatória com o comando da página de Colab. Guarde-a como chave exclusiva da integração Telegram. Pare a API e informe:

```powershell
$segredo=Read-Host 'Chave de integração Telegram gerada por você' -AsSecureString
$env:TELEGRAM_API_KEY=[System.Net.NetworkCredential]::new('', $segredo).Password
.\1-INICIAR-API.cmd
```

### Em outro PowerShell na raiz do projeto

```powershell
$segredo=Read-Host 'Token do BotFather' -AsSecureString
$env:TELEGRAM_BOT_TOKEN=[System.Net.NetworkCredential]::new('', $segredo).Password
$segredo=Read-Host 'Mesma TELEGRAM_API_KEY usada pela API' -AsSecureString
$env:TELEGRAM_API_KEY=[System.Net.NetworkCredential]::new('', $segredo).Password
$env:TELEGRAM_ALLOWED_USER_IDS='SEU_ID_NUMERICO'
$env:SENTINELA_API_BASE='http://127.0.0.1:8080/api/v1'
python notebooks/sentinela_telegram.py
```

Substitua SEU_ID_NUMERICO pelo ID obtido anteriormente. Separe vários IDs autorizados por vírgula. Uma lista vazia impede iniciar. No Telegram, envie **/start**, **/analisar https://example.org** e depois uma pergunta sobre o resultado. **/privacidade** explica os dados; **/apagar CONFIRMAR** remove análises e chat dessa identidade na API, mas não apaga mensagens do Telegram.

> O bot usa uma identidade técnica separada da conta do site e não pede sua senha ou 2FA. O bot atual analisa links; não administra a assinatura nem substitui o dashboard de monitoramento. Ctrl+C encerra o programa. Mantenha uma única instância com o mesmo token.

### Alternativa pelo Colab

Envie **Sentinela_Telegram_Colab.ipynb**, depois **sentinela_telegram.py** quando a célula pedir. Cadastre nos Secrets os quatro nomes usados no segundo bloco. Nesse cenário, SENTINELA_API_BASE deve ser uma API Java **HTTPS acessível externamente**. O localhost do Colab é o próprio Colab, não seu computador. Para começar, prefira o bot local.


## 14 Instalar o conector do site monitorado

Somente o backend do cliente deve guardar essa chave

1. Confirme o domínio por TXT. No dashboard, abra **Integração do seu site → Gerar chave de integração**.

2. Confirme a senha e, se ativo, um TOTP ainda não usado. Copie a chave mostrada uma vez. Guarde-a no ambiente privado do backend ou WAF do cliente.

3. Use **connectors/SentinelaReporter.java** no backend Java, ajustando o pacote, ou **connectors/site_event_client.py** no backend Python. Dispare eventos apenas onde a aplicação realmente identificou uma ocorrência.

4. O destino é **/api/v1/ingest/events** da API Sentinela, com cabeçalho **X-Sentinela-Site-Key**. A identidade vem da chave; o emissor não escolhe livremente outro site. Rotacionar a chave ou trocar o domínio revoga a chave anterior.

| Tipo | O que o emissor identificou |
|---|---|

| AUTH_FAILURE | Autenticação recusada |

| RATE_LIMIT | Limite de requisições atingido |

| WAF_BLOCK | Requisição bloqueada pelo WAF |

| INTEGRITY_ALERT | Alerta de integridade identificado |

| APPLICATION_ERROR | Erro da aplicação |

### Testar o caminho com uma simulação identificada

```powershell
$segredo=Read-Host 'Chave exclusiva do site' -AsSecureString
$env:SENTINELA_SITE_KEY=[System.Net.NetworkCredential]::new('', $segredo).Password
$env:SENTINELA_EVENT_URL='http://127.0.0.1:8080/api/v1/ingest/events'
python connectors/site_event_client.py --simulate --type AUTH_FAILURE
```

> O evento de teste aparece como simulação e não conta como ataque real. O endpoint exige domínio confirmado, conta/site ativos e horário válido. A URL pública, sozinha, não lê logs privados. Nunca envie senhas, tokens humanos, corpos de requisição ou dados pessoais no evento.


## 15 Entender variáveis caminhos e endereços

Use os nomes exatos e configure cada valor no processo correto

| Nome | Onde fica e de onde vem |
|---|---|

| ENCRYPTION_KEY | CMD da API gera em .runtime/encryption.key; preserve com o banco |

| GOOGLE_SAFE_BROWSING_KEY | Terminal da API; chave do Google Cloud para Safe Browsing |

| URLHAUS_KEY | Terminal da API; Auth-Key do portal abuse.ch |

| COLAB_CHAT_URL | Terminal da API; URL HTTPS do túnel terminando em /explain |

| COLAB_CHAT_KEY | Secret do Colab e terminal da API; valores iguais |

| GROQ_API_KEY | Secret do notebook explicador; chave Groq Console |

| GROQ_MODEL | Secret do notebook explicador; ID do modelo de chat da conta |

| NGROK_AUTHTOKEN | Secret do notebook explicador; authtoken do painel ngrok |

| TELEGRAM_API_KEY | Terminal da API e terminal do bot; valores iguais |

| TELEGRAM_BOT_TOKEN | Terminal do bot ou Secret do notebook Telegram; BotFather |

| TELEGRAM_ALLOWED_USER_IDS | Terminal do bot; IDs numéricos positivos separados por vírgula |

| SENTINELA_API_BASE | Terminal do bot; base da API incluindo /api/v1 |

| SENTINELA_SITE_KEY | Backend do site do cliente; chave emitida no dashboard |

| SENTINELA_EVENT_URL | Backend do cliente; endpoint completo /api/v1/ingest/events |

As variáveis do PowerShell duram enquanto a janela fica aberta. Ao parar a API com Ctrl+C e reiniciar no mesmo terminal, elas continuam definidas. Ao fechar a janela, informe-as novamente. Variáveis de um terminal não aparecem automaticamente no outro.

> Preencher .env não configura automaticamente o Java iniciado pelos CMDs. .env.example é um modelo usado pelo Compose. Este roteiro configura o ambiente pelo PowerShell. Não coloque chaves de servidor em apps/web ou no Android distribuído.

**localhost** aponta à própria máquina; **10.0.2.2** aponta ao computador a partir do emulador Android. Para outro computador ou um Colab remoto, é necessário um endereço acessível a partir dele. Não altere IPs ao acaso para contornar validação TLS ou de origem.


## 16 Compartilhar executar de novo e conferir

Envie código limpo e este manual sem dados privados da sua instalação

### O que mandar para outra pessoa

Envie **Sentinela-projeto-completo.zip** da entrega e este manual. O pacote completo contém API/JAR, web, Android, notebooks, conectores e guias. Os ZIPs separados são conveniências: sozinhos, o Android e o bot ainda precisam de uma API executando.

Não compacte a pasta de trabalho inteira depois de usar o projeto sem revisar seu conteúdo. Não compartilhe **.runtime**, bancos **data**, .env com valores reais, keystores, tokens, chaves ou códigos de recuperação. node_modules, .next, target e build são caches/saídas de compilação e não precisam ser enviados.

### Cada pessoa roda uma cópia local

Cada instalação cria seu próprio banco e suas contas. Um cadastro em um computador não aparece automaticamente no outro. Para uma turma compartilhar a mesma base, seria preciso hospedar uma API comum e configurar HTTPS, origens, banco e segredos. Isso é uma etapa de implantação separada; Colab não deve ser tratado como hospedagem permanente.

### Rotina de execução

1. Abra a pasta certa, configure as variáveis necessárias e ligue a API. Confira health UP.

2. Ligue o web e abra localhost:3000. Ligue o emulador se for usar Android.

3. Reconecte o Colab e atualize a URL do túnel na API quando mudar. Inicie o bot local se for usar Telegram.

4. Confirme uma análise no histórico, uma amostra do domínio verificado e, se instalado, um evento do conector. Observe de onde vem cada dado.

5. Ao terminar, Ctrl+C nos terminais, interrompa as células do bot, encerre o túnel e finalize o runtime do Colab.

> Antes de atualizar uma instalação existente, faça backup do banco e da chave de criptografia. Preserve os dois juntos; trocar a chave indiscriminadamente impede verificar segredos 2FA já gravados. Leia docs/VERIFICACAO.md para o que foi testado e o que depende de execução real no seu ambiente.


## 17 Resolver problemas comuns

Confira a mensagem do processo que falhou antes de reiniciar tudo

| Sintoma | O que fazer |
|---|---|

| Site abre mas API não responde | Abra health na porta 8080. Confira Java21 e o erro na janela da API. |

| Porta 8080 ou 3000 em uso | Encerre a instância anterior do próprio projeto com Ctrl+C. Não abra CMDs duplicados. |

| Java ou npm não encontrado | Conclua instalação e PATH; reabra o terminal. Configure JAVA_HOME para recompilar API. |

| Python abre Microsoft Store | Instale o interpretador real pelo site oficial e reabra o terminal. |

| Cannot find module ou dependências faltando | Pare o web. Em apps/web, execute npm ci e depois npm run build. |

| EPERM ou EBUSY no build | Encerre web/IDE que usem os arquivos. Prefira uma cópia limpa fora de sincronização em nuvem; preserve banco e chave antes de atualizar. |

| Logo ou interface antiga | Pare o web, compile com npm run build, inicie novamente e use Ctrl+F5. |

| Cadastro falha com mensagem genérica | Confira os dois healths e a janela Java. Não apague banco/chave como tentativa de correção. |

| TOTP recusado | Ative hora automática; use chave correta e código ainda não utilizado. Aguarde próximo intervalo. |

| Android sem conexão | API ligada; emulador usa 10.0.2.2; USB exige adb reverse e o build de localhost. |

| DNS TXT não encontrado | Confira nome/valor, sufixo acrescentado pelo provedor e propagação. Mantenha o registro. |

| Gráficos vazios | Confirme domínio e aguarde coletas com API ligada. Ausência de dados é mostrada explicitamente. |

| Fonte de reputação não configurada | Preencha a chave na API e reinicie; faça nova análise. |

| Colab não aparece na resposta | Confira URL HTTPS com /explain, chave igual, servidor/túnel ligados e reinício da API. |

| Bot não responde | Confira API, chave de integração, token, ID autorizado, conversa privada e uma única instância. |

| Rotas de sites retornam 404 | Confirme que iniciou o JAR atualizado da mesma pasta do web; a versão inclui migração V2. |

> Ao pedir ajuda, envie componente, etapa e mensagem de erro sanitizada. Remova tokens, senhas, códigos 2FA e URLs que incorporem chaves. Não compartilhe dumps de ambiente ou arquivos de banco.


## 18 Conferência final e referências

Marque apenas etapas que realmente concluiu no seu ambiente

| Conferência | Resultado esperado |
|---|---|

| API e proxy | Os dois endereços health retornam UP |

| Conta do cliente | Login abre Meus sites; administração não aparece |

| Análise pontual | Resultado salvo e encontrado no Histórico |

| Segurança | TOTP funciona se ativado; recuperação guardada em local seguro |

| Monitoramento | Seu TXT confirmado; primeiras amostras persistidas |

| Troca de domínio | Nova confirmação necessária e revisão atualizada |

| Android | App executa no seu emulador/aparelho e usa a mesma conta da API |

| Colab | Exemplo executa; modo COLAB aparece quando a integração está ativa |

| Reputação | Fontes respondem, com situação da consulta visível |

| Telegram | Seu ID autorizado recebe análise e resposta contextual |

| Conector | Teste aparece como simulação; emissor real instalado apenas no backend |

| Administração | Conta ADMIN com 2FA controla acesso e assinatura manual |

A versão do projeto foi verificada com testes automatizados e builds conforme docs/VERIFICACAO.md. Isso não garante funcionamento de credenciais de terceiros, disponibilidade do seu DNS ou execução em aparelho ainda não testado. As integrações precisam ser validadas com suas próprias contas.

### Referências oficiais e arquivos do projeto

[Android Studio](https://developer.android.com/studio/run) · [Colab e limites](https://research.google.com/colaboratory/faq.html) · [Groq e chaves](https://console.groq.com/docs/quickstart) · [ngrok](https://ngrok.com/docs/start) · [Telegram BotFather](https://core.telegram.org/bots/features#botfather) · [Safe Browsing v4](https://developers.google.com/safe-browsing/v4/get-started) · [URLhaus](https://urlhaus.abuse.ch/api/). Consultados para este manual em 31/10/2026; nomes de menus podem mudar.

No pacote: README.md, INICIO-AQUI.html, docs/MONITORAMENTO.md, docs/TELEGRAM.md, docs/VERIFICACAO.md e docs/openapi.json. Os guias do projeto complementam este roteiro com os limites técnicos e contratos de integração.
