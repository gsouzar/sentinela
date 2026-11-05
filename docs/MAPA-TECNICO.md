# Mapa técnico: arquivos, API, dados e configuração

Referência da implementação revisada em 04/11/2026. Prefixo de todos os endpoints abaixo: **`/api/v1`**. Controllers Java são a fonte do contrato; `openapi.json` é auxiliar e pode não enumerar todas as evoluções. Não gerar SDK assumindo que o OpenAPI antigo é exaustivo.

## Estrutura do repositório

| Caminho | Responsabilidade |
|---|---|
| `apps/web/app/` | entrada App Router, layout e estilos globais |
| `apps/web/components/Workspace.tsx` | sessão, navegação e painel principal |
| `Auth.tsx`, `Security.tsx` | cadastro/login, QR TOTP, senha e sessões |
| `Sites.tsx`, `Admin.tsx` | painel do cliente e central administrativa |
| `AnalysisResult.tsx`, `Chat.tsx` | apresentação da análise e conversa |
| `apps/web/lib/api.ts` | cliente HTTP, tipos, CSRF e evento de expiração |
| `apps/web/next.config.ts` | rewrite da API e cabeçalhos web |
| `services/api/src/main/java/br/edu/sentinela/` | backend Java |
| `services/api/src/main/resources/db/migration/` | esquema Flyway V1/V2 |
| `services/api/src/test/` | testes unitários e de integração MockMvc |
| `services/api/start-render.sh` | valida ambiente, monta JDBC e inicia jar |
| `Dockerfile.render` | build Maven/Java 21 e runtime Render |
| `apps/android/` | projeto Gradle importável no Android Studio |
| `apps/android/app/src/main/java/br/edu/sentinela/mobile/` | MainActivity, ApiClient e SecureTokens |
| `notebooks/Sentinela_Chatbot_Colab.ipynb` | notebook autocontido de explicação local/Groq e túnel |
| `notebooks/Sentinela_Telegram_Colab.ipynb` | notebook de polling, importa arquivo Python enviado |
| `notebooks/sentinela_chat.py` | explicador, validação e servidor Flask |
| `notebooks/sentinela_telegram.py` | bot usado pelo notebook/local; fonte atual do bot |
| `telegram/telegram_bot.py` | implementação legada independente, com contrato/configuração antigos; não usar no fluxo atual. Usar exclusivamente `notebooks/sentinela_telegram.py` |
| `connectors/` | exemplos Java/Python para backend do cliente |
| `scripts/` | avaliação de dataset e SQL de manutenção manual |
| `docs/` | documentação, contratos e evidências datadas |
| `compose.yaml`, `.env.example` | ambiente Docker local e nomes de configurações |
| `render.yaml` | modelo histórico de nova instalação Render com PostgreSQL Render; não representa automaticamente a conexão Neon |
| `1-INICIAR-API.cmd`, `2-INICIAR-WEB.cmd` | inicializadores Windows |

## Classes Java

| Classe | Papel |
|---|---|
| `SentinelaApplication` | entrada Spring Boot e agendamento |
| `SecurityConfig` | Argon2id, CSRF, CORS, autorização por role e filtros |
| `SessionFilter` | resolve cookie/Bearer, valida sessão, conta e política de MFA |
| `BodyLimitFilter` | limita corpos de requisição |
| `AuthController` | cadastro, login, refresh, sessões, senha e MFA |
| `Crypto` | tokens, hash, TOTP, proteção AES-GCM e códigos de recuperação |
| `Bootstrap` | criação inicial de ADMIN, sem promoção de conta existente |
| `Store` | JDBC, mapeamento de linhas, JSON, auditoria e rate limiting persistido |
| `UrlAnalyzer` | normalização de URL, regras e pontuação versionadas |
| `ReputationService` | fontes fixas, cache e estados de cobertura |
| `AnalysisController` | análises/histórico/dashboard/admin de acesso/auditoria |
| `ChatController` | explicação local, contexto autorizado e conexão Colab opcional |
| `SiteController` | propriedade/site/revisões, dashboards, chave de ingestão e assinatura |
| `SiteMonitor` | fila, leases e coleta agendada; persistência protegida por revisão |
| `PublicSiteProbe` | confirmação TXT, validação de destino, HEAD e TLS |
| `TelegramController` | autenticação técnica e isolamento das identidades BOT |
| `ApiError`, `Errors` | mensagens HTTP controladas sem stack trace ao cliente |

