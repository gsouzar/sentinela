"use client";
import {
  ArrowLeft,
  MessageSquare,
  ShieldCheck,
  AlertTriangle,
  Check,
  Copy,
} from "lucide-react";
import { useState } from "react";
import { Analysis, date, riskLabel, sourceLabel } from "../lib/api";
export default function AnalysisResult({
  value,
  onChat,
  onBack,
}: {
  value: Analysis;
  onChat: () => void;
  onBack: () => void;
}) {
  const [copied, setCopied] = useState(false);
  return (
    <section className="result-page">
      <button className="text-button" onClick={onBack}>
        <ArrowLeft size={16} /> Voltar às análises
      </button>
      <div className="section-heading">
        <div>
          <span className="eyebrow">RELATÓRIO DE ANÁLISE</span>
          <h2>Um olhar sobre este link.</h2>
          <p>
            {date(value.createdAt)} · Motor {value.rulesVersion}
          </p>
        </div>
        <span className={"badge " + value.risk}>{riskLabel[value.risk]}</span>
      </div>
      <div className="result-grid">
        <div className={"score-panel " + value.risk}>
          <ShieldCheck size={30} />
          <span>SCORE DE RISCO</span>
          <strong>
            {value.score}
            <small>/100</small>
          </strong>
          <h3>{riskLabel[value.risk]}</h3>
          <p>
            {value.risk === "HIGH"
              ? "Evite inserir informações neste endereço."
              : "Confira os indicadores antes de decidir."}
          </p>
          <div className="score-track">
            <i style={{ width: value.score + "%" }} />
          </div>
          <small>Pontuação de regras, não probabilidade.</small>
        </div>
        <div className="panel result-info">
          <span className="eyebrow">ENDEREÇO ANALISADO</span>
          <h3 className="domain">{value.domain}</h3>
          <div className="url-display">
            <code>{value.url}</code>
            <button
              aria-label="Copiar endereço"
              onClick={async () => {
                await navigator.clipboard.writeText(value.url);
                setCopied(true);
              }}
            >
              {copied ? <Check size={18} /> : <Copy size={18} />}
            </button>
          </div>
          <div className="notice">
            <AlertTriangle size={18} />
            <p>
              {value.coverage === "PARTIAL"
                ? "Cobertura parcial. Há fontes sem configuração ou indisponíveis."
                : "Consultas concluídas. Ausência de registro não comprova segurança."}
            </p>
          </div>
          <p className="muted">{value.notice}</p>
          <button className="primary" onClick={onChat}>
            <MessageSquare size={17} /> Entender com o assistente
          </button>
        </div>
      </div>
      <div className="result-grid details-grid">
        <section className="panel">
          <div className="panel-title">
            <h3>O que encontramos</h3>
            <span>{value.indicators.length} indicadores</span>
          </div>
          {value.indicators.length === 0 ? (
            <div className="empty compact">
              <ShieldCheck />
              <h3>Poucos sinais, atenção sempre.</h3>
              <p>
                Não encontramos indicadores nas regras locais. Isso não garante
                segurança.
              </p>
            </div>
          ) : (
            value.indicators.map((i) => (
              <div className="indicator" key={i.id}>
                <span className="indicator-icon">
                  <AlertTriangle size={18} />
                </span>
                <div>
                  <h4>{i.title}</h4>
                  <p>{i.explanation}</p>
                  <code>{i.id}</code>
                </div>
                <b>+{i.weight}</b>
              </div>
            ))
          )}
        </section>
        <section className="panel">
          <div className="panel-title">
            <h3>Fontes e cobertura</h3>
            <span>Transparência da análise</span>
          </div>
          {value.sources.map((s) => (
            <div className="source" key={s.name}>
              <div className="source-heading">
                <b>{s.name}</b>
                <span className={"source-dot " + s.status} />
              </div>
              <span>{sourceLabel[s.status]}</span>
              <p>{s.detail}</p>
              <small>{date(s.checkedAt)}</small>
            </div>
          ))}
          <p className="muted">
            Query e fragmento são removidos para reduzir a exposição de dados
            sensíveis.
          </p>
        </section>
      </div>
    </section>
  );
}
