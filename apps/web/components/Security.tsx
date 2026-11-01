"use client";
import { useEffect, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import {
  ShieldCheck,
  KeyRound,
  Monitor,
  LockKeyhole,
  Copy,
} from "lucide-react";
import { api, User, date } from "../lib/api";
export default function Security({
  user,
  onUpdate,
  onLogout,
}: {
  user: User;
  onUpdate: (u: User) => void;
  onLogout: () => void;
}) {
  const [sessions, setSessions] = useState<
      { id: string; created_at: number; expires_at: number }[]
    >([]),
    [secret, setSecret] = useState(""),
    [setupUri, setSetupUri] = useState(""),
    [codes, setCodes] = useState<string[]>([]),
    [busy, setBusy] = useState(false),
    [notice, setNotice] = useState(""),
    [error, setError] = useState("");
  useEffect(() => {
    api<typeof sessions>("/me/sessions")
      .then(setSessions)
      .catch((x) => setError(x.message));
  }, []);
  async function mfa(e: React.FormEvent<HTMLFormElement>, action: string) {
    e.preventDefault();
    setBusy(true);
    setError("");
    setNotice("");
    const f = new FormData(e.currentTarget);
    const form = e.currentTarget;
    try {
      const r = await api<{
        secret?: string;
        uri?: string;
        recoveryCodes?: string[];
      }>("/me/mfa/" + action, "POST", {
        password: f.get("password"),
        code: f.get("code") || undefined,
      });
      if (r.secret) {
        setSecret(r.secret);
        setSetupUri(r.uri || "");
        setCodes([]);
      }
      if (r.recoveryCodes) {
        setCodes(r.recoveryCodes);
        setSecret("");
      }
      if (action !== "setup") {
        setSecret("");
        setSetupUri("");
        form.reset();
        onUpdate(await api<User>("/me"));
        setNotice(
          action === "enable"
            ? "Segundo fator ativado. Guarde seus códigos de recuperação."
            : "Segundo fator desativado.",
        );
      }
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
          <span className="eyebrow">PROTEJA TAMBÉM SUA CONTA</span>
          <h1>Sua segurança, em camadas.</h1>
          <p>Gerencie autenticação, senha e sessões ativas.</p>
        </div>
        <span className="mini-icon">
          <LockKeyhole />
        </span>
      </div>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      {notice && (
        <p role="status" className="success">
          {notice}
        </p>
      )}
      <div className="security-grid">
        <section className="panel settings-panel">
          <span className="settings-symbol">
            <ShieldCheck />
          </span>
          <h3>Autenticação em dois fatores</h3>
          <p>
            Adicione um código temporário gerado no seu aplicativo autenticador.
          </p>
          <span className={"badge " + (user.mfa_enabled ? "LOW" : "MEDIUM")}>
            {user.mfa_enabled ? "Segundo fator ativo" : "Ainda não ativado"}
          </span>
          {secret && (
            <div className="secret-box">
              <b>Conecte o Google Authenticator</b>
              <p>
                No aplicativo, toque em + e em Ler QR code. Aponte a câmera para
                o código abaixo.
              </p>
              {setupUri && (
                <div className="mfa-qr">
                  <QRCodeSVG
                    value={setupUri}
                    size={232}
                    level="M"
                    marginSize={4}
                    title="QR code para adicionar sua conta Sentinela ao autenticador"
                    role="img"
                  />
                </div>
              )}
              <small>Conta: {user.email} · Sentinela</small>
              <p>
                Depois de escanear, digite abaixo o código de seis dígitos do
                aplicativo e clique em Confirmar e ativar.
              </p>
              <details>
                <summary>Não consegue escanear? Use a chave manual</summary>
                <code>{secret}</code>
                <small>
                  Tipo: baseado em tempo (TOTP) · 6 dígitos · 30 segundos
                </small>
              </details>
              <p>
                Não compartilhe o QR code nem a chave. A proteção só fica ativa
                após confirmar o código.
              </p>
            </div>
          )}
          {user.mfa_required && (
            <p>
              O segundo fator é obrigatório. Use um autenticador TOTP e guarde
              os códigos de recuperação em local seguro.
            </p>
          )}
          {!(user.mfa_required && user.mfa_enabled) && (
            <form
              onSubmit={(e) =>
                void mfa(
                  e,
                  user.mfa_enabled ? "disable" : secret ? "enable" : "setup",
                )
              }
            >
              <label>
                Confirme sua senha
                <input
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  required
                  maxLength={128}
                />
              </label>
              {(secret || user.mfa_enabled) && (
                <label>
                  Código do autenticador
                  <input
                    name="code"
                    autoComplete="one-time-code"
                    maxLength={40}
                    required
                  />
                </label>
              )}
              <button className="primary" disabled={busy}>
                {busy
                  ? "Aguarde…"
                  : user.mfa_enabled
                    ? "Desativar segundo fator"
                    : secret
                      ? "Confirmar e ativar"
                      : "Configurar segundo fator"}
              </button>
            </form>
          )}
          {codes.length > 0 && (
            <div className="recovery">
              <h4>Guarde estes códigos agora</h4>
              <p>
                Cada código só funciona uma vez. Eles não serão mostrados
                novamente.
              </p>
              <div>
                {codes.map((c) => (
                  <code key={c}>{c}</code>
                ))}
              </div>
              <button
                className="text-button"
                onClick={() =>
                  void navigator.clipboard.writeText(codes.join("\n"))
                }
              >
                <Copy size={16} /> Copiar códigos
              </button>
            </div>
          )}
        </section>
        <section className="panel settings-panel">
          <span className="settings-symbol">
            <KeyRound />
          </span>
          <h3>Uma senha só sua</h3>
          <p>
            Use pelo menos 12 caracteres. Ao alterar a senha, todas as suas
            sessões serão encerradas.
          </p>
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              setError("");
              const f = new FormData(e.currentTarget);
              try {
                await api("/me/password", "POST", {
                  password: f.get("password"),
                  newPassword: f.get("newPassword"),
                  code: f.get("code") || undefined,
                });
                onLogout();
              } catch (x) {
                setError((x as Error).message);
              } finally {
                setBusy(false);
              }
            }}
          >
            <label>
              Senha atual
              <input
                name="password"
                type="password"
                autoComplete="current-password"
                required
                maxLength={128}
              />
            </label>
            <label>
              Nova senha
              <input
                name="newPassword"
                type="password"
                autoComplete="new-password"
                required
                minLength={12}
                maxLength={128}
              />
            </label>
            {user.mfa_enabled && (
              <label>
                Código do segundo fator
                <input
                  name="code"
                  autoComplete="one-time-code"
                  required
                  maxLength={40}
                />
              </label>
            )}
            <button className="secondary" disabled={busy}>
              Alterar senha e sair
            </button>
          </form>
        </section>
      </div>
      <section className="panel sessions">
        <div className="panel-title">
          <div>
            <h3>Sessões da conta</h3>
            <span>Encerre os acessos que não reconhecer.</span>
          </div>
          <Monitor size={22} />
        </div>
        {sessions.map((s) => (
          <div className="session-row" key={s.id}>
            <Monitor size={19} />
            <div>
              <b>Sessão iniciada em {date(s.created_at)}</b>
              <small>Validade do acesso: {date(s.expires_at)}</small>
            </div>
            <button
              className="text-button danger"
              onClick={async () => {
                try {
                  await api("/me/sessions/" + s.id, "DELETE");
                  setSessions((x) => x.filter((t) => t.id !== s.id));
                  try {
                    await api("/me");
                  } catch {
                    onLogout();
                  }
                } catch (x) {
                  setError((x as Error).message);
                }
              }}
            >
              Encerrar
            </button>
          </div>
        ))}
      </section>
    </>
  );
}
