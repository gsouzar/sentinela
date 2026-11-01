import test from "node:test";
import assert from "node:assert/strict";
import { api } from "./api.ts";
test("401 informa expiração e sinaliza encerramento da sessão", async () => {
  const target = new EventTarget();
  globalThis.window = target;
  let expired = 0;
  target.addEventListener("sentinela:unauthorized", () => expired++);
  const old = globalThis.fetch;
  globalThis.fetch = async () =>
    new Response(JSON.stringify({ message: "Sessão expirada" }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    });
  try {
    await assert.rejects(
      () => api("/me"),
      (error) => error.status === 401,
    );
    assert.equal(expired, 1);
  } finally {
    globalThis.fetch = old;
    delete globalThis.window;
  }
});
test("mutação obtém token CSRF atualizado antes de enviar os dados", async () => {
  const old = globalThis.fetch;
  const calls = [];
  globalThis.fetch = async (url, options) => {
    calls.push([url, options]);
    return new Response(
      JSON.stringify(
        String(url).endsWith("/csrf") ? { token: "fresh-token" } : { ok: true },
      ),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  };
  try {
    await api("/analyses", "POST", { url: "https://example.org" });
    assert.equal(calls[0][0], "/api/v1/csrf");
    assert.equal(calls[1][1].headers["X-XSRF-TOKEN"], "fresh-token");
    assert.equal(calls[1][1].credentials, "include");
  } finally {
    globalThis.fetch = old;
  }
});
import * as apiModule from "./api.ts";
test("resumo usa histórico recente sem herdar filtros da tabela", () => {
  assert.equal(typeof apiModule.analysisQuery, "function");
  assert.equal(
    apiModule.analysisQuery("overview", {
      page: 4,
      risk: "HIGH",
      search: "x",
      days: "7",
    }),
    "/analyses?page=0&size=5&days=30",
  );
});
test("histórico preserva filtro e codifica consulta", () => {
  assert.equal(typeof apiModule.analysisQuery, "function");
  const value = apiModule.analysisQuery("history", {
    page: 1,
    risk: "HIGH",
    search: "banco & email",
    days: "7",
  });
  assert.equal(
    value,
    "/analyses?page=1&size=10&risk=HIGH&q=banco%20%26%20email&days=7",
  );
});
