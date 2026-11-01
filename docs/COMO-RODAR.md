# Sentinela — passo a passo no Windows

**Atualização:** para cadastrar seu próprio domínio e acompanhar o dashboard, siga [MONITORAMENTO.md](MONITORAMENTO.md). A análise pontual de links continua disponível. Cliente comum pode optar por 2FA; ADMIN sempre exige o fator.

## 1. Preparar

1. Extraia `Sentinela-projeto-completo.zip` para uma pasta sua, por exemplo `C:\Projetos\sentinela`. Não execute dentro do ZIP.
2. Instale **Java JDK 21**, **Node.js 24 LTS** e, para o bot local, **Python 3.11 ou superior**. Para editar o app, instale Android Studio e Android SDK 35 pelo SDK Manager.
3. Abra um terminal e confira `java -version`, `node --version`, `npm --version` e `python --version`. Se algum não existir, ajuste o PATH/reabra o terminal. Para compilar a API, `JAVA_HOME` deve apontar à pasta do JDK 21.

## 2. Rodar API e site

1. Na pasta extraída, abra `1-INICIAR-API.cmd`. Mantenha a janela aberta. Deve aparecer que o Spring iniciou na porta 8080.
2. Abra `2-INICIAR-WEB.cmd` e mantenha a segunda janela aberta. Na primeira execução, `npm ci` e o build precisam de internet e podem demorar.
3. Acesse **http://localhost:3000**. Use sempre `localhost`, inclusive durante o login.
4. Crie uma conta com senha de pelo menos 12 caracteres. O cliente entra em **Meus sites** e pode configurar o segundo fator em **Minha segurança**; ADMIN precisa configurá-lo para continuar.
5. Siga a seção abaixo para ativar o autenticador. Depois use **Analisar link**.
6. Teste `https://example.org` (exemplo neutro) e `https://banc0-seguranca-login.test` (exemplo sintético identificado; não é prova de ameaça real).
7. Para encerrar, pressione Ctrl+C em cada janela e confirme o encerramento quando o CMD perguntar.

O inicializador cria uma chave aleatória em `.runtime/encryption.key`; preserve esse arquivo com o banco `services/api/data`. A chave protege os segredos de 2FA. Não compartilhe esses arquivos nem troque a chave sem um procedimento de migração: perder a chave impede verificar o TOTP existente. O banco local H2 dispensa PostgreSQL na demonstração. As análises sobrevivem à reinicialização.

## 3. Ativar e usar 2FA

1. Instale um autenticador compatível com TOTP em seu celular, por exemplo Google Authenticator ou Microsoft Authenticator.
2. No Sentinela, abra **Minha segurança**, confirme a senha e escolha **Configurar segundo fator**.
3. No site, o QR code aparece em **Minha segurança** após confirmar sua senha. No Google Authenticator, toque **+ → Ler QR code** e escaneie. Isso adiciona sua conta ao aplicativo. Como alternativa, use a chave manual (TOTP, seis dígitos, 30 segundos). No Android nativo, a configuração manual continua disponível.
4. Digite no Sentinela a senha e o código atual. Clique **Confirmar e ativar**.
5. Guarde os **oito códigos de recuperação** fora do dispositivo, em local protegido. São exibidos uma vez e cada um só funciona uma vez. Não os coloque no Telegram.
6. Na próxima entrada, use email, senha e o código atual do autenticador. Se acabou de usar um código, aguarde o próximo: códigos usados não são aceitos novamente. Um código de recuperação pode substituir o TOTP no login.

Na política atual, o cliente pode usar Meus sites e ativar seu 2FA em Minha segurança. Administradores sempre precisam de 2FA; REQUIRE_MFA=true também exige o fator de clientes. Contas técnicas do bot não recebem login humano; são protegidas por chave de integração separada e lista de IDs permitidos. O login do Telegram não substitui o 2FA do Sentinela.

Se perder autenticador e todos os códigos, não existe recuperação automática por email nesta versão. Não há senha mestra ou bypass. Para apresentação, mantenha uma conta de demonstração cujos códigos você guardou. Ative data/hora automáticas no celular para evitar TOTP incorreto.

## 4. Android Studio

O arquivo `Sentinela-Android-Studio.zip` contém o **código-fonte Android**, não somente um APK.

