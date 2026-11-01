"""Sentinela Telegram: private conversations, scoped API integration, no target fetching.
Uses only Python 3.10+ standard library. No credentials in this source file.
"""
import collections
import json
import os
import re
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

HELP = ('Sou o Sentinela, assistente de prevenção a golpes.\n'
        '/analisar https://exemplo.com — analisar um link\n'
        'Depois, faça uma pergunta sobre o resultado.\n'
        '/limpar — esquecer o contexto local\n/apagar confirmar — excluir análises desta conversa no Sentinela\n'
        '/id — mostrar seu identificador\n/privacidade — como os dados são tratados\n'
        'Nunca envie senhas, códigos de autenticação, cartões ou links com dados pessoais.')
PRIVACY = ('Conversas com bots do Telegram não têm criptografia de ponta a ponta. '
           'O Telegram e o operador do bot podem processar suas mensagens. '
           'Com a API configurada, o Sentinela guarda análises e perguntas com uma identidade '
           'pseudônima por conversa. Parâmetros e fragmentos da URL são removidos; caminhos '
           'ainda podem conter dados pessoais. /apagar confirmar exclui esses dados no Sentinela, '
           'mas não apaga mensagens no Telegram nem backups do operador. '
           'Use o aplicativo ou painel para conteúdos privados. Nunca envie senha ou código 2FA.')

class SafeError(Exception):
    pass

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

def request_json(url, data, headers=None, timeout=20, limit=1048576):
    """Do not propagate HTTP exceptions: their URLs can contain the Telegram token."""
    req = urllib.request.Request(url, json.dumps(data).encode(),
                                 {'Content-Type': 'application/json', **(headers or {})})
    try:
        with urllib.request.build_opener(NoRedirect()).open(req, timeout=timeout) as response:
            raw = response.read(limit + 1)
            if len(raw) > limit:
                raise SafeError('Resposta excedeu o limite.')
            result = json.loads(raw)
            if not isinstance(result, dict):
                raise SafeError('Resposta inválida.')
            return result
    except SafeError:
        raise
    except Exception:
        raise SafeError('Serviço indisponível ou solicitação recusada.') from None

def api_base(raw):
    if not raw:
        return ''
    p = urllib.parse.urlsplit(raw)
    if (p.username or p.password or p.query or p.fragment or not p.hostname
            or p.scheme not in ('http', 'https')
            or (p.scheme == 'http' and p.hostname not in ('localhost', '127.0.0.1', '::1'))):
        raise SafeError('SENTINELA_API_URL precisa usar HTTPS; HTTP somente no computador local.')
    if p.path.rstrip('/') not in ('', '/api/v1'):
        raise SafeError('Informe a raiz da API ou /api/v1.')
    return urllib.parse.urlunsplit((p.scheme, p.netloc, '/api/v1', '', ''))

def defang(text):
    return re.sub(r'(?i)\bhttps?://[^\s<>]+', lambda m: m.group(0).replace('https://', 'hxxps://').replace('http://', 'hxxp://').replace('.', '[.]'), text)

def chunks(text, maximum=3500):
    """Telegram limits use UTF-16 units, not Python codepoints."""
    part, units = [], 0
    for char in text[:24000]:
        width = 2 if ord(char) > 0xFFFF else 1
        if units + width > maximum:
            yield ''.join(part)
            part, units = [], 0
        part.append(char)
        units += width
    if part:
        yield ''.join(part)

