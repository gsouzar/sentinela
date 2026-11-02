import contextlib
import io
import json
from pathlib import Path
import types
import unittest
from unittest.mock import patch


class ColabUploadTests(unittest.TestCase):
    def execute_upload(self, uploaded):
        notebook = json.loads((Path(__file__).resolve().parents[1] / "Sentinela_Telegram_Colab.ipynb").read_text(encoding="utf-8"))
        source = next("".join(c["source"]) for c in notebook["cells"]
                      if c["cell_type"] == "code" and "files.upload()" in "".join(c["source"]))
        writes = {}

        class Destination:
            def __init__(self, name):
                self.name = name

            def write_bytes(self, content):
                writes[self.name] = content

        google = types.ModuleType("google")
        colab = types.ModuleType("google.colab")
        colab.files = types.SimpleNamespace(upload=lambda: uploaded)
        google.colab = colab
        with patch.dict("sys.modules", {"google": google, "google.colab": colab}), patch("pathlib.Path", Destination), contextlib.redirect_stdout(io.StringIO()):
            exec(source, {})
        return writes

    def test_colab_duplicate_suffix_is_saved_as_importable_module(self):
        self.assertEqual({"sentinela_telegram.py": b"# module"},
                         self.execute_upload({"sentinela_telegram (1).py": b"# module"}))

    def test_original_name_still_works(self):
        self.assertEqual({"sentinela_telegram.py": b"# module"},
                         self.execute_upload({"sentinela_telegram.py": b"# module"}))

    def test_wrong_file_is_rejected(self):
        with self.assertRaises(ValueError):
            self.execute_upload({"another_module.py": b"# wrong"})

    def test_multiple_versions_are_rejected(self):
        with self.assertRaises(ValueError):
            self.execute_upload({"sentinela_telegram.py": b"# one", "sentinela_telegram (1).py": b"# two"})


if __name__ == "__main__":
    unittest.main()

