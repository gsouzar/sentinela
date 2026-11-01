"""Calcula métricas do CSV com expected e score; não acessa URLs."""
import csv,json,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"notebooks"))
from sentinela_chat import metrics
if len(sys.argv)!=3:raise SystemExit("Uso: python scripts/avaliar_dataset.py entrada.csv saida.json")
with open(sys.argv[1],encoding="utf-8-sig",newline="") as source:
    pairs=[]
    for line,row in enumerate(csv.DictReader(source),2):
        label=int(row["expected"]);score=float(row["score"])
        if label not in (0,1) or not 0<=score<=100:raise ValueError(f"Linha {line}: valores inválidos")
        pairs.append((label,int(score>=60)))
result={"threshold":60,"note":"A validade depende da origem e independência dos rótulos.",**metrics(pairs)}
Path(sys.argv[2]).write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding="utf-8")
print(f"Avaliadas {result['n']} linhas. Resultado salvo.")
