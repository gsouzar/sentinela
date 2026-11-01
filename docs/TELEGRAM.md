# Sentinela no Telegram

O bot recebe links, envia à API Java e responde com score, cobertura e indicadores. Perguntas posteriores usam a análise da mesma conversa. Se `COLAB_CHAT_URL` estiver configurado na API, o assistente usa o Colab; caso contrário, a explicação é local. A conta técnica Telegram é independente da conta humana do site/app e não conhece senha ou 2FA dela.

## 1. Criar o bot e obter seu ID

1. No Telegram, abra o **[@BotFather oficial](https://t.me/BotFather)**, envie `/newbot` e siga as instruções para nome e username. Guarde o token em local privado.
2. Pelo BotFather, desative participação em grupos (`/setjoingroups`) e mantenha privacidade de grupos ativada. O código também recusa grupos.
3. Envie `/start` **do seu usuário** ao bot recém-criado.
4. Antes de iniciar o programa, descubra o ID dessa mensagem usando a Bot API. No PowerShell, use o trecho abaixo; ele imprime apenas o ID numérico, nunca o token:

```powershell
$segredo = Read-Host 'Token recebido do BotFather' -AsSecureString
$env:TELEGRAM_BOT_TOKEN = [System.Net.NetworkCredential]::new('', $segredo).Password
$resposta = Invoke-RestMethod -Method Post -Uri ('https://api.telegram.org/bot' + $env:TELEGRAM_BOT_TOKEN + '/getUpdates') -ContentType 'application/json' -Body '{"timeout":0,"allowed_updates":["message"]}'
$resposta.result | ForEach-Object { $_.message.from.id } | Select-Object -Unique
```

Use apenas um token seu. Se não houver resultado, envie uma nova mensagem ao bot e repita. Não execute outro polling simultaneamente. Em caso de erro, não compartilhe a saída completa do PowerShell: o URL do Telegram inclui o token. Revogue pelo BotFather qualquer token exposto. Para um bot que já usa webhook, remova-o conscientemente antes de mudar para polling; o programa não apaga configuração existente automaticamente.

## 2. Preparar chave da integração e iniciar API

No terminal que iniciará a API:

```powershell
cd C:\Projetos\sentinela
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
# Guarde o valor gerado em gerenciador de segredos e informe-o abaixo.
$chave = Read-Host 'Chave de integração que você acabou de gerar' -AsSecureString
$env:TELEGRAM_API_KEY = [System.Net.NetworkCredential]::new('', $chave).Password
.\1-INICIAR-API.cmd
```

Se já havia uma API ligada, encerre-a e reinicie com essa variável. Sem `TELEGRAM_API_KEY`, a integração retorna indisponível. A chave deve ser exclusiva do bot, diferente de `ENCRYPTION_KEY` e `COLAB_CHAT_KEY`. Preserve a chave: ela também deriva a identidade pseudônima das conversas. Trocar a chave muda essas identidades e exige planejar remoção dos dados antigos.

## 3. Rodar o bot no computador

Em **outro PowerShell**, na raiz extraída:

```powershell
$token = Read-Host 'Token BotFather' -AsSecureString
$env:TELEGRAM_BOT_TOKEN = [System.Net.NetworkCredential]::new('', $token).Password
$chave = Read-Host 'Mesma TELEGRAM_API_KEY da API' -AsSecureString
$env:TELEGRAM_API_KEY = [System.Net.NetworkCredential]::new('', $chave).Password
$env:TELEGRAM_ALLOWED_USER_IDS = 'SEU_ID_NUMERICO'
$env:SENTINELA_API_BASE = 'http://127.0.0.1:8080/api/v1'
python notebooks/sentinela_telegram.py
```

O script usa somente biblioteca padrão Python. Separe vários IDs autorizados por vírgula (`12345,67890`). Uma lista vazia impede iniciar. IDs são numéricos, não `@username`. Mantenha uma única instância do bot e a API ligadas. Ctrl+C encerra o bot. As variáveis acima não são carregadas de `.env` pelo Python automaticamente.

A mesma chave precisa ser informada nos dois terminais. O primeiro comando de geração a mostra localmente: execute-o fora de gravações/projeções e depois limpe a tela. Não cole token/chaves em código-fonte, screenshots, commits ou mensagens.

## 4. Testar no Telegram

1. Envie `/start`.
2. Envie `/analisar https://example.org`.
3. Pergunte `Por que essa pontuação?`.
4. Envie `/privacidade` para entender onde os dados ficam.
5. `/apagar` apenas explica a confirmação. `/apagar CONFIRMAR` remove análises e chat dessa identidade na API. Mensagens no Telegram e auditoria técnica devem ser tratadas separadamente.

O bot só responde aos IDs autorizados em conversa privada. Não ativa login humano nem lê dados de contas do web/mobile. Mensagens do bot usam texto simples e previews de links desativados. A pontuação continua sendo do Java; Groq não determina risco.

## 5. Rodar o bot pelo Google Colab

Abra `notebooks/Sentinela_Telegram_Colab.ipynb`. Envie `sentinela_telegram.py` e cadastre no painel **Secrets**: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_API_KEY`, `TELEGRAM_ALLOWED_USER_IDS` e `SENTINELA_API_BASE`.

**O Colab não acessa a API pelo `localhost` do seu computador.** Para esse cenário, a API precisa de um endereço HTTPS acessível pelo Colab. Use hospedagem ou um túnel temporário autenticado pela chave da integração. Se usar um túnel, sua URL deve terminar em `/api/v1` e o tráfego público exige os cuidados de segurança do projeto. Para demonstração simples, execute o bot localmente e mantenha apenas o explicador no Colab.

Execute a última célula enquanto quiser manter o bot ligado; o runtime pode expirar e não é hospedagem permanente. Não execute simultaneamente esse notebook e o bot local com o mesmo token. Interrompa a célula para parar.

## Limites e fontes

Testes automatizados usam transportes simulados; não foi usado token real nem enviada mensagem por esta entrega. A lista de IDs limita a demonstração. Não há vinculação automática entre identidade Telegram e conta do site, nem painel para recuperar acesso ao bot. Perguntas precisam de uma análise anterior; o contexto ativo em memória se perde ao reiniciar, embora as análises continuem na API até exclusão. Mensagens passam pela infraestrutura Telegram. O código não registra token, URL de requisição nem conteúdo de mensagem em seus logs.

Referências consultadas: [BotFather e recursos oficiais](https://core.telegram.org/bots/features#botfather), [getUpdates e Bot API](https://core.telegram.org/bots/api#getupdates). O polling evita precisar publicar um webhook para o Telegram; a API Java ainda precisa estar acessível a partir do processo Python.
