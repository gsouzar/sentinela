"use client";
import { useState, useEffect, useCallback, useRef } from "react";
import {
  ShieldCheck,
  LayoutDashboard,
  ScanLine,
  History,
  MessageSquare,
  Settings,
  LogOut,
  ChevronRight,
  ArrowUpRight,
  Link2,
  Search,
  ArrowRight,
  Activity,
  ShieldAlert,
  Menu,
  X,
  Users,
  RefreshCw,
  Globe,
  Info,
} from "lucide-react";
import Auth from "./Auth";
import AnalysisResult from "./AnalysisResult";
import Chat from "./Chat";
import Security from "./Security";
import Admin from "./Admin";
import Sites from "./Sites";
import {
  api,
  analysisQuery,
  User,
  Analysis,
  Row,
  Dashboard,
  riskLabel,
  date,
} from "../lib/api";
type Page =
  | "sites"
  | "overview"
  | "analyze"
  | "history"
  | "chat"
  | "security"
  | "admin"
  | "result";
const nav = [
  { id: "sites", label: "Meus sites", icon: Globe },
  { id: "overview", label: "Visão geral", icon: LayoutDashboard },
  { id: "analyze", label: "Analisar link", icon: ScanLine },
  { id: "history", label: "Histórico", icon: History },
  { id: "chat", label: "Assistente", icon: MessageSquare },
  { id: "security", label: "Minha segurança", icon: Settings },
] as const;
const titles: Record<Page, string> = {
  sites: "Painel do cliente",
  overview: "Visão geral",
  analyze: "Analisar link",
  history: "Histórico de análises",
  chat: "Assistente Sentinela",
  security: "Minha segurança",
  admin: "Administração",
  result: "Detalhes da análise",
};
export default function Workspace() {
  const loadId = useRef(0);
  const [user, setUser] = useState<User | null>(null),
    [boot, setBoot] = useState(true),
    [page, setPage] = useState<Page>("sites"),
    [mobile, setMobile] = useState(false),
    [dashboard, setDashboard] = useState<Dashboard | null>(null),
    [rows, setRows] = useState<Row[]>([]),
    [total, setTotal] = useState(0),
    [selected, setSelected] = useState<Analysis | null>(null),
    [url, setUrl] = useState(""),
    [busy, setBusy] = useState(false),
    [loading, setLoading] = useState(false),
    [error, setError] = useState(""),
    [risk, setRisk] = useState(""),
    [query, setQuery] = useState(""),
    [search, setSearch] = useState(""),
    [index, setIndex] = useState(0),
    [days, setDays] = useState("30");
  const resetSession = useCallback(() => {
    loadId.current++;
    setUser(null);
    setSelected(null);
    setRows([]);
    setDashboard(null);
    setTotal(0);
    setUrl("");
    setBusy(false);
    setLoading(false);
    setError("");
    setRisk("");
    setQuery("");
    setSearch("");
    setIndex(0);
    setDays("30");
    setMobile(false);
    setPage("sites");
  }, []);
  useEffect(() => {
    window.addEventListener("sentinela:unauthorized", resetSession);
    return () =>
      window.removeEventListener("sentinela:unauthorized", resetSession);
  }, [resetSession]);
  useEffect(() => {
    api<User>("/me")
      .then((u) => {
        setUser(u);
        setPage(
          u.mfa_required && !u.mfa_enabled
            ? "security"
            : u.role === "ADMIN"
              ? "admin"
              : "sites",
        );
      })
      .catch(() => {})
      .finally(() => setBoot(false));
  }, []);
  const refresh = useCallback(async () => {
    if (!user || (user.mfa_required && !user.mfa_enabled)) return;
    const requestId = ++loadId.current;
    setLoading(true);
    try {
      const [d, h] = await Promise.all([
        api<Dashboard>("/dashboard?days=7"),
        api<{ items: Row[]; total: number }>(
          analysisQuery(page, { page: index, risk, search, days }),
        ),
      ]);
      if (requestId !== loadId.current) return;
      setDashboard(d);
      setRows(h.items);
      setTotal(h.total);
    } catch (x) {
      if (requestId === loadId.current) setError((x as Error).message);
    } finally {
      if (requestId === loadId.current) setLoading(false);
    }
  }, [user, index, risk, search, days, page]);
  useEffect(() => {
    void refresh();
  }, [refresh]);
  function go(p: Page) {
    if (user?.mfa_required && !user.mfa_enabled) p = "security";
    if (p !== page) loadId.current++;
    setPage(p);
    setError("");
    setMobile(false);
    window.scrollTo({ top: 0, behavior: "instant" });
  }
  async function analyze(e: React.FormEvent) {
    e.preventDefault();
    const requestId = loadId.current;
    setBusy(true);
    setError("");
    try {
      const result = await api<Analysis>("/analyses", "POST", { url });
      if (requestId !== loadId.current) return;
      setSelected(result);
      go("result");
      void refresh();
    } catch (x) {
      if (requestId === loadId.current) setError((x as Error).message);
    } finally {
      if (requestId === loadId.current) setBusy(false);
    }
  }
  async function open(id: string) {
    const requestId = loadId.current;
    setBusy(true);
    try {
      const result = await api<Analysis>("/analyses/" + id);
      if (requestId !== loadId.current) return;
      setSelected(result);
      go("result");
    } catch (x) {
      if (requestId === loadId.current) setError((x as Error).message);
    } finally {
      if (requestId === loadId.current) setBusy(false);
    }
  }
  async function logout() {
    try {
      await api("/auth/logout", "POST");
      resetSession();
    } catch (x) {
      setError((x as Error).message);
    }
  }
  if (boot)
    return (
      <main className="boot">
        <span className="brand-symbol boot-symbol" aria-hidden="true" />
        <p>Preparando seu espaço…</p>
      </main>
    );
  if (!user)
    return (
      <Auth
        onLogin={(u) => {
          resetSession();
          setUser(u);
          setPage(
            u.mfa_required && !u.mfa_enabled
              ? "security"
              : u.role === "ADMIN"
                ? "admin"
                : "sites",
          );
        }}
      />
    );
  const scanner = (
    <form className="scanner" onSubmit={analyze}>
      <label htmlFor="url-field">Qual link você quer verificar?</label>
      <div className="scan-input">
        <Link2 size={20} />
        <input
          id="url-field"
          type="url"
          required
          maxLength={2048}
          placeholder="Cole uma URL aqui. Ex.: https://exemplo.com"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          autoComplete="off"
        />
        <button className="primary" disabled={busy}>
          {busy ? "Analisando…" : "Analisar link"}
          <ArrowUpRight size={18} />
        </button>
      </div>
      <div className="scan-help">
        <span>
          <ShieldCheck size={14} /> O endereço não será aberto.
        </span>
        <span>HTTP / HTTPS · Até 2048 caracteres</span>
      </div>
    </form>
  );
  function table() {
    return (
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>ENDEREÇO</th>
              <th>NÍVEL DE RISCO</th>
              <th>SCORE</th>
              <th>ANALISADO EM</th>
              <th>
                <span className="sr-only">Ações</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {rows.slice(0, page === "overview" ? 5 : 10).map((row) => (
              <tr key={row.id}>
                <td>
                  <div className="table-domain">
                    <span className="globe">
                      <Globe size={17} />
                    </span>
                    <div>
                      <b>{row.domain}</b>
                      <small>{row.url}</small>
                    </div>
                  </div>
                </td>
                <td>
                  <span className={"badge " + row.risk}>
                    <i />
                    {riskLabel[row.risk]}
                  </span>
                </td>
                <td>
                  <span className={"table-score " + row.risk}>
                    {row.score}
                    <small>/100</small>
                  </span>
                </td>
                <td className="table-date">{date(row.created_at)}</td>
                <td>
                  <button
                    className="icon-button"
                    aria-label={"Ver análise de " + row.domain}
                    disabled={busy}
                    onClick={() => void open(row.id)}
                  >
                    <ArrowUpRight size={18} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }
  return (
    <div className="app-shell">
      <a className="skip" href="#main-content">
        Pular para o conteúdo
      </a>
      {mobile && (
        <button
          className="sidebar-overlay"
          aria-label="Fechar navegação"
          onClick={() => setMobile(false)}
        />
      )}
      <aside className={"sidebar " + (mobile ? "open" : "")}>
        <div className="brand">
          <span className="brand-mark brand-logo" aria-hidden="true" />
          sentinela<span className="brand-dot">.</span>
        </div>
        <span className="sidebar-caption">SENTINELA · ÁREA RESTRITA</span>
        <nav aria-label="Navegação principal">
          {nav
            .filter((n) => user.role !== "ADMIN" || n.id === "security")
            .map((n) => (
              <button
                key={n.id}
                aria-current={page === n.id ? "page" : undefined}
                className={page === n.id ? "active" : ""}
                onClick={() => go(n.id)}
              >
                <n.icon size={20} />
                {n.label}
                {page === n.id && <ChevronRight size={15} />}
              </button>
            ))}
          {user.role === "ADMIN" && (
            <button
              className={page === "admin" ? "active" : ""}
              onClick={() => go("admin")}
            >
              <Users size={20} /> Administração
            </button>
          )}
        </nav>
        <div className="sidebar-bottom">
          <div className="side-tip">
            <span className="tip-symbol">
              <ShieldCheck size={21} />
            </span>
            <h4>Um hábito que protege.</h4>
            <p>Na dúvida, confirme pelo canal oficial antes de clicar.</p>
            <button onClick={() => go("chat")}>
              Tirar uma dúvida <ArrowUpRight size={16} />
            </button>
          </div>
          <div className="side-user">
            <span className="avatar">{user.name.charAt(0).toUpperCase()}</span>
            <div>
              <strong>{user.name}</strong>
              <small>
                {user.role === "ADMIN" ? "Administrador" : "Área do cliente"}
              </small>
            </div>
            <button aria-label="Sair da conta" onClick={() => void logout()}>
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar">
          <div>
            <button
              className="mobile-menu icon-button"
              aria-label="Abrir navegação"
              onClick={() => setMobile(true)}
            >
              <Menu />
            </button>
            <span>Seu espaço</span>
            <ChevronRight size={13} />
            <b>{titles[page]}</b>
          </div>
          <span className="environment">
            <i /> Ambiente acadêmico
          </span>
        </header>
        <main id="main-content" className="main-content">
          {error && (
            <div className="error global-error" role="alert">
              {error}
              <button aria-label="Fechar mensagem" onClick={() => setError("")}>
                <X size={17} />
              </button>
            </div>
          )}
          {page === "sites" && <Sites />}
          {(page === "overview" || page === "analyze") && (
            <>
              <section className="greeting">
                <div>
                  <span className="eyebrow">
                    SEGURANÇA DIGITAL, SEM COMPLICAÇÃO
                  </span>
                  <h1>
                    {page === "overview"
                      ? `Olá, ${user.name.split(" ")[0]}.`
                      : "Antes de clicar, verifique."}
                  </h1>
                  <p>
                    {page === "overview"
                      ? "Mais contexto para cada link. Mais confiança para você."
                      : "Uma avaliação transparente, com os sinais que você precisa entender."}
                  </p>
                </div>
                <span className="greeting-date">
                  {new Intl.DateTimeFormat("pt-BR", {
                    day: "2-digit",
                    month: "long",
                    year: "numeric",
                  }).format(new Date())}
                </span>
              </section>
              <section className="hero-panel">
                <div className="hero-copy">
                  <span className="hero-kicker">
                    <span /> OLHAR ATENTO. ESCOLHAS INFORMADAS.
                  </span>
                  <h2>
                    Um link. Uma decisão
                    <br />
                    <em>mais segura.</em>
                  </h2>
                  <p>
                    Encontrou um endereço suspeito? Analise os sinais de risco
                    antes de compartilhar seus dados.
                  </p>
                </div>
                <div className="radar-art" aria-hidden="true">
                  <div className="radar-ring r1" />
                  <div className="radar-ring r2" />
                  <div className="radar-ring r3" />
                  <div className="radar-cross" />
                  <div className="radar-center">
                    <span className="brand-symbol" aria-hidden="true" />
                  </div>
                  <span className="radar-label">
                    <span /> ANÁLISE EM CAMADAS
                  </span>
                  <i className="radar-point p1" />
                  <i className="radar-point p2" />
                </div>
                {scanner}
              </section>
              {page === "analyze" && (
                <section className="panel examples">
                  <div>
                    <span className="eyebrow">EXPLORE O PROCESSO</span>
                    <h3>Três exemplos para começar</h3>
                    <p>
                      Preencha um endereço e clique em Analisar. Domínios .test
                      são exemplos reservados.
                    </p>
                  </div>
                  {[
                    {
                      name: "Poucos indicadores",
                      url: "https://example.org",
                      type: "LOW",
                    },
                    {
                      name: "Estrutura suspeita",
                      url: "http://confirmar-login.test/urgente",
                      type: "MEDIUM",
                    },
                    {
                      name: "Imitação de marca",
                      url: "http://banc0-seguranca-login.test/confirmar",
                      type: "HIGH",
                    },
                  ].map((x) => (
                    <button
                      key={x.name}
                      onClick={() => {
                        setUrl(x.url);
                        document.getElementById("url-field")?.focus();
                      }}
                    >
                      <span className={"badge " + x.type}>{x.name}</span>
                      <code>{x.url}</code>
                      <ArrowUpRight size={17} />
                    </button>
                  ))}
                  <p className="muted">
                    A classificação será calculada pelo motor. O último exemplo
                    tem correspondência sintética somente quando DEMO_MODE está
                    ativo.
                  </p>
                </section>
              )}
            </>
          )}
          {page === "overview" && (
            <>
              <div className="section-subhead">
                <h3>Sua atividade em perspectiva</h3>
                <span>
                  Últimos 7 dias · {dashboard?.scope || "Minha conta"}
                </span>
              </div>
              <section className="metrics">
                {[
                  {
                    label: "Links analisados",
                    value: dashboard?.total ?? 0,
                    icon: ScanLine,
                    color: "blue",
                    hint: "Avaliações realizadas",
                  },
                  {
                    label: "Alto risco",
                    value: dashboard?.risks.HIGH ?? 0,
                    icon: ShieldAlert,
                    color: "red",
                    hint: "Exigem mais atenção",
                  },
                  {
                    label: "Risco médio",
                    value: dashboard?.risks.MEDIUM ?? 0,
                    icon: Activity,
                    color: "amber",
                    hint: "Sinais para conferir",
                  },
                  {
                    label: "Baixo risco",
                    value: dashboard?.risks.LOW ?? 0,
                    icon: ShieldCheck,
                    color: "green",
                    hint: "Sem garantia de segurança",
                  },
                ].map((m) => (
                  <article className="metric" key={m.label}>
                    <div>
                      <span>{m.label}</span>
                      <span className={"metric-icon " + m.color}>
                        <m.icon size={19} />
                      </span>
                    </div>
                    <strong>
                      {loading ? "—" : m.value.toLocaleString("pt-BR")}
                    </strong>
                    <small>{m.hint}</small>
                  </article>
                ))}
              </section>
              <div className="overview-grid">
                <section className="panel trend">
                  <div className="panel-title">
                    <div>
                      <h3>Atividade de análises</h3>
                      <span>Distribuição diária · UTC</span>
                    </div>
                    <span className="legend">
                      <i /> Análises <i className="red" /> Alto risco
                    </span>
                  </div>
                  <div
                    className="chart"
                    role="img"
                    aria-label={
                      "Total de " +
                      (dashboard?.total ?? 0) +
                      " análises nos últimos sete dias"
                    }
                  >
                    {dashboard?.trend.map((t) => (
                      <div className="chart-col" key={t.date}>
                        <span className="chart-count">{t.total}</span>
                        <div className="bar-zone">
                          <div
                            className="chart-bar"
                            style={{
                              height:
                                Math.max(
                                  2,
                                  (t.total /
                                    Math.max(
                                      1,
                                      ...dashboard.trend.map((x) => x.total),
                                    )) *
                                    105,
                                ) + "px",
                            }}
                          >
                            <i
                              style={{
                                height:
                                  (t.total ? (t.high / t.total) * 100 : 0) +
                                  "%",
                              }}
                            />
                          </div>
                        </div>
                        <small>
                          {new Intl.DateTimeFormat("pt-BR", {
                            weekday: "short",
                            timeZone: "UTC",
                          })
                            .format(new Date(t.date + "T12:00:00Z"))
                            .replace(".", "")}
                        </small>
                      </div>
                    ))}
                  </div>
                </section>
                <section className="insight">
                  <div className="insight-top">
                    <Info size={20} />
                    <span>FIQUE UM PASSO À FRENTE</span>
                  </div>
                  <h3>
                    HTTPS sozinho
                    <br />
                    não garante segurança.
                  </h3>
                  <p>
                    O cadeado protege a conexão. Um site fraudulento também pode
                    ter certificado.
                  </p>
                  <button onClick={() => go("chat")}>
                    Entenda os sinais <ArrowUpRight size={17} />
                  </button>
                </section>
              </div>
              <section className="panel recent">
                <div className="panel-title">
                  <div>
                    <h3>Análises recentes</h3>
                    <span>Os últimos links que passaram pelo Sentinela</span>
                  </div>
                  <button className="text-button" onClick={() => go("history")}>
                    Ver histórico <ArrowRight size={16} />
                  </button>
                </div>
                {loading ? (
                  <p className="loading" role="status">
                    Carregando análises…
                  </p>
                ) : rows.length ? (
                  table()
                ) : (
                  <div className="empty">
                    <ScanLine size={30} />
                    <h3>Seu primeiro olhar começa aqui.</h3>
                    <p>
                      Cole um link no campo acima para criar sua primeira
                      análise.
                    </p>
                  </div>
                )}
              </section>
            </>
          )}
          {page === "history" && (
            <>
              <div className="section-heading">
                <div>
                  <span className="eyebrow">CADA LINK, UMA EVIDÊNCIA</span>
                  <h1>Seu histórico de análises.</h1>
                  <p>
                    Encontre resultados, compare os sinais e retome uma
                    investigação.
                  </p>
                </div>
                <button className="primary" onClick={() => go("analyze")}>
                  <ScanLine size={17} /> Nova análise
                </button>
              </div>
              <section className="panel history-panel">
                <div className="filters">
                  <form
                    onSubmit={(e) => {
                      e.preventDefault();
                      setSearch(query);
                      setIndex(0);
                    }}
                    className="search-field"
                  >
                    <Search size={18} />
                    <input
                      aria-label="Buscar domínio"
                      value={query}
                      onChange={(e) => setQuery(e.target.value)}
                      placeholder="Buscar por domínio"
                      maxLength={100}
                    />
                    <button type="submit">Buscar</button>
                  </form>
                  <select
                    aria-label="Filtrar risco"
                    value={risk}
                    onChange={(e) => {
                      setRisk(e.target.value);
                      setIndex(0);
                    }}
                  >
                    <option value="">Todos os riscos</option>
                    <option value="HIGH">Alto risco</option>
                    <option value="MEDIUM">Risco médio</option>
                    <option value="LOW">Baixo risco</option>
                  </select>
                  <select
                    aria-label="Filtrar período"
                    value={days}
                    onChange={(e) => {
                      setDays(e.target.value);
                      setIndex(0);
                    }}
                  >
                    <option value="7">Últimos 7 dias</option>
                    <option value="30">Últimos 30 dias</option>
                    <option value="365">Último ano</option>
                  </select>
                  <button
                    className="icon-button"
                    aria-label="Atualizar histórico"
                    onClick={() => void refresh()}
                  >
                    <RefreshCw size={18} />
                  </button>
                </div>
                {loading ? (
                  <p className="loading" role="status">
                    Buscando resultados…
                  </p>
                ) : rows.length ? (
                  table()
                ) : (
                  <div className="empty">
                    <Search />
                    <h3>Nenhuma análise encontrada.</h3>
                    <p>Experimente outros filtros ou analise um novo link.</p>
                  </div>
                )}
                <div className="pagination">
                  <span>
                    {total} {total === 1 ? "resultado" : "resultados"} · Página{" "}
                    {index + 1}
                  </span>
                  <div>
                    <button
                      disabled={index === 0 || loading}
                      onClick={() => setIndex(index - 1)}
                    >
                      Anterior
                    </button>
                    <button
                      disabled={(index + 1) * 10 >= total || loading}
                      onClick={() => setIndex(index + 1)}
                    >
                      Próxima
                    </button>
                  </div>
                </div>
              </section>
            </>
          )}
          {page === "result" && selected && (
            <>
              <AnalysisResult
                value={selected}
                onChat={() => go("chat")}
                onBack={() => go("history")}
              />
              <button
                className="danger text-button delete-link"
                onClick={async () => {
                  if (
                    window.confirm(
                      "Excluir esta análise e as conversas vinculadas da sua conta?",
                    )
                  )
                    try {
                      await api("/analyses/" + selected.id, "DELETE");
                      setSelected(null);
                      go("history");
                      void refresh();
                    } catch (x) {
                      setError((x as Error).message);
                    }
                }}
              >
                Excluir análise da minha conta
              </button>
            </>
          )}
          {page === "chat" && (
            <Chat
              selected={selected}
              clearSelection={() => setSelected(null)}
            />
          )}
          {page === "security" && (
            <Security user={user} onUpdate={setUser} onLogout={resetSession} />
          )}
          {page === "admin" && user.role === "ADMIN" && <Admin />}
          <footer className="page-footer">
            <span>
              <ShieldCheck size={14} /> Sentinela · Segurança com informação
            </span>
            <span>
              Projeto acadêmico · Nenhuma análise garante segurança absoluta.
            </span>
          </footer>
        </main>
      </div>
    </div>
  );
}
