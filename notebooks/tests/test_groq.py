import json,os,sys,unittest
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import sentinela_chat as chat

class Response:
 def __init__(self,value):self.value=value
 def __enter__(self):return self
 def __exit__(self,*args):pass
 def read(self,limit):return self.value[:limit]

class Transport:
 def __init__(self,result):self.result=result;self.requests=[]
 def open(self,request,timeout):
  self.requests.append((request,timeout))
  if isinstance(self.result,Exception):raise self.result
  return Response(self.result)

class GroqTests(unittest.TestCase):
 def setUp(self):
  self.analysis={'score':10,'risk':'LOW','coverage':'PARTIAL','url':'https://private.example/?secret=never-send','indicators':[{'id':'HTTP','title':'Sem HTTPS','explanation':'Conexão sem HTTPS'}]}
 def invoke(self,result,env=None):
  transport=Transport(result)
  with patch.dict(os.environ,{'GROQ_API_KEY':'test-only-provider-secret','GROQ_MODEL':'model-authorized-in-test',**(env or {})}),patch.object(chat,'_groq_transport',transport):
   response=chat.explain_with_groq('Explique',self.analysis)
  return response,transport
 def test_calls_groq_with_header_and_validates_evidence(self):
  raw=json.dumps({'choices':[{'message':{'content':json.dumps({'answer':'Há um indicador HTTP.','evidenceIds':['HTTP']})}}]}).encode()
  response,transport=self.invoke(raw)
  self.assertEqual('GROQ',response['providerMode']);self.assertEqual(['HTTP'],response['evidenceIds'])
  request,timeout=transport.requests[0]
  self.assertEqual('https://api.groq.com/openai/v1/chat/completions',request.full_url)
  self.assertEqual('Bearer test-only-provider-secret',request.get_header('Authorization'))
  self.assertNotIn('test-only-provider-secret',request.full_url)
  body=json.loads(request.data);self.assertEqual('model-authorized-in-test',body['model'])
  self.assertEqual({'type':'json_object'},body['response_format']);self.assertNotIn('never-send',str(body))
  self.assertEqual(10,self.analysis['score']);self.assertLessEqual(timeout,10)
 def test_missing_key_never_calls_network(self):
  response,t=self.invoke(b'',{'GROQ_API_KEY':''});self.assertEqual('LOCAL',response['providerMode']);self.assertEqual('NOT_CONFIGURED',response['providerStatus']);self.assertFalse(t.requests)
 def test_model_must_be_selected_from_account(self):
  response,t=self.invoke(b'',{'GROQ_MODEL':''});self.assertEqual('MODEL_NOT_SET',response['providerStatus']);self.assertFalse(t.requests)
 def test_invented_evidence_is_refused(self):
  raw=json.dumps({'choices':[{'message':{'content':json.dumps({'answer':'Uma invasão inventada','evidenceIds':['FAKE']})}}]}).encode()
  response,_=self.invoke(raw);self.assertEqual('LOCAL',response['providerMode']);self.assertNotIn('invasão inventada',response['answer'])
 def test_network_failure_does_not_disclose_key(self):
  response,_=self.invoke(RuntimeError('test-only-provider-secret'));self.assertEqual('LOCAL',response['providerMode']);self.assertNotIn('test-only-provider-secret',str(response));self.assertIn('10/100',response['answer'])
 def test_oversized_or_non_json_response_falls_back(self):
  for value in [b'x'*70000,b'not-json']:
   response,_=self.invoke(value);self.assertEqual('LOCAL',response['providerMode'])

if __name__=='__main__':unittest.main()
