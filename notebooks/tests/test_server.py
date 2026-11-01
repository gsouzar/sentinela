import unittest,sys,os
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from sentinela_chat import create_app
class ServerTests(unittest.TestCase):
    def setUp(self):
        self.key="test-only-shared-key-with-more-than-32-characters"
        self.client=create_app(self.key).test_client()
        self.analysis={"score":0,"risk":"LOW","coverage":"PARTIAL","indicators":[]}
    def test_requires_shared_key_and_valid_analysis(self):
        self.assertEqual(401,self.client.post("/explain",json={"analysis":self.analysis}).status_code)
        self.assertEqual(400,self.client.post("/explain",headers={"X-Sentinela-Key":self.key},json={"analysis":{}}).status_code)
    def test_returns_evidence_and_explanation_without_model_key(self):
        with patch.dict(os.environ,{"GROQ_API_KEY":""}):
            response=self.client.post("/explain",headers={"X-Sentinela-Key":self.key},json={"question":"Por que?","analysis":self.analysis})
        self.assertEqual(200,response.status_code)
        self.assertEqual([],response.json["evidenceIds"])
        self.assertIn("0/100",response.json["answer"])
if __name__=="__main__":unittest.main()
