"""Conector de backend. Não coloque a chave em páginas públicas ou aplicativos distribuídos."""
import argparse,json,os,time,uuid
from urllib.parse import urlsplit
from urllib.request import Request,build_opener,HTTPRedirectHandler
TYPES={'AUTH_FAILURE','RATE_LIMIT','WAF_BLOCK','INTEGRITY_ALERT','APPLICATION_ERROR'}
class NoRedirect(HTTPRedirectHandler):
 def redirect_request(self,request,fp,code,msg,headers,newurl):return None
class SiteReporter:
 def __init__(self,endpoint,key):
  parsed=urlsplit(endpoint)
  if (parsed.scheme not in ('http','https') or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment or parsed.path!='/api/v1/ingest/events' or parsed.scheme=='http' and parsed.hostname not in ('127.0.0.1','localhost')):
   raise ValueError('Use endpoint HTTPS /api/v1/ingest/events; HTTP somente em loopback local.')
  if len(key)<32 or len(key)>200:raise ValueError('Chave de site inválida.')
  self.endpoint,self.key=endpoint,key;self.transport=build_opener(NoRedirect)
 def report(self,event_type,*,simulated=False,event_id=None):
  if event_type not in TYPES:raise ValueError('Tipo de evento inválido.')
  event_id=str(uuid.UUID(event_id)) if event_id else str(uuid.uuid4())
  body={'eventId':event_id,'type':event_type,'occurredAt':int(time.time()*1000),'simulated':bool(simulated)}
  request=Request(self.endpoint,data=json.dumps(body).encode(),headers={'Content-Type':'application/json','X-Sentinela-Site-Key':self.key},method='POST')
  with self.transport.open(request,timeout=10) as response:
   raw=response.read(16385)
   if len(raw)>16384:raise ValueError('Resposta excedeu limite.')
   return bool(json.loads(raw).get('accepted'))
if __name__=='__main__':
 parser=argparse.ArgumentParser(description='Envie somente simulações pelo terminal; integração real chama SiteReporter.report no backend.')
 parser.add_argument('--type',choices=sorted(TYPES),default='AUTH_FAILURE');parser.add_argument('--simulate',action='store_true',required=True)
 args=parser.parse_args()
 try:
  reporter=SiteReporter(os.environ.get('SENTINELA_EVENT_URL','http://127.0.0.1:8080/api/v1/ingest/events'),os.environ.get('SENTINELA_SITE_KEY',''))
  print('Simulação aceita.' if reporter.report(args.type,simulated=True) else 'Evento já registrado.')
 except Exception:
  raise SystemExit('Não foi possível enviar. Confira API, chave, domínio confirmado e situação da conta. Nenhum segredo foi exibido.')
