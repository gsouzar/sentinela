import sys,unittest,json
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'connectors'))
from site_event_client import SiteReporter
class SiteConnectorTests(unittest.TestCase):
 def test_key_stays_in_header_and_simulation_is_explicit(self):
  reporter=SiteReporter('http://127.0.0.1:8080/api/v1/ingest/events','k'*43)
  seen=[]
  class Response:
   def __enter__(self):return self
   def __exit__(self,*args):pass
   def read(self,limit):return b'{"accepted":true}'
  class Transport:
   def open(self,request,timeout):seen.append(request);return Response()
  reporter.transport=Transport()
  self.assertTrue(reporter.report('AUTH_FAILURE',simulated=True))
  request=seen[0];self.assertNotIn('k'*43,request.full_url)
  self.assertEqual(request.get_header('X-sentinela-site-key'),'k'*43)
  self.assertTrue(json.loads(request.data)['simulated'])
 def test_requires_https_except_loopback_and_rejects_unknown_events(self):
  with self.assertRaises(ValueError):SiteReporter('http://remote.example/api/v1/ingest/events','k'*43)
  with self.assertRaises(ValueError):SiteReporter('https://api.example/api/v1/ingest/events','short')
  with self.assertRaises(ValueError):SiteReporter('https://api.example/api/v1/ingest/events','k'*43).report('arbitrary-event')
if __name__=='__main__':unittest.main()
