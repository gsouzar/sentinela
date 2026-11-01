"""Telegram privado -> API Java -> explicador local/Colab. Não executa links."""
import json
import os
import time
from dataclasses import dataclass
from urllib.parse import urlsplit
from urllib.request import Request, build_opener, HTTPRedirectHandler


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


@dataclass(frozen=True)
class Config:
    token: str
    api_base: str
    api_key: str
    allowed_users: frozenset

    def __post_init__(self):
        url = urlsplit(self.api_base)
        if (url.scheme not in ('http', 'https') or not url.hostname or url.username or
                url.password or url.query or url.fragment or
                url.scheme == 'http' and url.hostname not in ('127.0.0.1', 'localhost')):
            raise ValueError('API deve usar HTTPS; HTTP permitido apenas em loopback local.')
        if not self.token or ':' not in self.token or len(self.api_key) < 32:
            raise ValueError('Configure TELEGRAM_BOT_TOKEN e TELEGRAM_API_KEY (mínimo 32 caracteres).')
        if not self.allowed_users or any(not isinstance(x, int) or x <= 0 for x in self.allowed_users):
            raise ValueError('Defina TELEGRAM_ALLOWED_USER_IDS com seus IDs numéricos positivos.')

    @classmethod
    def from_env(cls):
        return cls(os.environ.get('TELEGRAM_BOT_TOKEN', ''),
                   os.environ.get('SENTINELA_API_BASE', 'http://127.0.0.1:8080/api/v1').rstrip('/'),
                   os.environ.get('TELEGRAM_API_KEY', ''),
                   frozenset(int(x.strip()) for x in os.environ.get('TELEGRAM_ALLOWED_USER_IDS', '').split(',') if x.strip()))


class Bot:
    def __init__(self, config):
        self.config = config
        self.context = {}  # IDs de análises; nunca guarda tokens humanos nem URL em disco.
        self.opener = build_opener(NoRedirect)

    def post(self, url, data, headers=None, timeout=20):
        request = Request(url, data=json.dumps(data).encode(),
                          headers={'Content-Type': 'application/json', **(headers or {})}, method='POST')
        with self.opener.open(request, timeout=timeout) as response:
            raw = response.read(1048577)
            if len(raw) > 1048576:
                raise ValueError('Resposta excedeu o limite.')
            return json.loads(raw)

    def telegram(self, method, data):
        # Nunca registre URL/erro bruto: o token faz parte da URL da Bot API.
        result = self.post('https://api.telegram.org/bot' + self.config.token + '/' + method, data, timeout=40)
        if not result.get('ok'):
            raise ValueError('Telegram indisponível.')
        return result.get('result')

    def backend(self, action, data):
        return self.post(self.config.api_base + '/integrations/telegram/' + action, data,
                         {'X-Sentinela-Telegram-Key': self.config.api_key})

    def send(self, chat, text):
        # Texto puro e preview desativado para evitar tornar URL suspeita uma chamada para abrir.
        self.telegram('sendMessage', {'chat_id': chat, 'text': text[:4000],
                      'link_preview_options': {'is_disabled': True}})

    def handle(self, update):
        msg = update.get('message', {})
        chat, sender = msg.get('chat', {}), msg.get('from', {})
        who = sender.get('id')
        if (chat.get('type') != 'private' or chat.get('id') != who or
                who not in self.config.allowed_users or sender.get('is_bot')):
            return
        text = msg.get('text', '').strip()
        if not text:
            return
        if len(text) > 2048:
            self.send(who, 'Envie até 2048 caracteres; perguntas devem ter até 1500.')
            return
        data = {'chatId': str(who), 'userId': str(who)}
        if text in ('/start', '/help', '/ajuda'):
            self.send(who, 'Sou o Sentinela. Envie um link HTTP(S) ou /analisar LINK. Depois pergunte sobre o resultado. '
                      'O score não garante segurança. /privacidade explica os dados; /apagar CONFIRMAR remove o histórico na API. '
                      'Nunca envie senhas, códigos 2FA, dados bancários ou links com segredos. Não uso sua conta do site.')
        elif text == '/privacidade':
            self.send(who, 'Mensagens passam pelo Telegram. Na API, uma identidade técnica pseudônima guarda análises e perguntas; '
                      'query e fragmento são removidos dos links. Reputação/Colab podem receber dados conforme configuração. '
                      '/apagar CONFIRMAR remove análises e chat da API, mas não mensagens do Telegram nem auditoria técnica. '
                      'Apague a conversa também no aplicativo Telegram.')
        elif text.startswith('/apagar'):
            if text != '/apagar CONFIRMAR':
                self.send(who, 'Para excluir análises e chat da API, envie exatamente /apagar CONFIRMAR.')
                return
            self.backend('erase', data)
            self.context.pop(who, None)
            self.send(who, 'Análises e chat removidos da API. Para remover mensagens no Telegram, apague a conversa no aplicativo.')
        elif text.startswith(('https://', 'http://', '/analisar ')):
            link = text[len('/analisar '):].strip() if text.startswith('/analisar ') else text
            result = self.backend('analyze', {**data, 'url': link})
            self.context[who] = result['id']
            self.send(who, f"Análise concluída: {result['risk']} · {result['score']}/100.\n"
                      f"Cobertura: {result.get('coverage', 'PARTIAL')}. Score não é probabilidade nem garantia.\n" +
                      '\n'.join('• ' + i['title'] + ': ' + i['explanation'] for i in result.get('indicators', [])) +
                      '\nPergunte sobre os indicadores. Confirme solicitações pelo canal oficial.')
        elif text.startswith('/'):
            self.send(who, 'Comandos: /ajuda, /analisar LINK, /privacidade e /apagar CONFIRMAR.')
        elif who not in self.context:
            self.send(who, 'Envie primeiro um link para receber uma explicação contextual. Use /ajuda para ver os comandos.')
        elif len(text) > 1500:
            self.send(who, 'Envie uma pergunta com até 1500 caracteres.')
        else:
            result = self.backend('chat', {**data, 'message': text, 'analysisId': self.context[who]})
            self.send(who, result['answer'])

    def run(self):
        # Não derruba webhook existente nem descarta mensagens pendentes silenciosamente.
        if self.telegram('getWebhookInfo', {}).get('url'):
            raise ValueError('Há um webhook ativo. Remova-o explicitamente antes de usar polling.')
        offset = None
        print('Sentinela Telegram iniciado. Ctrl+C interrompe. Apenas usuários autorizados.')
        while True:
            try:
                updates = self.telegram('getUpdates', {'offset': offset, 'timeout': 25,
                                        'limit': 20, 'allowed_updates': ['message']})
                for update in updates:
                    # Avança mesmo em erro: nunca repete exclusão/análise automaticamente.
                    offset = update['update_id'] + 1
                    try:
                        self.handle(update)
                    except Exception:
                        print('Uma mensagem não pôde ser processada. Confira a API; nenhum segredo foi registrado.')
            except KeyboardInterrupt:
                print('Bot encerrado.')
                return
            except Exception:
                print('Conexão temporariamente indisponível. Nova tentativa em 5 segundos.')
                time.sleep(5)


if __name__ == '__main__':
    try:
        Bot(Config.from_env()).run()
    except (ValueError, KeyboardInterrupt):
        raise SystemExit('Confira as variáveis do bot e o guia TELEGRAM.md. Nenhuma credencial é exibida.')
