"use client";
import { useState, useEffect, useRef } from "react";
import { MessageSquare, ArrowUp, ShieldCheck, X, Trash2 } from "lucide-react";
import { api, Analysis, riskLabel } from "../lib/api";
type Message = { id: string; question: string; answer: string; mode: string };
export default function Chat({
  selected,
  clearSelection,
}: {
  selected: Analysis | null;
  clearSelection: () => void;
}) {
  const [messages, setMessages] = useState<Message[]>([]),
    [text, setText] = useState(""),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const end = useRef<HTMLDivElement>(null);
  useEffect(() => {
    api<Message[]>("/chat")
      .then((x) => setMessages(x.reverse()))
      .catch((x) => setError(x.message));
  }, []);
  async function send(e: React.FormEvent) {
    e.preventDefault();
    if (!text.trim() || busy) return;
    setBusy(true);
    setError("");
    try {
      const r = await api<{ id: string; answer: string; mode: string }>(
        "/chat",
        "POST",
        { message: text, analysisId: selected?.id },
      );
      setMessages((m) => [...m, { ...r, question: text }]);
      setText("");
      setTimeout(() => end.current?.scrollIntoView({ block: "nearest" }), 30);
    } catch (x) {
      setError((x as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <div className="section-heading">
        <div>
          <span className="eyebrow">INFORMAÇÃO QUE PROTEGE</span>
          <h1>Vamos entender os sinais?</h1>
          <p>Um assistente para traduzir riscos em próximos passos.</p>
        </div>
        <button
          className="text-button danger"
          onClick={async () => {
            if (window.confirm("Excluir seu histórico de conversas?"))
              try {
                await api("/chat", "DELETE");
                setMessages([]);
              } catch (x) {
                setError((x as Error).message);
              }
          }}
        >
          <Trash2 size={16} /> Limpar conversa
        </button>
      </div>
      <div className="chat-layout">
        <section className="panel chat-panel">
          <div className="chat-head">
            <span className="mini-icon">
              <ShieldCheck />
            </span>
            <div>
              <h3>Assistente Sentinela</h3>
              <span>Explicações baseadas em evidências</span>
            </div>
            <span className="chat-tag">EDUCATIVO</span>
          </div>
          {selected && (
            <div className="chat-context">
              <div>
                <b>{selected.domain}</b>
                <small>
                  {riskLabel[selected.risk]} · {selected.score}/100
                </small>
              </div>
              <button
                aria-label="Remover contexto da análise"
                onClick={clearSelection}
              >
                <X size={17} />
              </button>
            </div>
          )}
          <div className="messages" aria-live="polite">
            {!messages.length && (
              <div className="chat-welcome">
                <MessageSquare size={34} />
                <h3>Como posso ajudar?</h3>
                <p>
                  Posso explicar uma análise ou orientar sobre golpes, senhas e
                  hábitos mais seguros.
                </p>
              </div>
            )}
            {messages.map((m) => (
              <div key={m.id} className="exchange">
                <div className="message user-message">{m.question}</div>
                <div className="assistant-label">
                  <ShieldCheck size={15} /> SENTINELA{" "}
                  <span>
                    {m.mode === "COLAB"
                      ? "COLAB"
                      : m.mode === "LOCAL_FALLBACK"
                        ? "EXPLICADOR LOCAL · FALLBACK"
                        : "EXPLICADOR LOCAL"}
                  </span>
                </div>
                <div className="message assistant-message">{m.answer}</div>
              </div>
            ))}
            {busy && (
              <p role="status" className="chat-thinking">
                Preparando uma explicação…
              </p>
            )}
            <div ref={end} />
          </div>
          {error && (
            <p className="error" role="alert">
              {error}
            </p>
          )}
          <form className="chat-compose" onSubmit={send}>
            <label className="sr-only" htmlFor="chat-input">
              Sua pergunta
            </label>
            <textarea
              id="chat-input"
              value={text}
              onChange={(e) => setText(e.target.value)}
              maxLength={1500}
              placeholder="Escreva sua dúvida sobre segurança…"
              required
              rows={2}
            />
            <button
              className="primary"
              disabled={busy || !text.trim()}
              aria-label="Enviar pergunta"
            >
              <ArrowUp size={20} />
            </button>
          </form>
          <p className="chat-disclaimer">
            Não envie senhas, códigos de autenticação ou dados bancários.
          </p>
        </section>
        <aside className="chat-aside">
          <span className="eyebrow">POR ONDE COMEÇAR</span>
          <h3>
            Uma pergunta já
            <br />
            faz diferença.
          </h3>
          {[
            "Por que esse link é suspeito?",
            "Cliquei em um link. E agora?",
            "Como criar uma senha mais segura?",
            "Como evitar golpes no Pix?",
          ].map((q) => (
            <button
              key={q}
              onClick={() => {
                setText(q);
                document.getElementById("chat-input")?.focus();
              }}
            >
              {q}
              <ArrowUp size={16} />
            </button>
          ))}
          <div className="notice">
            <ShieldCheck size={20} />
            <p>O assistente explica. O motor de análise calcula o risco.</p>
          </div>
        </aside>
      </div>
    </>
  );
}