## Endpoints

| Método e caminho | Função / acesso |
|---|---|
| GET `/health` | público; retorna UP e serviço, **não consulta banco** |
| GET `/csrf` | público; emite token CSRF para web |
| POST `/auth/register` | cria USER; corpo name/email/password |
| POST `/auth/login` | email/password/code opcional; code exigido com MFA ativo |
| POST `/auth/refresh` | refresh Android rotativo |
| POST `/auth/logout` | encerra sessão atual |
| GET `/me` | usuário autenticado e política MFA |
| GET `/me/sessions` | sessões da própria conta |
| DELETE `/me/sessions/{id}` | revoga sessão da própria conta |
| POST `/me/sessions/revoke-all` | revoga acessos da conta |
| POST `/me/password` | troca senha com provas exigidas |
| POST `/me/mfa/setup` | gera segredo/otpauth após prova de senha |
| POST `/me/mfa/enable` | confirma TOTP e ativa segundo fator |
| POST `/me/mfa/disable` | desativa conforme política e prova; ADMIN não pode contornar exigência |
| POST `/analyses` | analisa e persiste URL da conta; corpo `{ "url": "https://example.org" }` |
| GET `/analyses` | lista/pagina/filtra histórico autorizado |
| GET `/analyses/{id}` | resultado autorizado |
| DELETE `/analyses/{id}` | remove análise e conversas vinculadas |
| GET `/dashboard` | agregados de análises da conta conforme autorização |
| POST `/chat` | pergunta e analysisId opcional, contexto autorizado |
| GET `/chat` | histórico próprio de conversas |
| DELETE `/chat` | apaga histórico próprio de conversas |
| GET, POST `/sites` | lista próprios / cadastra nome e URL |
| PATCH `/sites/{id}` | edita nome/origem; troca de origem cria revisão |
| PATCH `/sites/{id}/status` | pausa/retoma com enabled |
| POST `/sites/{id}/verify` | valida prova DNS |
| POST `/sites/{id}/check` | enfileira verificação, exige propriedade/atividade |
| GET `/sites/{id}/dashboard` | métricas, amostras e eventos da revisão atual |
| POST `/sites/{id}/integration-key` | gera/rotaciona chave de eventos, exige prova da conta |
| POST `/sites/{id}/assistant` | resumo determinístico do site |
| POST `/ingest/events` | autenticação por X-Sentinela-Site-Key; sem sessão humana |
| GET `/admin/users` | lista usuários, só ADMIN com MFA satisfeito |
| PATCH `/admin/users/{id}` | altera enabled, bloqueia/revoga sessões conforme regras |
| GET `/admin/audit` | auditoria administrativa |
| GET `/admin/sites` | sites/clientes para administração |
| GET `/admin/subscriptions` | situações de assinatura de USER |
| PATCH `/admin/subscriptions/{id}` | status/dueAt manuais |
| POST `/integrations/telegram/analyze` | análise da identidade técnica da conversa |
| POST `/integrations/telegram/chat` | pergunta vinculada ao contexto autorizado do bot |
| POST `/integrations/telegram/erase` | remove dados da identidade técnica correspondente |

Cadastro aceita nome até 80 caracteres, email até 254, senha de 12–128. URL de análise até 2048; pergunta até 1500. Requisições JSON com campos desconhecidos são recusadas. Consulte os records dos controllers antes de montar payloads novos. Não registrar corpos de login/2FA em logs.

Status importantes: 400 entrada inválida, 401 sessão/credenciais ausentes ou inválidas, 403 autorização/CSRF, 404 recurso não visível/inexistente, 409 conflito/estado, 428 segundo fator ou matrícula MFA necessária, 429 limite, 503 integração indisponível. Uma negativa não deve ser corrigida removendo autorização.

## Banco de dados

