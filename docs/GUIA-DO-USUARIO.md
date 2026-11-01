# Comece por aqui: como usar o Sentinela

**Novo fluxo principal:** login → Meus sites → Adicionar site → confirmação DNS → painel de monitoramento. O [guia de monitoramento](MONITORAMENTO.md) detalha a troca de domínio, gráficos, eventos reais do conector, administração e assinatura manual. Os passos de análise pontual abaixo continuam disponíveis. 2FA é opcional para clientes e obrigatório para administradores.

## O que cada parte faz

| Parte | Para que serve | O que você abre |
|---|---|---|
| Site Next.js | Cadastro, 2FA, análise de links, resultado, histórico, dashboard e assistente | Navegador em http://localhost:3000 |
| API Java | Autentica, aplica regras, consulta fontes configuradas e guarda os dados | `1-INICIAR-API.cmd`, que fica aberto |
| App Android Java | Usa os recursos principais pelo celular/emulador, com a mesma conta | Pasta `apps/android` no Android Studio |
| Notebook Colab do chatbot | Estuda e demonstra as explicações; pode acrescentar Groq à explicação | `notebooks/Sentinela_Chatbot_Colab.ipynb` |
| Bot Telegram | Recebe links e explica a análise em conversa privada autorizada | Script `notebooks/sentinela_telegram.py` |
| Notebook Telegram no Colab | Alternativa para rodar o bot em um notebook com API acessível por HTTPS | `notebooks/Sentinela_Telegram_Colab.ipynb` |

O site e o Android usam a mesma conta e o mesmo banco da API. O Telegram tem uma identidade técnica separada por conversa; ele não pede sua senha ou 2FA do Sentinela. Você não precisa iniciar Colab ou Telegram para usar o site.

## 1. Colocar o site para funcionar

