import sys, unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from sentinela_telegram import Bot, Config

class TelegramTests(unittest.TestCase):
    def setUp(self):
        self.calls=[]
        self.bot=Bot(Config('123:fake', 'http://127.0.0.1:8080/api/v1', 'k'*32, frozenset({12})))
        self.bot.telegram=lambda method, data: self.calls.append((method,data)) or {}
        self.bot.backend=lambda action,data: self.calls.append((action,data)) or {'id':'analysis-1','score':40,'risk':'MEDIUM','coverage':'PARTIAL','indicators':[],'answer':'Resposta com evidências'}
    def message(self,text,who=12,kind='private'):
        return {'message':{'chat':{'id':who,'type':kind},'from':{'id':who},'text':text}}
    def test_denies_groups_and_unlisted_users(self):
        self.bot.handle(self.message('https://example.org',kind='group'))
        self.bot.handle(self.message('https://example.org',who=13))
        self.assertFalse(self.calls)
    def test_analyzes_then_explains_only_current_conversation(self):
        self.bot.handle(self.message('/analisar https://example.org'))
        self.bot.handle(self.message('Explique o resultado'))
        chat=next(data for action,data in self.calls if action=='chat')
        self.assertEqual(chat['analysisId'],'analysis-1')
        self.assertEqual(chat['userId'],'12')
        sends=[data for action,data in self.calls if action=='sendMessage']
        self.assertTrue(all(data['link_preview_options']['is_disabled'] for data in sends))
        self.assertTrue(all('parse_mode' not in data for data in sends))
    def test_erase_requires_explicit_confirmation_and_forgets_context(self):
        self.bot.handle(self.message('https://example.org'))
        self.bot.handle(self.message('/apagar'))
        self.assertFalse(any(a=='erase' for a,_ in self.calls))
        self.bot.handle(self.message('/apagar CONFIRMAR'))
        self.assertTrue(any(a=='erase' for a,_ in self.calls))
        self.assertNotIn(12,self.bot.context)
    def test_rejects_remote_plain_http_and_empty_allowlist(self):
        with self.assertRaises(ValueError): Config('123:fake','http://remote.example/api/v1','k'*32,frozenset({12}))
        with self.assertRaises(ValueError): Config('123:fake','https://api.example/api/v1','k'*32,frozenset())
    def test_without_analysis_provides_local_help(self):
        self.bot.handle(self.message('Como funciona?'))
        self.assertFalse(any(a=='chat' for a,_ in self.calls))
        self.assertTrue(any(a=='sendMessage' for a,_ in self.calls))

if __name__=='__main__': unittest.main()