| Tabela | Dados e relacionamento |
|---|---|
| `app_users` | id UUID textual, nome/email único, password_hash, role, enabled, MFA cifrado/estado/contador, assinatura e criação |
| `sessions` | usuário FK CASCADE, hash de acesso/refresh e expiração |
| `consumed_refresh` | hash já consumido e usuário; detecta reutilização |
| `recovery_codes` | PK usuário + hash; códigos de recuperação de uso único |
| `analyses` | usuário, domínio/URL reduzida, score/risco, snapshot JSON e data |
| `chat_messages` | usuário, análise opcional, pergunta/resposta/modo e data |
| `audit_events` | ator opcional, ação, detalhe limitado e data; não editar pelas APIs comuns |
| `rate_buckets` | chave do limite, hits e janela |
| `monitored_sites` | usuário, origem/domínio, revisão, prova DNS, atividade, hash da chave e leases |
| `site_checks` | site/revisão, momento, status, tempo, HTTP, vencimento TLS e JSON |
| `site_events` | site/revisão, UUID único por site, tipo, simulação e horários |
| `flyway_schema_history` | controle de versões/checksums de esquema, também deve acompanhar backup |

V1 cria contas/autenticação/análises/chat/auditoria/limites. V2 acrescenta assinatura e monitoramento. Índices cobrem proprietário/data, fila de sites e histórico por site/revisão. FKs para usuário/site usam exclusão em cascata onde definido no SQL. `chat_messages.analysis_id` não deve ser tratado como FK automática: a remoção vinculada ocorre na lógica da API.

## Variáveis por componente

| Variável | Onde / significado |
|---|---|
| `PORT` | API; padrão 8080 |
| `DATABASE_URL` | API; **JDBC** `jdbc:postgresql://HOST:5432/DB?sslmode=require` |
| `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME` | start-render.sh monta JDBC somente se DATABASE_URL estiver ausente |
| `DATABASE_USER`, `DATABASE_PASSWORD` | API; credenciais separadas, exigidas pelo inicializador Render |
| `ENCRYPTION_KEY` | API; segredo persistente >=32 caracteres para MFA; preservar exatamente |
| `WEB_ORIGIN` | API; origem pública exata, sem barra final/caminho |
| `SECURE_COOKIES` | API; true em HTTPS, false somente HTTP local |
| `REQUIRE_MFA` | API; false deixa USER opcional; ADMIN continua obrigatório |
| `DEMO_MODE` | API; base sintética identificada, false em produção |
| `MONITORING_ENABLED` | API; true padrão, false para impedir coleta no ambiente de testes |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | API; limite de conexões do pool, instalação pequena usa 5 |
| `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE` | API; mínimo ocioso; avaliar 0 para permitir suspensão do compute Neon |
| `GOOGLE_SAFE_BROWSING_KEY`, `URLHAUS_KEY` | API; chaves opcionais das fontes de reputação |
| `COLAB_CHAT_URL` | API; HTTPS do túnel terminando `/explain` |
| `COLAB_CHAT_KEY` | API e explicador Colab; mesmo segredo próprio, diferente de Groq e MFA |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | bootstrap temporário; remover ambas após criação |
| `API_INTERNAL_URL` | Next/Vercel; origem da API, sem `/api/v1`; requer rebuild quando muda |
| `STANDALONE` | web; true seleciona saída standalone no build Docker |
| `GROQ_API_KEY`, `GROQ_MODEL` | Secrets do explicador Colab; chave e ID exato de modelo disponível |
| `NGROK_AUTHTOKEN` | Secrets Colab; autentica túnel na conta ngrok |
| `TELEGRAM_API_KEY` | API e bot Python; segredo exclusivo igual nos dois lados |
| `TELEGRAM_BOT_TOKEN` | somente bot Python; token BotFather |
| `TELEGRAM_ALLOWED_USER_IDS` | bot; IDs numéricos humanos permitidos, separados por vírgula |
| `SENTINELA_API_BASE` | bot; HTTPS da API incluindo `/api/v1` |
| `SENTINELA_SITE_KEY` | backend do cliente; chave exclusiva de ingestão, nunca frontend/mobile |
| `SENTINELA_EVENT_URL` | conector do cliente; endpoint `/api/v1/ingest/events` |
| `POSTGRES_PASSWORD` | Compose local; senha do banco local, não o segredo MFA |
| `apiBase` | propriedade Gradle `-PapiBase=.../api/v1`, embutida no APK no build |
| `TEST_DATABASE_URL` | testes Java; banco isolado, nunca produção |

`.env.example` é catálogo para Compose e referência. Spring/Python não leem arbitrariamente esse arquivo por conta própria. Definir variáveis no processo correto; no Colab usar Secrets e permitir acesso ao notebook. Não versionar valores reais.
