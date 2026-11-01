"use client";
import { useState, useEffect } from "react";
import { api, date } from "../lib/api";
type Account = {
  id: string;
  name: string;
  email: string;
  role: string;
  enabled: boolean;
  mfa_enabled: boolean;
};
type Event = { id: string; action: string; detail: string; created_at: number };
type Subscription = {
  id: string;
  name: string;
  email: string;
  enabled: boolean;
  subscription_status: string;
  subscription_due: number | null;
};
type ManagedSite = {
  id: string;
  name: string;
  origin: string;
  enabled: boolean;
  verified_at: number | null;
  owner_name: string;
  owner_email: string;
};
export default function Admin() {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]),
    [sites, setSites] = useState<ManagedSite[]>([]),
    [saving, setSaving] = useState("");
  const [users, setUsers] = useState<Account[]>([]),
    [events, setEvents] = useState<Event[]>([]),
    [error, setError] = useState(""),
    [page, setPage] = useState(0);
  useEffect(() => {
    Promise.all([
      api<Account[]>("/admin/users"),
      api<Event[]>("/admin/audit?page=" + page),
      api<Subscription[]>("/admin/subscriptions"),
      api<ManagedSite[]>("/admin/sites"),
    ])
      .then(([u, e, subscriptions, sites]) => {
        setUsers(u);
        setEvents(e);
        setSubscriptions(subscriptions);
        setSites(sites);
      })
      .catch((x) => setError(x.message));
  }, [page]);
  return (
    <>
      <div className="section-heading">
        <div>
          <span className="eyebrow">GOVERNANÇA E RASTREABILIDADE</span>
          <h1>Administração.</h1>
          <p>Contas e eventos de segurança da plataforma.</p>
        </div>
      </div>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      <div className="mon-admin-head">
        <h2>Central administrativa</h2>
        <p>
          Clientes, assinaturas e domínios da operação. O cliente acompanha
          apenas os próprios sites em sua área.
        </p>
      </div>
      <section className="panel mon-space">
        <div className="panel-title">
          <h3>Assinaturas e acesso dos clientes</h3>
          <span>Controle manual · sem cobrança automática</span>
        </div>
        <p>
          Inadimplente ou suspenso encerra sessões e pausa novas coletas,
          preservando o histórico. O vencimento é informativo: a situação é
          definida por você.
        </p>
        <div className="mon-admin-list">
          {subscriptions.length === 0 ? (
            <p>Nenhum cliente cadastrado.</p>
          ) : (
            subscriptions.map((client) => (
              <form
                className="mon-admin-row"
                key={
                  client.id +
                  client.subscription_status +
                  client.subscription_due
                }
                onSubmit={async (e) => {
                  e.preventDefault();
                  const f = new FormData(e.currentTarget);
                  setSaving(client.id);
                  setError("");
                  try {
                    await api(`/admin/subscriptions/${client.id}`, "PATCH", {
                      status: f.get("status"),
                      dueAt: f.get("due")
                        ? new Date(String(f.get("due")) + "T12:00:00").getTime()
                        : null,
                    });
                    setSubscriptions(
                      await api<Subscription[]>("/admin/subscriptions"),
                    );
                    setUsers(await api<Account[]>("/admin/users"));
                    setEvents(await api<Event[]>("/admin/audit?page=" + page));
                  } catch (x) {
                    setError((x as Error).message);
                  } finally {
                    setSaving("");
                  }
                }}
              >
                <div>
                  <b>{client.name}</b>
                  <p>{client.email}</p>
                  <small>
                    {client.enabled ? "Acesso habilitado" : "Acesso suspenso"}
                  </small>
                </div>
                <div className="mon-billing">
                  <label>
                    Situação
                    <select
                      name="status"
                      defaultValue={client.subscription_status}
                    >
                      <option value="ACTIVE">Ativo</option>
                      <option value="TRIAL">Avaliação</option>
                      <option value="PAST_DUE">Inadimplente</option>
                      <option value="SUSPENDED">Suspenso</option>
                    </select>
                  </label>
                  <label>
                    Vencimento
                    <input
                      name="due"
                      type="date"
                      defaultValue={
                        client.subscription_due
                          ? new Date(client.subscription_due)
                              .toISOString()
                              .slice(0, 10)
                          : ""
                      }
                    />
                  </label>
                  <button className="secondary" disabled={saving === client.id}>
                    {saving === client.id ? "Salvando…" : "Salvar"}
                  </button>
                </div>
              </form>
            ))
          )}
        </div>
      </section>
      <section className="panel mon-space">
        <div className="panel-title">
          <h3>Sites cadastrados na plataforma</h3>
          <span>{sites.length} registros recentes</span>
        </div>
        {!sites.length ? (
          <p>Nenhum site cadastrado.</p>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>CLIENTE</th>
                  <th>SITE</th>
                  <th>CONFIRMAÇÃO</th>
                  <th>STATUS</th>
                </tr>
              </thead>
              <tbody>
                {sites.map((site) => (
                  <tr key={site.id}>
                    <td>
                      {site.owner_name}
                      <br />
                      <small>{site.owner_email}</small>
                    </td>
                    <td>
                      {site.name}
                      <br />
                      <small>{site.origin}</small>
                    </td>
                    <td>
                      {site.verified_at ? "Domínio confirmado" : "Pendente"}
                    </td>
                    <td>
                      {site.enabled
                        ? "Habilitado (depende da conta)"
                        : "Pausado"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <section className="panel">
        <div className="panel-title">
          <h3>Contas cadastradas</h3>
          <span>Até 200 contas recentes</span>
        </div>
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>NOME</th>
                <th>EMAIL</th>
                <th>PERFIL</th>
                <th>MFA</th>
                <th>STATUS</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.name}</td>
                  <td>{u.email}</td>
                  <td>{u.role}</td>
                  <td>{u.mfa_enabled ? "Ativo" : "Inativo"}</td>
                  <td>
                    {u.role === "ADMIN" ? (
                      "Administrador"
                    ) : (
                      <button
                        className="text-button"
                        onClick={async () => {
                          try {
                            await api("/admin/users/" + u.id, "PATCH", {
                              enabled: !u.enabled,
                            });
                            setUsers(await api<Account[]>("/admin/users"));
                            setSubscriptions(await api<Subscription[]>("/admin/subscriptions"));
                          } catch (x) {
                            setError((x as Error).message);
                          }
                        }}
                      >
                        {u.enabled ? "Desativar conta" : "Reativar conta"}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <section className="panel audit">
        <div className="panel-title">
          <h3>Trilha de auditoria</h3>
          <span>Eventos do servidor</span>
        </div>
        {events.map((e) => (
          <div className="audit-row" key={e.id}>
            <span>{date(e.created_at)}</span>
            <b>{e.action}</b>
            <p>{e.detail}</p>
          </div>
        ))}
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(page - 1)}>
            Anterior
          </button>
          <button
            disabled={events.length < 50}
            onClick={() => setPage(page + 1)}
          >
            Próxima
          </button>
        </div>
      </section>
    </>
  );
}
