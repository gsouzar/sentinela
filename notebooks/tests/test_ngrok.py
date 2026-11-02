import contextlib
import io
import json
import os
from pathlib import Path
import types
import unittest
from unittest.mock import patch


class NgrokNotebookTests(unittest.TestCase):
    def test_tunnel_works_when_cli_reports_token_already_configured(self):
        notebook = json.loads((Path(__file__).resolve().parents[1] / "Sentinela_Chatbot_Colab.ipynb").read_text(encoding="utf-8"))
        source = next("".join(c["source"]) for c in notebook["cells"]
                      if c["cell_type"] == "code" and "ngrok.connect" in "".join(c["source"]))
        token = "synthetic-ngrok-token-do-not-disclose"
        module = types.ModuleType("pyngrok")

        class Config:
            def __init__(self, auth_token):
                self.auth_token = auth_token

        def save_token(*args, **kwargs):
            raise RuntimeError("Authtoken already set in configuration file")

        connected = []

        def connect(addr, bind_tls, pyngrok_config=None):
            self.assertEqual(5000, addr)
            self.assertTrue(bind_tls)
            self.assertEqual(token, pyngrok_config.auth_token)
            connected.append(True)
            return types.SimpleNamespace(public_url="https://synthetic.example")

        module.conf = types.SimpleNamespace(PyngrokConfig=Config)
        module.ngrok = types.SimpleNamespace(set_auth_token=save_token, connect=connect)
        output = io.StringIO()
        with patch.dict(os.environ, {"NGROK_AUTHTOKEN": token}), patch.dict("sys.modules", {"pyngrok": module}), contextlib.redirect_stdout(output):
            exec(source, {"os": os})
        self.assertEqual([True], connected)
        self.assertIn("COLAB_CHAT_URL=https://synthetic.example/explain", output.getvalue())
        self.assertNotIn(token, output.getvalue())


if __name__ == "__main__":
    unittest.main()