1. Extraia esse ZIP. No Android Studio, escolha **Open** e selecione a pasta `android`, que contém `settings.gradle`, `build.gradle` e `gradlew.bat`.
2. Aguarde o Gradle Sync. Instale Android SDK Platform 35 e Build Tools solicitados pelo IDE. Se houver conflito de JDK, selecione o JDK integrado compatível com Gradle/AGP (17 ou 21).
3. No Device Manager, crie e inicie um emulador de celular API 35 (mínimo suportado API 26).
4. Inicie a API no computador pelo passo 2. O app debug usa `http://10.0.2.2:8080/api/v1`: esse IP representa o computador dentro do emulador. Não use esse IP no navegador do computador.
5. Selecione o módulo **app** e pressione **Run ▶**. Entre na mesma conta do site; o histórico é compartilhado pela API.
6. Para celular físico via USB, habilite depuração USB, autorize o computador e execute `adb reverse tcp:8080 tcp:8080`. No terminal da pasta Android, gere o app com `gradlew.bat assembleDebug -PapiBase=http://127.0.0.1:8080/api/v1` e instale o APK produzido. ADB está em `platform-tools` do SDK. Repita o reverse ao reconectar. O APK padrão do pacote usa o emulador, não essa configuração USB.
7. Para distribuição real, configure uma API pública **HTTPS** e compile com `gradlew.bat assembleRelease -PapiBase=https://api.seu-dominio.example/api/v1`. Configure assinatura de release no Android Studio; não compartilhe keystore/senhas. O release bloqueia HTTP.

O APK `dist/Sentinela-debug.apk` serve para avaliação e instalação em emulador. O arquivo Android Studio permite editar todo o projeto Java nativo. Capturas de tela são bloqueadas pelo app para proteger códigos/chaves exibidos. O app foi compilado e analisado por lint; ainda precisa ser exercitado em aparelho/emulador real.

## 5. Chatbot no Colab e Telegram

Para explicar resultados pela IA do Colab, abra `notebooks/Sentinela_Chatbot_Colab.ipynb` no Google Colab, execute as células em ordem (o notebook do explicador já contém seu código). A chave Groq e o túnel são opcionais; sem eles há explicação local.

Para o bot no Telegram, siga **[TELEGRAM.md](TELEGRAM.md)**. Há também um notebook separado, `Sentinela_Telegram_Colab.ipynb`, para executar o polling no Colab. O Telegram não substitui o motor Java: ele envia o link para a mesma API, que calcula o score e, quando configurado, pede explicação ao Colab.

## 6. Executar pelo terminal ou recompilar

Na raiz, para gerar uma chave (copie o resultado para variável de ambiente no seu terminal, nunca para código):

```powershell
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
$env:ENCRYPTION_KEY='COLE_A_CHAVE_GERADA_LOCALMENTE'
cd services/api
.\mvnw.cmd test package
java -jar target/sentinela-api-1.0.0.jar --server.address=127.0.0.1
```

Confirme o nome efetivo do JAR em `target`; o binário já entregue usa `java -jar dist/sentinela-api.jar` a partir da raiz. Os arquivos CMD usam o binário em `dist`, portanto copie o novo JAR para lá após recompilar.

Em outro terminal:

```powershell
cd apps/web
npm ci
npm test
npm run dev
```

Para build de produção, use `npm run build` seguido de `npm start`. Os scripts CMD são o caminho mais simples. O arquivo `.env.example` é modelo para Docker Compose; **o Java iniciado diretamente não carrega `.env` automaticamente**. Configure suas variáveis no terminal antes de executar o CMD.

## 7. Problemas comuns

| Sintoma | O que conferir |
|---|---|
| Porta 8080/3000 ocupada | Encerre a instância anterior com Ctrl+C e inicie novamente. |
| Site abre, mas login/análise falham | Confira se API iniciou e se `http://127.0.0.1:8080/api/v1/health` retorna UP. |
| Código 2FA recusado | Hora automática no celular, chave correta e código ainda não utilizado. Aguarde o próximo código. |
| Exige 2FA antes de analisar | Conclua a configuração em Minha segurança. Essa conta ou ambiente exige o fator; configure-o em Minha segurança. |
| Chave de criptografia indisponível | Inicie pelo CMD ou configure `ENCRYPTION_KEY`; não apague a chave usada pelo banco. |
| Android sem conexão | API ligada; emulador usa 10.0.2.2, aparelho USB usa adb reverse e build específico. |
| Fontes não configuradas | Configure chaves de Safe Browsing/URLhaus e reinicie API; resultado parcial é mostrado com clareza. |
| Colab parou | Sessões Colab expiram. Reinicie notebook/túnel e atualize URL na API; explicação local continua disponível. |

Hospedagem exige HTTPS, `SECURE_COOKIES=true`, `WEB_ORIGIN` exata e segredo estável. Os CMDs expõem apenas loopback para uso local. Leia o relatório de segurança antes de publicar a API. Docker está preparado em `compose.yaml`, mas não foi executado neste ambiente.
