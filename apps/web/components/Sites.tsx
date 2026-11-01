"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  Activity,
  ArrowLeft,
  CheckCircle2,
  ChevronRight,
  Clock,
  Copy,
  Globe,
  KeyRound,
  Link2,
  Pause,
  Play,
  Plus,
  RefreshCw,
  ShieldCheck,
  TriangleAlert,
} from "lucide-react";
import { api, date } from "../lib/api";
type Site = {
  id: string;
  name: string;
  origin: string;
  domain: string;
  revision: number;
  enabled: boolean;
  verified: boolean;
  integrationConfigured: boolean;
  checking: boolean;
  dnsName: string;
  dnsValue: string;
  verified_at: number | null;
};
type Check = {
  checked_at: number;
  status: string;
  response_ms: number;
  http_code: number;
  tls_expires: number | null;
};
type Event = {
  event_id: string;
  type: string;
  simulated: boolean;
  occurred_at: number;
  received_at: number;
};
type Dashboard = {
  site: Site;
  availability: number | null;
  sampleCount: number;
  checks: Check[];
  events: Event[];
  reportedEvents: number;
  simulatedEvents: number;
  eventSeries: { bucket: number; total: number }[];
  latest: null | {
    status: string;
    responseMs: number;
    httpCode: number;
    tlsExpires: number | null;
    message: string;
    findings: { id: string; title: string; detail: string; severity: string }[];
  };
  notice: string;
};
const statusNames: Record<string, string> = {
  UP: "Respondendo",
  DOWN: "Erro no servidor",
  REDIRECT: "Redirecionamento",
  RESTRICTED: "Acesso restrito",
  UNREACHABLE: "Não alcançado",
  TLS_ERROR: "Falha TLS",
  VERIFICATION_LOST: "Confirmação perdida",
  DNS_UNAVAILABLE: "DNS temporariamente indisponível",
};
const eventNames: Record<string, string> = {
  AUTH_FAILURE: "Autenticação recusada",
  RATE_LIMIT: "Limite de requisições",
  WAF_BLOCK: "Bloqueio reportado pelo WAF",
  INTEGRITY_ALERT: "Alerta de integridade",
  APPLICATION_ERROR: "Erro de aplicação",
};
function Plot({
  values,
  label,
  color = "mint",
}: {
  values: number[];
  label: string;
  color?: string;
}) {
  if (!values.length)
    return (
      <div className="mon-no-chart">
        <Activity />
        <p>Aguardando medições reais.</p>
        <small>O gráfico aparece após as primeiras verificações.</small>
      </div>
    );
  const samples = values;
  const max = Math.max(1, ...samples);
  const points = samples
    .map(
      (v, i) =>
        `${12 + i * (696 / Math.max(1, samples.length - 1))},${164 - (v / max) * 140}`,
    )
    .join(" ");
  return (
    <svg
      className={"mon-plot " + color}
      viewBox="0 0 720 190"
      role="img"
      aria-label={label}
      preserveAspectRatio="none"
    >
      <title>{label}</title>
      {[24, 70, 116, 164].map((y) => (
        <line key={y} x1="12" x2="708" y1={y} y2={y} className="mon-gridline" />
      ))}
      <polyline
        points={points}
        fill="none"
        stroke="currentColor"
        strokeWidth="2.5"
        vectorEffect="non-scaling-stroke"
      />
      {samples.length === 1 && (
        <circle
          cx="12"
          cy={164 - (samples[0] / max) * 140}
          r="4"
          fill="currentColor"
        />
      )}
    </svg>
  );
}
export default function Sites() {
  const [sites, setSites] = useState<Site[]>([]),
    [id, setId] = useState<string | null>(null),
    [data, setData] = useState<Dashboard | null>(null),
    [days, setDays] = useState("7"),
    [form, setForm] = useState<"create" | "edit" | null>(null),
    [busy, setBusy] = useState(false),
    [loading, setLoading] = useState(true),
    [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [key, setKey] = useState(""),
    [keyForm, setKeyForm] = useState(false),
    [nonce, setNonce] = useState(0),
    [answer, setAnswer] = useState("");
  const request = useRef(0),
    alive = useRef(true),
    currentSite = useRef(id);
  currentSite.current = id;
  useEffect(() => {
    alive.current = true;
    return () => {
      alive.current = false;
      request.current++;
    };
  }, []);
  const list = useCallback(async () => {
    const rows = await api<Site[]>("/sites");
    if (alive.current) setSites(rows);
    return rows;
  }, []);
  useEffect(() => {
    list()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [list]);
  useEffect(() => {
    setData(null);
    setKey("");
    setKeyForm(false);
    setAnswer("");
    setError("");
    if (!id) return;
    let active = true;
    async function refresh() {
      const ticket = ++request.current;
      try {
        const result = await api<Dashboard>(
          `/sites/${id}/dashboard?days=${days}`,
        );
        if (active && ticket === request.current) setData(result);
      } catch (e) {
        if (active && ticket === request.current)
          setError((e as Error).message);
      }
    }
    void refresh();
    const timer = setInterval(() => void refresh(), 30000);
    return () => {
      active = false;
      request.current++;
      clearInterval(timer);
    };
  }, [id, days, nonce]);
  const selected = data?.site || sites.find((s) => s.id === id);
  async function run(path: string, method = "POST", body?: unknown) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const result = await api<Record<string, unknown>>(path, method, body);
      if (alive.current) {
        await list();
        setNonce((x) => x + 1);
      }
      return result;
    } catch (e) {
      if (alive.current) setError((e as Error).message);
      return null;
    } finally {
      if (alive.current) setBusy(false);
    }
  }
  async function save(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const f = new FormData(event.currentTarget);
    const result = await run(
      form === "edit" ? `/sites/${id}` : "/sites",
      form === "edit" ? "PATCH" : "POST",
      { name: f.get("name"), url: f.get("url") },
    );
    if (result) {
      setId(result.id as string);
      setForm(null);
      setNotice(
        "Site salvo. A origem é monitorada pela raiz do domínio; confirme o TXT para iniciar.",
      );
    }
  }
  return (
    <div className="monitor-area">
      <div className="section-heading">
        <div>
          <span className="eyebrow">ÁREA DO CLIENTE · SEUS DOMÍNIOS</span>
          <h1>
            {id
              ? selected?.name || "Painel do site"
              : "Seus sites. Um olhar contínuo."}
          </h1>
          <p>
            {id
              ? selected?.origin
              : "Cadastre seu domínio, confirme a propriedade e acompanhe verificações e eventos."}
          </p>
        </div>
        <button
          className="primary"
          onClick={() => {
            setForm("create");
            setError("");
          }}
        >
          <Plus size={17} /> Adicionar site
        </button>
      </div>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
      {form && (
        <section className="panel mon-form">
          <div className="panel-title">
            <h2>{form === "edit" ? "Editar site" : "Cadastrar seu site"}</h2>
            <button className="text-button" onClick={() => setForm(null)}>
              Cancelar
            </button>
          </div>
          <form onSubmit={(e) => void save(e)} key={form + id}>
            <label>
              Nome do site
              <input
                name="name"
                required
                maxLength={80}
                placeholder="Minha loja"
                defaultValue={form === "edit" ? selected?.name : ""}
              />
            </label>
            <label>
              URL pública
              <input
                name="url"
                type="url"
                required
                maxLength={2048}
                placeholder="https://meusite.com.br"
                defaultValue={form === "edit" ? selected?.origin : ""}
              />
            </label>
            <p>
              Apenas domínios sob seu controle, HTTP/HTTPS em portas padrão.
              Caminho, query e fragmento são descartados: o monitor verifica a
              raiz.
            </p>
            {form === "edit" && (
              <p className="mon-warning">
                Trocar a origem exige nova confirmação DNS e revoga a chave do
                conector. O histórico anterior continua separado pela revisão do
                domínio.
              </p>
            )}
            <button className="primary" disabled={busy}>
              {busy ? "Salvando…" : "Salvar site"}
            </button>
          </form>
        </section>
      )}
      {!id && (
        <>
          <section className="mon-intro">
            <div>
              <span className="mon-live-dot" />
              <b>Observe antes. Entenda melhor.</b>
              <p>
                Disponibilidade, resposta, certificado e eventos de segurança,
                com a origem de cada dado identificada.
              </p>
            </div>
            <ShieldCheck size={68} />
          </section>
          {loading ? (
            <p role="status">Carregando seus sites…</p>
          ) : sites.length === 0 ? (
            <section className="panel mon-empty">
              <Globe size={38} />
              <h2>Seu primeiro site começa aqui.</h2>
              <p>
                Adicione uma URL para abrir o painel. Até confirmar o domínio,
                ele mostra o que falta configurar, sem inventar métricas.
              </p>
              <button className="primary" onClick={() => setForm("create")}>
                Cadastrar meu site <ChevronRight size={17} />
              </button>
            </section>
          ) : (
            <div className="mon-sites">
              {sites.map((site) => (
                <button
                  className="mon-site-card"
                  key={site.id}
                  onClick={() => {
                    setId(site.id);
                    setNotice("");
                  }}
                >
                  <span className="mon-site-icon">
                    <Globe />
                  </span>
                  <div>
                    <h2>{site.name}</h2>
                    <p>{site.origin}</p>
                    <span
                      className={
                        "badge " +
                        (site.verified && site.enabled ? "LOW" : "MEDIUM")
                      }
                    >
                      {!site.enabled
                        ? "Pausado"
                        : site.verified
                          ? "Domínio confirmado"
                          : "Aguardando confirmação"}
                    </span>
                  </div>
                  <ChevronRight />
                </button>
              ))}
            </div>
          )}
          <p className="mon-footnote">
            Até cinco sites por conta. A coleta ocorre enquanto a API está
            ligada. Monitoramento externo não é proteção ativa nem comprovação
            de ausência de vulnerabilidades.
          </p>
        </>
      )}
      {id && !data && <p role="status">Carregando o painel…</p>}
      {id && data && (
        <>
          <div className="mon-toolbar">
            <button
              className="text-button"
              onClick={() => {
                setId(null);
                setForm(null);
              }}
            >
              {" "}
              <ArrowLeft size={16} /> Todos os sites
            </button>
            <div>
              <select
                aria-label="Período das métricas"
                value={days}
                onChange={(e) => setDays(e.target.value)}
              >
                <option value="1">Últimas 24 horas</option>
                <option value="7">Últimos 7 dias</option>
                <option value="30">Últimos 30 dias</option>
              </select>
              <button className="secondary" onClick={() => setForm("edit")}>
                Editar site
              </button>
              <button
                className="secondary"
                disabled={busy}
                onClick={() =>
                  void run(`/sites/${id}/status`, "PATCH", {
                    enabled: !data.site.enabled,
                  })
                }
              >
                {data.site.enabled ? <Pause size={15} /> : <Play size={15} />}{" "}
                {data.site.enabled ? "Pausar" : "Retomar"}
              </button>
            </div>
          </div>
          {!data.site.verified && (
            <section className="panel mon-verification">
              <div>
                <span className="settings-symbol">
                  <ShieldCheck />
                </span>
                <h2>Confirme que este domínio é seu.</h2>
                <p>
                  No painel DNS do seu provedor, adicione um registro <b>TXT</b>
                  . Se o provedor já acrescenta o domínio, use somente{" "}
                  <code>_sentinela</code> no campo Nome.
                </p>
              </div>
              <div className="mon-dns">
                <label>
                  Nome<code>{data.site.dnsName}</code>
                </label>
                <label>
                  Valor<code>{data.site.dnsValue}</code>
                </label>
                <small>
                  Mantenha o registro: a propriedade é conferida novamente a
                  cada coleta. A propagação depende do provedor.
                </small>
                <button
                  className="primary"
                  disabled={busy}
                  onClick={() =>
                    void run(`/sites/${id}/verify`).then(
                      (r) =>
                        r &&
                        setNotice(
                          "Domínio confirmado. A coleta ocorrerá automaticamente; você também pode verificar agora.",
                        ),
                    )
                  }
                >
                  <CheckCircle2 size={16} />{" "}
                  {busy ? "Conferindo…" : "Já adicionei: confirmar domínio"}
                </button>
              </div>
            </section>
          )}
          <section className="mon-dashboard" aria-label="Métricas do domínio">
            <div className="mon-dashboard-head">
              <div>
                <span className="mon-overline">
                  SENTINELA / OBSERVABILIDADE EXTERNA
                </span>
                <h2>{data.site.domain}</h2>
                <p>
                  <span
                    className={
                      "mon-status-dot " +
                      (!data.site.enabled
                        ? "paused"
                        : data.latest?.status === "UP"
                          ? "up"
                          : "pending")
                    }
                  />
                  {!data.site.enabled
                    ? "Monitoramento pausado"
                    : !data.site.verified
                      ? "Confirmação pendente"
                      : data.latest
                        ? statusNames[data.latest.status] || data.latest.status
                        : "Aguardando primeira amostra"}
                  <span>Revisão {data.site.revision}</span>
                </p>
              </div>
              <button
                disabled={
                  busy ||
                  !data.site.verified ||
                  !data.site.enabled ||
                  data.site.checking
                }
                onClick={() =>
                  void run(`/sites/${id}/check`).then(
                    (r) =>
                      r &&
                      setNotice(
                        "Verificação enfileirada. O painel atualiza a cada 30 segundos.",
                      ),
                  )
                }
              >
                <RefreshCw size={15} />{" "}
                {data.site.checking
                  ? "Verificação pendente"
                  : "Verificar agora"}
              </button>
            </div>
            <div className="mon-metrics">
              <div>
                <span>
                  <Activity size={16} /> Disponibilidade nas amostras
                </span>
                <strong>
                  {data.availability === null ? "—" : `${data.availability}%`}
                </strong>
                <small>{data.sampleCount} verificações no período</small>
              </div>
              <div>
                <span>
                  <Clock size={16} /> Tempo da última verificação
                </span>
                <strong>
                  {data.latest?.httpCode ? `${data.latest.responseMs}` : "—"}
                  <em>{data.latest?.httpCode ? " ms" : ""}</em>
                </strong>
                <small>DNS + conexão + TLS + resposta HEAD</small>
              </div>
              <div>
                <span>
                  <ShieldCheck size={16} /> Certificado HTTPS
                </span>
                <strong className="mon-small-value">
                  {data.latest?.tlsExpires
                    ? date(data.latest.tlsExpires)
                    : "Sem medição"}
                </strong>
                <small>Validade observada; não é selo de segurança</small>
              </div>
              <div>
                <span>
                  <TriangleAlert size={16} /> Eventos do conector
                </span>
                <strong>
                  {data.site.integrationConfigured ? data.reportedEvents : "—"}
                </strong>
                <small>
                  {data.site.integrationConfigured
                    ? `${data.simulatedEvents} simulados, contados separadamente`
                    : "Integração ainda não configurada"}
                </small>
              </div>
            </div>
            <div className="mon-charts">
              <article>
                <div className="mon-chart-title">
                  <div>
                    <h3>Tempo de resposta</h3>
                    <p>Somente amostras com resposta HTTP · ms</p>
                  </div>
                  <span className="mon-chart-key">HEAD</span>
                </div>
                <Plot
                  values={data.checks
                    .filter((c) => c.http_code > 0)
                    .map((c) => c.response_ms)}
                  label="Evolução do tempo das verificações HTTP em milissegundos"
                />
                <div className="mon-chart-axis">
                  <span>
                    {data.checks[0]
                      ? date(data.checks[0].checked_at)
                      : "Sem início"}
                  </span>
                  <span>Última coleta</span>
                </div>
              </article>
              <article>
                <div className="mon-chart-title">
                  <div>
                    <h3>Eventos reportados</h3>
                    <p>Contagem por hora · simulações excluídas</p>
                  </div>
                  <span className="mon-chart-key amber">CONECTOR</span>
                </div>
                <Plot
                  color="amber"
                  values={
                    data.site.integrationConfigured
                      ? data.eventSeries.map((e) => e.total)
                      : []
                  }
                  label="Eventos reais reportados por hora pelo conector"
                />
                <p className="mon-chart-note">
                  Sem eventos recebidos não significa ausência de ataques. São
                  relatos da aplicação ou WAF, não invasões confirmadas.
                </p>
              </article>
            </div>
            <div className="mon-availability">
              <h3>Últimas verificações</h3>
              <div className="mon-check-strip">
                {data.checks.slice(-80).map((c, i) => (
                  <span
                    key={i}
                    className={
                      ["UP", "REDIRECT"].includes(c.status) ? "good" : "bad"
                    }
                    title={`${date(c.checked_at)} · ${statusNames[c.status] || c.status} · HTTP ${c.http_code}`}
                    aria-label={`${date(c.checked_at)}: ${statusNames[c.status] || c.status}`}
                  />
                ))}
              </div>
              <p>
                {data.latest?.message ||
                  "Sem amostras. A coleta recorrente começa após a confirmação DNS, com a API ligada."}
              </p>
            </div>
          </section>
          <div className="mon-bottom-grid">
            <section className="panel">
              <div className="panel-title">
                <h2>Achados de configuração</h2>
                <span>Origem: verificação externa</span>
              </div>
              {!data.latest ? (
                <p>Aguardando verificação.</p>
              ) : data.latest.findings.length ? (
                data.latest.findings.map((item) => (
                  <div className="mon-finding" key={item.id}>
                    <TriangleAlert size={18} />
                    <div>
                      <b>{item.title}</b>
                      <p>{item.detail}</p>
                    </div>
                  </div>
                ))
              ) : (
                <p>
                  Nenhum achado registrado nessa amostra. Isso não comprova
                  segurança completa.
                </p>
              )}
              <p className="mon-footnote">
                Não há exploração de falhas, varredura de portas ou execução do
                conteúdo do destino.
              </p>
            </section>
            <section className="panel">
              <div className="panel-title">
                <h2>Integração do seu site</h2>
                <KeyRound size={20} />
              </div>
              <p>
                Receba eventos do seu backend ou WAF: autenticações recusadas,
                bloqueios, limites, integridade e erros. A chave é exclusiva
                deste site.
              </p>
              <span
                className={
                  "badge " +
                  (data.site.integrationConfigured ? "LOW" : "MEDIUM")
                }
              >
                {data.site.integrationConfigured
                  ? "Chave emitida"
                  : "Não configurada"}
              </span>
              {!keyForm && !key && (
                <button
                  className="secondary mon-space"
                  disabled={!data.site.verified}
                  onClick={() => setKeyForm(true)}
                >
                  {data.site.integrationConfigured
                    ? "Rotacionar chave"
                    : "Gerar chave de integração"}
                </button>
              )}
              {keyForm && (
                <form
                  className="mon-space"
                  onSubmit={async (e) => {
                    e.preventDefault();
                    const targetSite = id;
                    const element = e.currentTarget;
                    const f = new FormData(element);
                    setBusy(true);
                    setError("");
                    try {
                      const result = await api<{ key: string }>(
                        `/sites/${id}/integration-key`,
                        "POST",
                        {
                          password: f.get("password"),
                          code: f.get("code") || undefined,
                        },
                      );
                      if (alive.current && currentSite.current === targetSite) {
                        setKey(result.key);
                        setKeyForm(false);
                        setData((d) =>
                          d
                            ? {
                                ...d,
                                site: {
                                  ...d.site,
                                  integrationConfigured: true,
                                },
                              }
                            : d,
                        );
                        await list();
                      }
                      element.reset();
                    } catch (x) {
                      setError((x as Error).message);
                    } finally {
                      setBusy(false);
                    }
                  }}
                >
                  <label>
                    Confirme sua senha
                    <input
                      name="password"
                      type="password"
                      required
                      autoComplete="current-password"
                    />
                  </label>
                  <label>
                    Código 2FA, se estiver ativo
                    <input
                      name="code"
                      autoComplete="one-time-code"
                      maxLength={40}
                    />
                  </label>
                  <button className="primary" disabled={busy}>
                    Gerar e revogar chave anterior
                  </button>
                </form>
              )}
              {key && (
                <div className="secret-box mon-space">
                  <b>Copie agora. Não será exibida novamente.</b>
                  <code>{key}</code>
                  <button
                    className="text-button"
                    onClick={() =>
                      void navigator.clipboard.writeText(key).then(
                        () =>
                          setNotice(
                            "Chave copiada. Guarde somente no backend.",
                          ),
                        () =>
                          setError(
                            "Não foi possível copiar. Selecione a chave manualmente.",
                          ),
                      )
                    }
                  >
                    <Copy size={15} /> Copiar chave
                  </button>
                </div>
              )}
              <p className="mon-footnote">
                Envie para <code>/api/v1/ingest/events</code> no servidor
                Sentinela com <code>X-Sentinela-Site-Key</code>. Consulte
                docs/MONITORAMENTO.md e o conector Java. Nunca coloque a chave
                no JavaScript público do cliente.
              </p>
            </section>
          </div>
          <section className="panel">
            <div className="panel-title">
              <h2>Eventos de segurança</h2>
              <span>Últimos 100 relatos do período</span>
            </div>
            {!data.events.length ? (
              <p>
                Nenhum evento recebido. Instale o conector para receber relatos;
                o monitor externo não consegue observar seus logs internos.
              </p>
            ) : (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>QUANDO</th>
                      <th>EVENTO</th>
                      <th>ORIGEM</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.events.map((event) => (
                      <tr key={event.event_id}>
                        <td>{date(event.occurred_at)}</td>
                        <td>{eventNames[event.type] || event.type}</td>
                        <td>
                          <span
                            className={
                              "badge " + (event.simulated ? "MEDIUM" : "LOW")
                            }
                          >
                            {event.simulated
                              ? "Simulação explícita"
                              : "Reportado pelo conector"}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
          <section className="panel mon-assistant">
            <div>
              <span className="eyebrow">ASSISTENTE DO SEU SITE</span>
              <h2>Entenda o que foi observado.</h2>
              <p>O resumo usa somente dados registrados do domínio atual.</p>
            </div>
            <form
              onSubmit={async (e) => {
                e.preventDefault();
                const targetSite = id;
                const f = new FormData(e.currentTarget);
                setBusy(true);
                setError("");
                try {
                  const result = await api<{ answer: string }>(
                    `/sites/${id}/assistant`,
                    "POST",
                    { question: f.get("question") },
                  );
                  if (alive.current && currentSite.current === targetSite)
                    setAnswer(result.answer);
                } catch (x) {
                  setError((x as Error).message);
                } finally {
                  setBusy(false);
                }
              }}
            >
              <label>
                Sua pergunta
                <input
                  name="question"
                  required
                  maxLength={1500}
                  placeholder="Como está meu site? O que preciso configurar?"
                />
              </label>
              <button className="primary" disabled={busy}>
                Conversar sobre este site
              </button>
            </form>
            {answer && (
              <p className="mon-answer" role="status">
                {answer}
              </p>
            )}
          </section>
          <p className="mon-footnote">{data.notice}</p>
        </>
      )}
    </div>
  );
}
