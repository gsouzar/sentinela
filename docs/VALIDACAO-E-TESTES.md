# Validação, testes e roteiro de aceitação

Atualização: 04/11/2026. Resultados desta operação ficam em ESTADO-ATUAL.md. Evidência antiga em `docs/evidencias/` tem a data da execução original; não deve ser apresentada como teste repetido hoje.

## Comandos reproduzíveis

Na raiz do repositório, com ferramentas instaladas e sem variáveis de produção no terminal:

```powershell
cd services/api
.\mvnw.cmd test
```

Padrão dos testes de integração: H2 em memória, chaves sintéticas e transporte controlado. Para ensaiar compatibilidade PostgreSQL, criar banco exclusivo vazio, definir TEST_DATABASE_URL JDBC e credenciais do ambiente de testes. **A suíte insere/altera/remove dados; nunca apontar para banco de produção ou cópia contendo contas reais.**

```powershell
cd apps/web
npm ci
npm test
npm run build
```

```powershell
python -m pip install -r notebooks/requirements.txt
python -m unittest discover -s notebooks/tests
```

```powershell
cd apps/android
.\gradlew.bat assembleDebug lintDebug
```

Em máquina com cache Maven fora da área permitida, usar `-Dmaven.repo.local=CAMINHO_GRAVAVEL`. Isso muda o cache, não o código. Compilação e lint Android não comprovam navegação real, câmera/QR ou funcionamento do Keystore em um aparelho.

## Resultado automatizado em 04/11/2026

- Java/H2: **38 testes**, zero falhas e erros. Primeiro comando parou por cache Maven sem permissão; repetição com cache gravável passou.
- Web: **4 testes**, zero falhas.
- Web: build Next.js/TypeScript de produção concluído, rotas geradas.
- Python: **23 testes**, zero falhas; integrações Groq/Telegram são simuladas nos testes.
- Android: assembleDebug e lintDebug repetidos nesta operação, BUILD SUCCESSFUL. Não houve teste em aparelho nesta etapa.
- Ensaio de migração: restore transacional das 12 tabelas e comparação integral bem-sucedida. Houve diferença inicial apenas na ordem de linhas por locale; comparação refeita com COLLATE C conferiu todos os conteúdos. Isso não exigiu alterar dados.

## Aceitação funcional manual

| Jornada | Como testar | Resultado esperado |
|---|---|---|
| Cadastro USER | email de teste próprio e senha >=12 | conta comum, sem painel/role ADMIN |
| Login | senha certa, errada e sessão expirada | acesso controlado; mensagem sem stack trace |
| MFA USER | configurar QR, confirmar código, sair/entrar | exige senha + código após ativação |
| MFA ADMIN | entrar sem MFA configurado | encaminha à segurança, impede recursos funcionais até ativar |
| MFA replay | reutilizar código já consumido | recusa; próximo intervalo permite novo código |
| Senha | mudar com provas e abrir sessão antiga | sessões antigas revogadas |
| Isolamento | duas contas e análise/site de cada uma | conta B não lê/edita recursos de A |
| Análise | URL simples e URL sintética identificada | score/evidências explicados, cobertura explícita |
| Histórico | filtrar, abrir detalhe, excluir dado de teste | filtros corretos e exclusão autorizada |
| Fonte ausente | sem chave reputação | NOT_CONFIGURED/PARTIAL, nunca “comprovadamente seguro” |
| Chat local | pergunta sobre análise própria | resposta determinística, sem nova pontuação inventada |
| Groq | Colab e modelo válidos | diagnóstico GROQ/OK e COLAB no fluxo Java |
| Fallback | interromper runtime/túnel | análise continua e explicação local disponível |
| Monitor | domínio próprio com TXT | confirmação e coleta real; aguardar amostras |
| Domínio alterado | editar para outro domínio próprio | revisão nova, prova e chave antigas revogadas |
| Eventos | conector com simulação explícita | evento simulado separado, duplicata ignorada |
| Assinatura | suspender usuário de teste no admin | revoga acesso/coleta; histórico permanece |
| Telegram | /start, /analisar e pergunta | responde só conversa privada/ID autorizado |
| Android | login, análise, histórico, MFA e reabrir app | base correta, sessão/refresh conforme política |

Usar contas e domínios autorizados. Não simular ataques reais em site alheio; eventos de demonstração devem manter simulated=true. Testes de recuperação e exclusão só com dados de teste que podem ser perdidos.

## Aceitação do deploy e migração

1. Página principal retorna 200.
2. `/api/v1/health` responde UP pela URL web e pela API.
3. `/me` e `/admin/users` sem sessão retornam 401; mutação web sem CSRF retorna 403.
4. Entrada inválida com CSRF válido retorna 400 sem inserir conta.
5. Cookie e cabeçalhos esperados estão presentes.
6. Hikari/Flyway conectam ao host/banco alvo e validam V1/V2.
7. Antes de reabrir API, esquema, contagem e conteúdo de cada tabela coincidem com a origem parada.
8. Depois de reabrir, uma operação autenticada de leitura/gravação confirma a conexão com o destino. Conferir login existente/2FA com o responsável e registrar o resultado real.
9. Acesso externo temporário à origem foi removido; backup protegido existe e não foi enviado ao Git.

Health UP, deploy verde e contagem de usuários são evidências diferentes; nenhum deles isoladamente prova toda a jornada. Não declarar teste físico de Google Authenticator, Telegram ou Android sem executá-lo.