Preparação no Windows: baixe o [JDK 21](https://adoptium.net/temurin/releases/?version=21), [Node.js 24 LTS](https://nodejs.org/en/download), [Python](https://www.python.org/downloads/) e [Android Studio](https://developer.android.com/studio). No instalador Python, marque a opção de adicionar ao PATH, quando apresentada. No JDK, habilite JAVA_HOME/PATH quando o instalador oferecer. Reabra o terminal após instalar. Android Studio e Python só são necessários quando chegar às respectivas partes.

Abra o Explorador, entre na pasta extraída e procure o diretório que contém os dois CMDs. Para abrir PowerShell nessa pasta, clique na barra de endereço, digite `powershell` e pressione Enter. Você estará na raiz correta se `dir` mostrar `apps`, `services`, `notebooks` e `dist`. Confira `java -version`, `node --version` e `python --version`. Os comandos abaixo presumem essa raiz, sem trocar de pasta.

1. Extraia o ZIP completo. Na pasta `sentinela`, você deve ver os dois arquivos CMD, `apps`, `services`, `notebooks` e `dist`.
2. Tenha **JDK 21** e **Node.js 24 LTS** instalados. Não é necessário instalar PostgreSQL para a demonstração local.
3. Abra `1-INICIAR-API.cmd` e deixe aberto. Espere a mensagem de que o Spring iniciou. Se disser que a porta 8080 está em uso, pode já existir uma API ligada: confira a janela anterior antes de iniciar outra.
4. Abra `2-INICIAR-WEB.cmd` em outra janela. A primeira execução instala dependências e compila; precisa de internet.
5. Entre em **http://localhost:3000**. Mantenha as duas janelas abertas enquanto usar o sistema.
6. Ao terminar, use Ctrl+C nas duas janelas. Não é preciso apagar banco ou chave entre execuções.

Se você atualizou os arquivos do site em uma pasta que já tinha um build, encerre o terminal web, entre em `apps/web`, execute `npm run build` e então execute novamente `2-INICIAR-WEB.cmd`. Use Ctrl+F5 no navegador. O CMD só compila automaticamente quando ainda não existe build.

## 2. Criar conta e configurar o segundo fator

1. Na tela inicial, escolha **Criar conta**, preencha nome, email e senha de pelo menos 12 caracteres.
2. Após o cadastro, abra **Minha segurança** para configurar o fator opcional da conta de cliente. Escolha **Configurar segundo fator** e confirme sua senha.
3. No Google Authenticator, toque **+ → Ler QR code** e escaneie o QR code exibido no site. A conta Sentinela será adicionada. Se não puder escanear, abra **Não consegue escanear? Use a chave manual** e escolha TOTP, seis dígitos/30 segundos. O fluxo do site vale para ADMIN e usuário. No Android nativo, continua disponível a chave manual.
4. Volte ao Sentinela e confirme a senha e o código atual do autenticador.
5. Guarde os oito códigos de recuperação em lugar protegido, fora do celular. Cada código só pode ser usado uma vez.
6. Nos próximos logins, use email, senha e código do autenticador. Se acabou de usar um código, aguarde o próximo. Não mande códigos ao chatbot.

Não existe conta pronta com senha padrão. Quem já tem conta escolhe **Entrar**. Se perder autenticador e todos os códigos, esta versão não oferece recuperação automática por email.

## 3. Fazer uma análise e entender o resultado

1. Clique **Analisar link**.
2. Cole um endereço completo começando com `https://` ou `http://`. Para começar, use `https://example.org`.
3. Clique no botão de análise. O Java verifica regras locais e as fontes de reputação que estiverem configuradas.
4. Leia o **score**, o nível **baixo/médio/alto**, os indicadores e a situação de cada fonte.
5. Se aparecer **cobertura parcial** ou **não configurada**, alguma fonte não foi consultada. Isso é mostrado para você saber o limite do resultado.
6. Abra o assistente para perguntar por que aquela análise recebeu a pontuação.

O sistema não abre nem executa o site suspeito. Remove query e fragmento antes de guardar o link. O score vai de 0 a 100, mas **não é porcentagem de chance de golpe**. Baixo risco não garante que um endereço seja seguro.

No inicializador de demonstração, `https://banc0-seguranca-login.test` serve para mostrar um resultado alto com base **sintética**, explicitamente identificada. Use esse exemplo no TCC sem apresentar como consulta de ameaça real.

## 4. Usar os outros menus do site

- **Visão geral:** acompanha totais e distribuição das análises da sua conta.
- **Histórico:** encontra análises anteriores, filtra resultados e abre detalhes. Você também pode excluir uma análise.
- **Assistente:** explica indicadores e cuidados com base no resultado selecionado. A explicação local funciona sem Groq.
- **Minha segurança:** mostra 2FA, troca de senha e sessões ativas. A troca de senha encerra sessões.
- **Administração:** aparece para ADMIN e permite administrar usuários e consultar auditoria. Criar uma conta comum não dá privilégios de administrador; veja README.md para provisionamento inicial.

## 5. Abrir e executar o mobile

1. Extraia `Sentinela-Android-Studio.zip`, ou use `apps/android` do projeto completo. Ambos contêm o mesmo código Android.
2. No Android Studio, clique **Open** e selecione a pasta `android` que contém `settings.gradle`. O APK não é o projeto para editar.
3. Espere o **Gradle Sync**. Instale Android SDK 35 pelo SDK Manager quando solicitado e use JDK 17/21 compatível.
4. Abra o **Device Manager**, crie um emulador API 35 e inicie-o.
5. Deixe a API Java ligada no computador. Para o app no emulador, não é necessário deixar o site aberto.
6. Escolha o módulo **app**, selecione o emulador e clique **Run ▶**. Esse é o fluxo indicado na [documentação do Android Studio](https://developer.android.com/studio/run).
7. Entre na mesma conta do site. Faça uma análise, abra o histórico e converse sobre o resultado. Os dados vêm da mesma API.

O debug usa `http://10.0.2.2:8080/api/v1`: dentro do emulador, esse IP aponta ao computador. O APK pronto está em `dist/Sentinela-debug.apk`; ele é de depuração e usa esse endereço. Para celular físico por USB ou distribuição HTTPS, siga a seção Android Studio em **COMO-RODAR.md**. O APK foi compilado, mas ainda precisa de teste de navegação em dispositivo.

## 6. Usar o Colab primeiro sem chaves

1. Abra [Google Colab](https://colab.research.google.com/) e escolha abrir/enviar um notebook.
2. Envie **`notebooks/Sentinela_Chatbot_Colab.ipynb`**. Não envie o ZIP completo nem o JAR.
3. Conecte o ambiente e execute, em ordem, a célula de instalação, a célula com as funções e a célula de exemplo sintético. O notebook já contém o código do explicador; não precisa enviar o `.py` para essa demonstração.
4. Observe a explicação da amostra. Isso demonstra o chatbot de forma independente; ainda não significa que o site esteja conectado ao Colab.
5. A célula de avaliação calcula métricas de um exemplo sintético. Não use esses números como prova de eficácia contra golpes reais.

## 7. Conectar a explicação do Colab ao site

1. No painel **Secrets** do Colab, adicione `COLAB_CHAT_KEY` com uma chave aleatória de pelo menos 32 caracteres. Se quiser Groq, adicione sua `GROQ_API_KEY`. Para o túnel opcional, adicione `NGROK_AUTHTOKEN` da sua conta ngrok. Autorize o acesso do notebook aos segredos.
2. Execute a célula que carrega os segredos, depois a que inicia o servidor e a que abre o túnel.
3. Copie a URL HTTPS exibida com `/explain` no final.
4. Encerre a API Java com Ctrl+C. Abra PowerShell **na raiz da pasta sentinela** e configure:

```powershell
$env:COLAB_CHAT_URL='https://SEU-ENDERECO-DO-TUNEL/explain'
$chave = Read-Host 'Mesma COLAB_CHAT_KEY definida no Colab' -AsSecureString
$env:COLAB_CHAT_KEY=[System.Net.NetworkCredential]::new('', $chave).Password
.\1-INICIAR-API.cmd
```

5. No site, faça uma análise e pergunte sobre ela no assistente. Quando a integração responder corretamente, a mensagem terá modo **COLAB**. Se falhar, o sistema preserva a explicação local.
6. Quando terminar, encerre o túnel e o runtime do notebook. Se a URL do túnel mudar, atualize a variável e reinicie a API.

O Colab é ambiente de estudo interativo e tem limites de execução; não deve ser tratado como hospedagem permanente. Consulte as [restrições e limites oficiais](https://research.google.com/colaboratory/faq.html). Para serviço contínuo, hospede o Python em ambiente próprio. Groq é opcional e suas condições/cotas dependem da conta.

## 8. Usar o Telegram

1. Crie seu bot no **BotFather**, pelo comando `/newbot`, conforme a [documentação oficial](https://core.telegram.org/bots/features#botfather). Guarde o token sem compartilhá-lo.
2. Siga **TELEGRAM.md** para descobrir seu ID numérico, gerar a chave de integração e configurar as variáveis. Há comandos prontos com entrada de segredos sem exibi-los.
3. Deixe a API ligada com `TELEGRAM_API_KEY`. Em outro terminal, configure o mesmo valor, seu `TELEGRAM_BOT_TOKEN` e a lista `TELEGRAM_ALLOWED_USER_IDS`.
4. Na raiz do projeto, execute `python notebooks/sentinela_telegram.py`.
5. No Telegram, abra conversa privada com seu bot e envie `/start`, depois `/analisar https://example.org`. Em seguida, pergunte `Por que recebeu essa pontuação?`.
6. `/privacidade` explica os dados. `/apagar CONFIRMAR` remove suas análises e chat na API; as mensagens do Telegram são removidas no próprio aplicativo.

Comece pelo bot no computador: ele acessa a API local diretamente. O notebook **Sentinela_Telegram_Colab.ipynb** é uma alternativa; nesse caso, envie também `sentinela_telegram.py` e configure uma API Java HTTPS acessível externamente. O `localhost` do Colab não é o seu computador. Nunca rode as duas instâncias com o mesmo token ao mesmo tempo.

## Ordem recomendada para aprender e apresentar

**Site + 2FA → análise + histórico → assistente local → app no emulador → notebook Colab → integração Colab → Telegram.** Cada passo acrescenta uma parte; você consegue testar o núcleo antes de configurar contas e chaves externas.

Se o cadastro falhar, confira se `http://127.0.0.1:8080/api/v1/health` mostra UP e se o terminal Java apresenta erro. Não apague banco ou chave para tentar resolver uma mensagem genérica. Em uma investigação anterior, um cadastro sintético pelo localhost retornou 201; isso não identificou a causa do erro da sua captura.

## 9. Habilitar as duas fontes reais de reputação

Sem essas chaves, as regras locais funcionam, mas o resultado informa fontes não configuradas. Para habilitar as consultas reais, faça as duas configurações abaixo. Ter chave não garante uma resposta: conectividade, permissão e cota também precisam estar válidas.

**Google Safe Browsing**

1. Entre no Google Cloud Console com sua conta Google. No seletor de projeto, crie ou selecione um projeto para o TCC.
2. Abra **APIs e serviços → Biblioteca**, procure **Safe Browsing API** e habilite-a.
3. Em **APIs e serviços → Credenciais**, crie uma **chave de API**. Restrinja a chave à API necessária; a chamada parte do Java, portanto não use restrição de referenciador de navegador para essa chave de servidor.
4. Guarde a chave para a variável `GOOGLE_SAFE_BROWSING_KEY`. Essa não é a chave Groq. Confira termos, cota e disponibilidade na [configuração oficial da API v4 usada pelo projeto](https://developers.google.com/safe-browsing/v4/get-started).

**URLhaus**

1. Abra a [página oficial da API URLhaus](https://urlhaus.abuse.ch/api/).
2. Clique **Obtain an Auth-Key** e acesse o portal de autenticação abuse.ch indicado pela página.
3. Entre/crie sua conta conforme o portal, gere seu **Auth-Key** e guarde-o para `URLHAUS_KEY`. Não use sua senha do portal nesse campo. A documentação detalhada pode exigir login.

**Colocar as chaves na API**

Encerre a janela Java anterior com Ctrl+C. Abra PowerShell na raiz do projeto e execute:

```powershell
$valor = Read-Host 'Chave Google Safe Browsing' -AsSecureString
$env:GOOGLE_SAFE_BROWSING_KEY=[System.Net.NetworkCredential]::new('', $valor).Password
$valor = Read-Host 'Auth-Key URLhaus' -AsSecureString
$env:URLHAUS_KEY=[System.Net.NetworkCredential]::new('', $valor).Password
.\1-INICIAR-API.cmd
```

No site, faça **uma nova análise** de `https://example.org`. Nos detalhes, as fontes devem ter resposta consultada, por exemplo **Sem correspondência**, em vez de **Não configurada**. **Indisponível** significa que a consulta não completou com sucesso; confira habilitação, cota, rede e chave. O histórico antigo é um retrato da consulta anterior e não se atualiza sozinho. O servidor pode manter cache por cinco minutos; reiniciar a API limpa esse cache. Não precisa acessar URL maliciosa para testar o funcionamento da integração.

## 10. Criar as chaves Groq e ngrok para o Colab

1. Abra [Groq Console](https://console.groq.com/keys), entre com sua conta e abra a área **API keys / Get API key**. Crie uma chave em um projeto elegível, conforme a [documentação oficial](https://console.groq.com/docs/quickstart). Contas podem exigir habilitação, consentimentos ou condições próprias. Não cole a chave no código.
2. No Colab, abra o notebook **Sentinela_Chatbot_Colab.ipynb** e clique no ícone de chave **Secrets** da barra lateral.
3. Adicione `GROQ_API_KEY` com a chave e `GROQ_MODEL` com o ID exato de um modelo de chat disponível no Playground da sua conta, compatível com JSON Object Mode. Autorize o acesso do notebook aos dois Secrets.
4. Gere uma chave compartilhada exclusiva no terminal local com `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"`. Guarde o resultado e crie no Colab o Secret `COLAB_CHAT_KEY` com esse valor. Você informará o mesmo valor ao Java. Não use a chave Groq como chave compartilhada.
5. Entre/crie sua conta no [ngrok](https://dashboard.ngrok.com/), localize seu **authtoken** no painel e copie-o para um Secret chamado `NGROK_AUTHTOKEN`. Esse é o token da conta ngrok; não é o endereço do túnel. Veja a [documentação oficial](https://ngrok.com/docs/start).
6. Execute no notebook, na ordem, instalação, funções, demonstração, carregamento de Secrets, servidor e túnel. A célula do túnel mostrará uma URL HTTPS terminando em `/explain`.
7. Volte à seção 7 e informe a URL e a chave compartilhada ao Java. Se Groq estiver configurado mas falhar, o notebook pode devolver uma explicação local; o rótulo COLAB confirma a passagem pelo notebook, não prova sozinho que o Groq gerou o texto.

## 11. Integrar tudo sem perder as variáveis ao reiniciar a API

Use **um PowerShell para a API**, outro para o web e outro para o bot. Quando configurar mais uma integração, interrompa apenas a API e use o mesmo PowerShell para acrescentar as variáveis antes de iniciá-la novamente. As variáveis já preenchidas continuam nessa sessão do PowerShell. Ao fechar a janela, será necessário informá-las de novo.

| Nome | Onde preencher | Origem do valor |
|---|---|---|
| `ENCRYPTION_KEY` | Gerada automaticamente pelo CMD; preservada em `.runtime/encryption.key` | Chave local de criptografia do 2FA |
| `GOOGLE_SAFE_BROWSING_KEY` | PowerShell da API | Google Cloud / Safe Browsing |
| `URLHAUS_KEY` | PowerShell da API | Portal abuse.ch / Auth-Key |
| `COLAB_CHAT_URL` | PowerShell da API | URL HTTPS do túnel + `/explain` |
| `COLAB_CHAT_KEY` | Secret no Colab e PowerShell da API, iguais | Gerada por você, mínimo 32 caracteres aleatórios |
| `GROQ_API_KEY` | Secret no Colab do explicador | Groq Console |
| `GROQ_MODEL` | Secret no Colab do explicador | ID exato do modelo de chat disponível na conta |
| `NGROK_AUTHTOKEN` | Secret no Colab do explicador | Painel ngrok |
| `TELEGRAM_API_KEY` | PowerShell da API e PowerShell do bot, iguais | Outra chave exclusiva, gerada por você |
| `TELEGRAM_BOT_TOKEN` | PowerShell do bot | BotFather |
| `TELEGRAM_ALLOWED_USER_IDS` | PowerShell do bot | ID numérico do seu usuário, separado por vírgula para vários |
| `SENTINELA_API_BASE` | PowerShell do bot | `http://127.0.0.1:8080/api/v1` no bot local |

Não coloque esses valores em `apps/web`: o navegador não precisa conhecer as chaves de servidor. O arquivo `.env.example` é um modelo para Docker, não um formulário que o CMD lê automaticamente. Apenas preencher `.env` não configura a API Java iniciada diretamente. Os comandos deste guia configuram o ambiente correto.

Para mostrar apenas o que está definido, sem mostrar valores, rode antes de iniciar a API:

```powershell
'GOOGLE_SAFE_BROWSING_KEY','URLHAUS_KEY','COLAB_CHAT_URL','COLAB_CHAT_KEY','TELEGRAM_API_KEY' | ForEach-Object {
  [pscustomobject]@{Nome=$_; Definida=[bool][Environment]::GetEnvironmentVariable($_)}
}
```

## 12. Entrar como administrador

É opcional para analisar links, mas necessário para demonstrar o menu administrativo.

1. Escolha um email administrativo **que ainda não tenha sido cadastrado nesse banco**. O inicializador não promove conta existente.
2. Encerre a API. No PowerShell da API, preencha:

```powershell
$env:ADMIN_EMAIL=Read-Host 'Email da nova conta administrativa'
$valor=Read-Host 'Senha administrativa nova, mínimo 16 caracteres' -AsSecureString
$env:ADMIN_PASSWORD=[System.Net.NetworkCredential]::new('', $valor).Password
.\1-INICIAR-API.cmd
```

3. No site, escolha **Entrar** com esse email e senha, configure o 2FA e confira o menu **Administração**. Não use Criar conta para criar o administrador.
4. Após o provisionamento, remova `ADMIN_PASSWORD` e `ADMIN_EMAIL` das configurações de inicialização futura. No mesmo PowerShell, após parar a API, use `Remove-Item Env:ADMIN_PASSWORD,Env:ADMIN_EMAIL` e inicie novamente. A conta continua no banco.

## 13. Colocar a logo do bot no Telegram

A logo já está nos locais de marca do site, Android e notebooks. A foto do bot é configuração da sua conta Telegram:

1. Abra o BotFather oficial.
2. Envie `/setuserpic` e selecione seu bot.
3. Envie o PNG `apps/web/public/sentinela-logo.png` (também entregue como `Sentinela-logo.png`).
4. Abra a conversa com seu bot e confira a foto. Não é preciso compartilhar o token para definir a foto pelo BotFather.

## 14. Conferência final e uso em cada dia

| Conferência | Como confirmar |
|---|---|
| API ligada | Abra `http://127.0.0.1:8080/api/v1/health`: deve mostrar UP. |
| Comunicação web/API | Abra `http://localhost:3000/api/v1/health`: também deve mostrar UP. |
| Conta e 2FA | Entre, confirme TOTP e abra Analisar link. |
| Motor e persistência | Faça uma análise, recarregue a página e encontre-a no Histórico. |
| Reputação real | Faça análise nova e veja fontes consultadas, sem Não configurada/Indisponível. |
| Android | Run no emulador e encontre a análise da mesma conta. |
| Colab | Pergunta contextual retorna modo COLAB; observe os limites do fallback explicados acima. |
| Telegram | Seu usuário autorizado recebe análise e resposta contextual em conversa privada. |
| Administração | Conta ADMIN acessa usuários e auditoria. |

Em cada dia: abra a pasta correta, configure as variáveis necessárias, ligue a API, ligue o web, abra o emulador se for usar Android, reconecte o Colab e atualize URL do túnel se mudou, inicie **uma única instância** do bot se for usar Telegram. Para parar, Ctrl+C nos terminais, interrompa as células e finalize o túnel/runtime.

Os arquivos CMD ligam a demonstração sintética explicitamente; isso não vira inteligência de ameaça real por adicionar chaves. Para usar sem a base de demonstração, inicie o JAR manualmente com `DEMO_MODE=false` e as demais variáveis configuradas, preservando a chave original de criptografia. A aplicação não analisa conteúdo completo de páginas, não bloqueia a navegação do navegador e não garante ausência de golpe. A configuração de todas as integrações não elimina esses limites.

Não é possível confirmar todas as integrações reais sem suas contas, permissões e chaves. As rotinas locais foram testadas conforme VERIFICACAO.md; use a tabela acima para validar seu ambiente após configurá-lo. Se um passo falhar, copie a mensagem de erro correspondente sem incluir senhas, tokens ou códigos 2FA.


# Guia completo do novo painel de monitoramento


Esta versão acrescenta **Meus sites** como entrada da conta do cliente e mantém **Analisar link** para avaliações pontuais. A administração tem entrada própria. O usuário comum pode configurar/desativar 2FA em Minha segurança; ADMIN precisa ativá-lo. `REQUIRE_MFA=true` permite tornar o fator obrigatório também para clientes, se desejado.

## 1. Cadastrar e abrir o painel

1. Inicie API e web pelos CMDs, entre com sua conta comum e abra **Meus sites**.
2. Clique **Adicionar site**. Informe um nome e a URL do seu site, como `https://seudominio.com.br`.
3. Clique **Salvar site**. O painel já abre; sem medições ele mostra **aguardando**, sem números inventados.
4. O monitor usa somente a origem: caminhos, query e fragmento são descartados. Aceita HTTP/HTTPS nas portas padrão. Domínios internos, IPs e `.test` não são destinos válidos para o monitor de produção.

## 2. Confirmar propriedade por DNS

1. Entre no painel do provedor que administra o DNS desse domínio.
2. Procure a área **DNS / Gerenciar zona / Registros** e clique em adicionar registro.
3. Escolha **TXT**. Copie os campos **Nome** e **Valor** exibidos no painel do Sentinela.
4. O nome completo é `_sentinela.seudominio.com.br`; se o provedor já acrescenta o domínio, informe somente `_sentinela`. O valor começa por `sentinela=` e contém o token exclusivo exibido.
5. Salve e aguarde a propagação. Volte ao Sentinela e clique **Já adicionei: confirmar domínio**.
6. Após a confirmação, clique **Verificar agora**, ou aguarde a coleta automática. O painel atualiza a cada 30 segundos; coletas recorrentes são programadas aproximadamente a cada cinco minutos, sujeitas à fila. A API deve permanecer ligada.

Mantenha o TXT no DNS: antes de cada consulta externa o servidor confere novamente a propriedade. Registro ausente ou incorreto suspende a confirmação; uma falha temporária de resolução apenas adia a coleta e tenta novamente. Não é necessário fornecer senha do provedor ao Sentinela. O TXT não é uma chave de acesso à API; pode ser público.

Sem domínio próprio ou permissão para alterar o DNS, você pode usar o analisador pontual de links, mas não deve afirmar que configurou o monitor de produção. Os testes automatizados usam banco e respostas controlados; não substituem essa confirmação real.

## 3. Ler o dashboard

| Informação | Origem e significado |
|---|---|
| Disponibilidade nas amostras | Percentual de verificações HTTP com resposta 2xx/3xx no período. Não é medição contínua nem prova de proteção. |
| Tempo da verificação | Tempo observado desde DNS/conexão/TLS até os cabeçalhos de resposta. Não é tempo isolado de processamento da aplicação. |
| Certificado HTTPS | Vencimento obtido em conexão validada; em HTTP ou falha TLS pode não haver medição. |
| Achados de configuração | Presença/ausência de cabeçalhos na resposta HEAD da raiz. Ausência não comprova vulnerabilidade explorável. |
| Eventos do conector | Relatos enviados pelo backend/WAF do cliente, com chave do site. Não são ataques confirmados pelo monitor externo. |
| Eventos simulados | Dados de teste explicitamente marcados; contados separadamente dos relatos reais. |

**Respondendo**: HTTP 2xx. **Redirecionamento**: HTTP 3xx observado, sem visitar o destino. **Acesso restrito**: HTTP 4xx, que pode refletir política do servidor. **Erro no servidor**: HTTP 5xx. **Não alcançado/Falha TLS**: a consulta não completou. **DNS indisponível**: não foi possível confirmar a propriedade naquele momento; não houve acesso ao destino. Queda e erro não são classificados automaticamente como ataque.

Os gráficos mostram até as 2.000 últimas amostras do período; o indicador de disponibilidade usa todas as amostras elegíveis do período. Falhas de consulta de propriedade não contam como indisponibilidade do site. Os últimos 100 eventos aparecem na lista. A atualização periódica não depende de a página estar aberta, mas depende da API estar ligada.

O assistente dentro do painel resume dados do domínio atual com uma explicação determinística; não inventa invasões. O assistente de análise pontual mantém a integração Colab/Groq anterior. O novo monitor não transforma o Colab em detector de ataques.

## 4. Trocar domínio e pausar

1. No painel do site, clique **Editar site**.
2. Alterar só o nome mantém a confirmação. Alterar protocolo/domínio cria uma nova revisão, exige novo TXT e revoga a chave de eventos anterior.
3. As amostras e eventos antigos continuam no banco associados à revisão anterior; não entram no gráfico do novo endereço. A interface mostra a revisão atual; não há navegador de revisões antigas nesta entrega.
4. Para interromper consultas sem apagar dados, use **Pausar**. **Retomar** volta a permitir a coleta após a confirmação.

## 5. Receber eventos reais da aplicação ou WAF

Esse modo exige acesso ao backend do site do cliente. Uma URL pública, sozinha, não permite observar tentativas de login ou logs privados.

1. Confirme o domínio por TXT.
2. No painel, encontre **Integração do seu site → Gerar chave de integração**.
3. Confirme a senha e, se ativo, o TOTP/código de recuperação. Um TOTP já utilizado não pode ser repetido; aguarde o próximo código.
4. Copie a chave mostrada uma vez. Guarde-a no ambiente privado do backend do cliente. Nunca no JavaScript público, no aplicativo Android distribuído ou em repositório.
5. O endereço de destino é `https://SEU-SENTINELA/api/v1/ingest/events`. Para teste no mesmo computador, use `http://127.0.0.1:8080/api/v1/ingest/events`.
6. Instale/adapte um dos conectores em `connectors/`. O conector deve enviar eventos nos pontos onde a aplicação realmente identificou cada ocorrência.

Tipos permitidos: `AUTH_FAILURE`, `RATE_LIMIT`, `WAF_BLOCK`, `INTEGRITY_ALERT`, `APPLICATION_ERROR`. O corpo contém ID UUID, tipo, horário em milissegundos e `simulated` explícito. Eventos fora da janela de cinco minutos são recusados. A chave fica no cabeçalho `X-Sentinela-Site-Key`. O servidor aplica limites e ignora duplicata de ID já aceita. Não envie senhas, códigos 2FA, tokens humanos, corpos de requisições ou dados pessoais no evento.

### Testar a integração sem inventar um ataque

Instale Python real (o atalho da Microsoft Store, sozinho, não é o interpretador). No PowerShell da raiz do projeto:

```powershell
$segredo=Read-Host 'Chave exclusiva do site mostrada no painel' -AsSecureString
$env:SENTINELA_SITE_KEY=[System.Net.NetworkCredential]::new('', $segredo).Password
$env:SENTINELA_EVENT_URL='http://127.0.0.1:8080/api/v1/ingest/events'
python connectors/site_event_client.py --simulate --type AUTH_FAILURE
```

O evento aparecerá como **Simulação explícita**. A ferramenta de terminal só envia simulações. Não apresenta esse teste como um ataque real. Domínio confirmado, conta/site ativos e chave atual são necessários até para a simulação.

### Integrar ao backend Java do cliente

Copie `connectors/SentinelaReporter.java` para o backend, ajuste o pacote e inicialize uma única instância usando variáveis de ambiente:

```java
var reporter = new SentinelaReporter(
    System.getenv("SENTINELA_EVENT_URL"),
    System.getenv("SENTINELA_SITE_KEY"));
// No ponto onde a sua aplicação RECUSOU uma autenticação real:
reporter.report("AUTH_FAILURE", false);
// Somente em uma demonstração identificada:
reporter.report("AUTH_FAILURE", true);
```

A chamada é assíncrona e retorna um CompletableFuture<Boolean>; uma falha do Sentinela não deve impedir o fluxo de negócio do cliente. O conector de exemplo não substitui um sistema de filas com retries duráveis. Adapte o emissor às regras reais da sua aplicação. Não coloque uma chamada falsa em cada acesso apenas para preencher gráficos.

Para Python, importe `SiteReporter` de `connectors/site_event_client.py` e chame `report('AUTH_FAILURE', simulated=False)` no backend. A identidade vem da chave do site, não de um siteId escolhido pelo emissor. Rotacionar a chave no painel revoga imediatamente a anterior. Ao trocar o domínio, gere outra chave e atualize o backend.

## 6. Administrar usuários e assinaturas

1. Entre no site com a conta ADMIN provisionada pelo procedimento de README/GUIA-DO-USUARIO e configure seu 2FA obrigatório.
2. A entrada é **Central administrativa**, separada de Meus sites.
3. Em **Assinaturas e acesso**, escolha Ativo, Avaliação, Inadimplente ou Suspenso e informe o vencimento se necessário.
4. Clique **Salvar**. Inadimplente/Suspenso desabilita acesso, encerra sessões e impede novas verificações/eventos. Histórico é preservado. Consultas que já estavam em andamento podem concluir a conexão; o resultado é descartado se a situação mudou antes de persistir.
5. Escolher Ativo/Avaliação reabilita acesso. O vencimento é informativo e não muda a situação automaticamente. Não existe gateway, emissão de cobrança ou confirmação automática de pagamento nesta versão.
6. A seção de sites permite conferir cliente, origem e confirmação; a auditoria registra as mudanças administrativas.

## 7. Android, execução e atualização

No Android, entre na conta e escolha **Meus sites · monitoramento** na tela inicial. Há cadastro, edição, confirmação DNS, pausa, métricas, histórico recente em gráfico e assistente do site. A geração de chave e administração de assinaturas são feitas na área web. O app não precisa guardar uma chave de integração de backend.

Execute API e web pelos CMDs. Flyway aplica a migração V2 automaticamente sem apagar análises anteriores. Preserve `.runtime/encryption.key` e o banco. Faça backup antes de atualizar uma instalação. `MONITORING_ENABLED=false` desativa coleta no ambiente de testes; o padrão é true. A coleta usa duas threads e fila limitada. Não há fila distribuída ou garantia de intervalo exato em grande volume.

## Segurança e limites

DNS é validado antes da conexão e o endereço IPv4 público é fixado ao socket. Endereços privados/reservados e portas arbitrárias são recusados; não há navegação de redirects, scripts, leitura do corpo, scanner de portas ou exploração. HTTPS valida certificado e nome do host. Produção também requer regras de saída de rede, HTTPS da própria plataforma, gestão de segredos, retenção e capacidade operacional.

O cliente com chave pode enviar relatos falsos; eles são identificados como reportados pelo conector, não como fatos comprovados independentemente. Nunca diga “100% seguro” a partir desses indicadores. Referências: [OWASP sobre SSRF](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html), [Java SSLParameters](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/net/ssl/SSLParameters.html).


## Atualização Groq

O provedor correto é Groq (console.groq.com). O notebook atualizado usa GROQ_API_KEY e GROQ_MODEL; não usa Gemini nem Grok/xAI. Após configurar os Secrets, a célula de diagnóstico mostra GROQ/OK quando recebeu resposta validada. LOCAL indica explicação de reserva; a situação ajuda a identificar chave/modelo ausentes ou erro de consulta. O modo COLAB no Java indica passagem pelo notebook. Veja GROQ-COLAB.md para o roteiro atual.
