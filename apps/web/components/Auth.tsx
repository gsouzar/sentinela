"use client";
import { useState } from "react";
import {
  ShieldCheck,
  ArrowUpRight,
  Check,
  Link2,
  Eye,
  EyeOff,
  LockKeyhole,
} from "lucide-react";
import { api, User } from "../lib/api";
export default function Auth({ onLogin }: { onLogin: (u: User) => void }) {
  const [register, setRegister] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [mfa, setMfa] = useState(false),
    [show, setShow] = useState(false);
  async function submit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError("");
    const f = new FormData(e.currentTarget);
    try {
      const b = await api<{ user: User }>(
        "/auth/" + (register ? "register" : "login"),
        "POST",
        {
          ...(register ? { name: f.get("name") } : {}),
          email: f.get("email"),
          password: f.get("password"),
          ...(!register && f.get("code") ? { code: f.get("code") } : {}),
        },
      );
      onLogin(b.user);
    } catch (x) {
      const message = (x as Error).message;
      setError(message);
      if (message.includes("autenticador")) setMfa(true);
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="auth-page">
      <section className="auth-story">
        <div className="brand">
          <span className="brand-mark brand-logo" aria-hidden="true" />
          sentinela<span className="brand-dot">.</span>
        </div>
        <div className="story-body">
          <span className="eyebrow light">SUA PRÓXIMA CAMADA DE PROTEÇÃO</span>
          <h1>
            Antes do clique,
            <br />
            um olhar
            <br />
            <em>mais atento.</em>
          </h1>
          <p>
            Links suspeitos não precisam ser um mistério. Entenda os sinais de
            risco e decida com mais informação.
          </p>
          <div className="story-checks">
            <span>
              <Check size={17} /> Análise com critérios explicáveis
            </span>
            <span>
              <Check size={17} /> Orientação em linguagem simples
            </span>
          </div>
        </div>
        <div className="story-foot">
          <span>PROJETO ACADÊMICO · SENTINELA</span>
          <ShieldCheck size={20} />
        </div>
        <div className="auth-orbit" aria-hidden="true">
          <div>
            <span className="brand-symbol" />
          </div>
        </div>
      </section>
      <section className="auth-form-wrap">
        <div className="auth-top">
          <span>Segurança começa com informação.</span>
          <LockKeyhole size={17} />
        </div>
        <div className="auth-form">
          <span className="mini-icon">
            <Link2 />
          </span>
          <span className="eyebrow">SEU ESPAÇO SEGURO</span>
          <h2>{register ? "Vamos começar?" : "Bom ter você aqui."}</h2>
          <p>
            {register
              ? "Crie sua conta e faça sua primeira análise."
              : "Entre para analisar links e acompanhar seu histórico."}
          </p>
          <div className="auth-tabs">
            <button
              className={!register ? "selected" : ""}
              onClick={() => {
                setRegister(false);
                setError("");
                setMfa(false);
              }}
            >
              Entrar
            </button>
            <button
              className={register ? "selected" : ""}
              onClick={() => {
                setRegister(true);
                setError("");
                setMfa(false);
              }}
            >
              Criar conta
            </button>
          </div>
          <form onSubmit={submit}>
            {register && (
              <label>
                Como podemos chamar você?
                <input
                  name="name"
                  autoComplete="name"
                  maxLength={80}
                  required
                  placeholder="Seu nome"
                />
              </label>
            )}
            <label>
              Email
              <input
                name="email"
                type="email"
                autoComplete="email"
                maxLength={254}
                required
                placeholder="voce@exemplo.com"
              />
            </label>
            <label>
              Senha
              <div className="password-field">
                <input
                  name="password"
                  type={show ? "text" : "password"}
                  autoComplete={register ? "new-password" : "current-password"}
                  minLength={register ? 12 : 1}
                  maxLength={128}
                  required
                  placeholder={
                    register ? "Pelo menos 12 caracteres" : "Sua senha"
                  }
                />
                <button
                  type="button"
                  aria-label={show ? "Ocultar senha" : "Mostrar senha"}
                  onClick={() => setShow(!show)}
                >
                  {show ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </label>
            {mfa && (
              <label>
                Código do segundo fator
                <input
                  name="code"
                  autoComplete="one-time-code"
                  maxLength={40}
                  placeholder="Autenticador ou recuperação"
                  required
                />
              </label>
            )}
            {error && (
              <p role="alert" className="error">
                {error}
              </p>
            )}
            <button className="primary wide" disabled={busy}>
              {busy
                ? "Conectando…"
                : register
                  ? "Criar minha conta"
                  : "Entrar no Sentinela"}
              <ArrowUpRight size={18} />
            </button>
          </form>
          <p className="auth-disclaimer">
            O Sentinela avalia sinais de risco. Nenhuma análise garante que um
            endereço seja seguro.
          </p>
        </div>
        <div className="auth-bottom">
          <ShieldCheck size={15} /> Seus links, com mais contexto.
        </div>
      </section>
    </main>
  );
}