class Bot:
    def __init__(self, env=None, transport=request_json, clock=time.monotonic):
        env = os.environ if env is None else env
        self.token = env.get('TELEGRAM_BOT_TOKEN', '').strip()
        if not re.fullmatch(r'[0-9]+:[A-Za-z0-9_-]{20,}', self.token):
            raise SafeError('Configure TELEGRAM_BOT_TOKEN nos segredos ou variáveis locais.')
        self.base = api_base(env.get('SENTINELA_API_URL', '').strip())
        self.key = env.get('TELEGRAM_API_KEY', '')
        if self.base and len(self.key) < 32:
            raise SafeError('Configure TELEGRAM_API_KEY com pelo menos 32 caracteres.')
        self.allowed = {int(v) for v in env.get('TELEGRAM_ALLOWED_USER_IDS', '').split(',') if v.strip().isdigit()}
        self.public = env.get('TELEGRAM_ALLOW_PUBLIC', '').lower() == 'true'
        self.transport, self.clock = transport, clock
        self.context = collections.OrderedDict()
        self.buckets = collections.OrderedDict()
        self.offset_file = Path(env.get('TELEGRAM_OFFSET_FILE', '.runtime/telegram-offset.json'))
        self.offset = 0
        try:
            state = json.loads(self.offset_file.read_text(encoding='utf-8'))
            if state.get('bot_id') == self.token.split(':')[0]:
                self.offset = max(0, int(state['offset']))
        except (OSError, ValueError, KeyError, TypeError):
            pass

    def telegram(self, method, body):
        if method not in {'getMe', 'getWebhookInfo', 'getUpdates', 'sendMessage'}:
            raise SafeError('Método não permitido.')
        result = self.transport('https://api.telegram.org/bot' + self.token + '/' + method, body, timeout=40)
        if not result.get('ok'):
            raise SafeError('Telegram recusou a solicitação.')
        return result.get('result')

    def backend(self, action, chat, user, **fields):
        if not self.base:
            raise SafeError('Análise indisponível: o operador precisa configurar a API do Sentinela. Posso explicar prevenção a golpes com /ajuda.')
        return self.transport(self.base + '/integrations/telegram/' + action,
                              {'chatId': str(chat), 'userId': str(user), **fields},
                              {'X-Sentinela-Telegram-Key': self.key}, timeout=30)

    def limited(self, user):
        now = self.clock()
        for bucket, maximum in (('global', 60), (str(user), 10)):
            times = self.buckets.setdefault(bucket, collections.deque())
            self.buckets.move_to_end(bucket)
            while times and times[0] <= now - 60:
                times.popleft()
            if len(times) >= maximum:
                return True
            times.append(now)
        while len(self.buckets) > 5000:
            self.buckets.popitem(last=False)
        return False

    def handle(self, update):
        message = update.get('message') or {}
        chat, sender = message.get('chat') or {}, message.get('from') or {}
        cid, uid = chat.get('id'), sender.get('id')
        if (chat.get('type') != 'private' or type(uid) is not int or type(cid) is not int
                or uid <= 0 or uid != cid or sender.get('is_bot')):
            return None
        text = message.get('text', '').strip()
        if not text:
            return None
        if self.limited(uid):
            return None
        if len(text) > 2048:
            return cid, 'Mensagem muito longa. Envie até 2.048 caracteres.'
        command, _, argument = text.partition(' ')
        command = command.split('@')[0].lower()
        if command in ('/start', '/ajuda', '/help'):
            return cid, HELP
        if command == '/id':
            return cid, 'Seu identificador: ' + str(uid) + '. Peça ao operador para autorizar este ID.'
        if command == '/privacidade':
            return cid, PRIVACY
        if not self.public and uid not in self.allowed:
            return cid, 'Acesso restrito. Use /id e solicite autorização ao operador.'
        actor = (cid, uid)
        if command == '/limpar':
            self.context.pop(actor, None)
            return cid, 'Contexto local removido. Para excluir os dados na API: /apagar confirmar.'
        if command == '/apagar':
            if argument.strip().lower() != 'confirmar':
                return cid, 'Para excluir análises desta conversa no Sentinela, envie /apagar confirmar.'
            self.backend('erase', cid, uid)
            self.context.pop(actor, None)
            return cid, 'Análises e perguntas desta conversa excluídas no Sentinela.'
        url = argument.strip() if command == '/analisar' else text if re.match(r'(?i)^https?://', text) else None
        if url is not None:
            if not url:
                return cid, 'Use /analisar seguido de um link HTTP ou HTTPS.'
            result = self.backend('analyze', cid, uid, url=url)
            self.context[actor] = result['id']
            self.context.move_to_end(actor)
            while len(self.context) > 5000:
                self.context.popitem(last=False)
            level = {'HIGH': 'alto', 'MEDIUM': 'médio', 'LOW': 'baixo'}.get(result.get('risk'), 'indeterminado')
            indicators = '\n'.join('• ' + str(i.get('title', i.get('code', 'Indicador'))) for i in result.get('indicators', [])[:12])
            sources = '\n'.join('• ' + str(s.get('name', s.get('source', 'Fonte'))) + ': ' + str(s.get('status', 'indisponível')) for s in result.get('sources', []))
            return cid, defang(f"Domínio: {result.get('domain', '')}\nRisco: {level} · Score: {result.get('score')}/100\n{indicators}\nFontes:\n{sources}\n{result.get('notice', '')}\nFaça uma pergunta para entender este resultado. Nunca considere uma análise garantia de segurança.")
        if len(text) > 1500:
            return cid, 'Perguntas devem ter até 1.500 caracteres.'
        if not self.base:
            return cid, 'Desconfie de urgência, pedidos de senha, pagamentos inesperados e domínios parecidos. Confirme pelo canal oficial. Análises de links exigem a API configurada. /ajuda'
        result = self.backend('chat', cid, uid, message=text, analysisId=self.context.get(actor))
        return cid, defang(str(result.get('answer', 'Não foi possível responder.')))

    def checkpoint(self, offset):
        self.offset_file.parent.mkdir(parents=True, exist_ok=True)
        temporary = self.offset_file.with_suffix('.tmp')
        temporary.write_text(json.dumps({'bot_id': self.token.split(':')[0], 'offset': offset}), encoding='utf-8')
        temporary.replace(self.offset_file)
        self.offset = offset

    def run(self):
        self.telegram('getMe', {})
        if self.telegram('getWebhookInfo', {}).get('url'):
            raise SafeError('Este bot tem webhook ativo. Desative-o conscientemente antes de usar long polling.')
        print('Sentinela iniciado. Interrompa com Ctrl+C. Não execute duas instâncias do mesmo bot.')
        backoff = 1
        while True:
            try:
                updates = self.telegram('getUpdates', {'offset': self.offset, 'timeout': 25, 'limit': 20, 'allowed_updates': ['message']})
                for update in updates:
                    message = update.get('message') or {}
                    try:
                        reply = self.handle(update)
                    except SafeError as exc:
                        chat = message.get('chat') or {}
                        reply = (chat.get('id'), str(exc)) if chat.get('type') == 'private' and chat.get('id') == (message.get('from') or {}).get('id') else None
                    # At-most-once handling across normal restarts; a crash before delivery can lose a reply.
                    self.checkpoint(int(update['update_id']) + 1)
                    if reply:
                        for part in chunks(reply[1]):
                            self.telegram('sendMessage', {'chat_id': reply[0], 'text': part, 'link_preview_options': {'is_disabled': True}, 'protect_content': True})
                backoff = 1
            except SafeError:
                print('Falha temporária de comunicação; nova tentativa sem registrar mensagens ou credenciais.')
                time.sleep(backoff)
                backoff = min(30, backoff * 2)

if __name__ == '__main__':
    try:
        Bot().run()
    except KeyboardInterrupt:
        print('Bot encerrado.')
    except SafeError as exc:
        print(str(exc))
    except Exception:
        # No traceback: transport errors can carry URLs containing the bot token.
        print('Falha interna. Confira a configuração e execute os testes locais, sem compartilhar segredos.')
