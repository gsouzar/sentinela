# Painel do cliente: monitoramento externo e integração

Esta versão acrescenta **Meus sites** como entrada da conta do cliente e mantém **Analisar link** para avaliações pontuais. A administração tem entrada própria. O usuário comum pode configurar/desativar 2FA em Minha segurança; ADMIN precisa ativá-lo. `REQUIRE_MFA=true` permite tornar o fator obrigatório também para clientes, se desejado.

## 1. Cadastrar e abrir o painel

1. Inicie API e web pelos CMDs, entre com sua conta comum e abra **Meus sites**.
2. Clique **Adicionar site**. Informe um nome e a URL do seu site, como `https://seudominio.com.br`.
3. Clique **Salvar site**. O painel já abre; sem medições ele mostra **aguardando**, sem números inventados.
4. O monitor usa somente a origem: caminhos, query e fragmento são descartados. Aceita HTTP/HTTPS nas portas padrão. Domínios internos, IPs e `.test` não são destinos válidos para o monitor de produção.

## 2. Confirmar propriedade por DNS

1. Entre no painel do provedor que administra o DNS desse domínio.
2. Procure a área **DNS / Gerenciar zona / Registros** e clique em adicionar registro.
3. Escolha **TXT**. Copie os campos **Nome** e **Valor** exibidos no painel do Sentinela.
4. O nome completo é `_sentinela.seudominio.com.br`; se o provedor já acrescenta o domínio, informe somente `_sentinela`. O valor começa por `sentinela=` e contém o token exclusivo exibido.
5. Salve e aguarde a propagação. Volte ao Sentinela e clique **Já adicionei: confirmar domínio**.
6. Após a confirmação, clique **Verificar agora**, ou aguarde a coleta automática. O painel atualiza a cada 30 segundos; coletas recorrentes são programadas aproximadamente a cada cinco minutos, sujeitas à fila. A API deve permanecer ligada.

Mantenha o TXT no DNS: antes de cada consulta externa o servidor confere novamente a propriedade. Registro ausente ou incorreto suspende a confirmação; uma falha temporária de resolução apenas adia a coleta e tenta novamente. Não é necessário fornecer senha do provedor ao Sentinela. O TXT não é uma chave de acesso à API; pode ser público.

Sem domínio próprio ou permissão para alterar o DNS, você pode usar o analisador pontual de links, mas não deve afirmar que configurou o monitor de produção. Os testes automatizados usam banco e respostas controlados; não substituem essa confirmação real.

## 3. Ler o dashboard

| Informação | Origem e significado |
|---|---|
| Disponibilidade nas amostras | Percentual de verificações HTTP com resposta 2xx/3xx no período. Não é medição contínua nem prova de proteção. |
| Tempo da verificação | Tempo observado desde DNS/conexão/TLS até os cabeçalhos de resposta. Não é tempo isolado de processamento da aplicação. |
| Certificado HTTPS | Vencimento obtido em conexão validada; em HTTP ou falha TLS pode não haver medição. |
| Achados de configuração | Presença/ausência de cabeçalhos na resposta HEAD da raiz. Ausência não comprova vulnerabilidade explorável. |
| Eventos do conector | Relatos enviados pelo backend/WAF do cliente, com chave do site. Não são ataques confirmados pelo monitor externo. |
| Eventos simulados | Dados de teste explicitamente marcados; contados separadamente dos relatos reais. |

**Respondendo**: HTTP 2xx. **Redirecionamento**: HTTP 3xx observado, sem visitar o destino. **Acesso restrito**: HTTP 4xx, que pode refletir política do servidor. **Erro no servidor**: HTTP 5xx. **Não alcançado/Falha TLS**: a consulta não completou. **DNS indisponível**: não foi possível confirmar a propriedade naquele momento; não houve acesso ao destino. Queda e erro não são classificados automaticamente como ataque.

Os gráficos mostram até as 2.000 últimas amostras do período; o indicador de disponibilidade usa todas as amostras elegíveis do período. Falhas de consulta de propriedade não contam como indisponibilidade do site. Os últimos 100 eventos aparecem na lista. A atualização periódica não depende de a página estar aberta, mas depende da API estar ligada.

O assistente dentro do painel resume dados do domínio atual com uma explicação determinística; não inventa invasões. O assistente de análise pontual mantém a integração Colab/Groq anterior. O novo monitor não transforma o Colab em detector de ataques.

## 4. Trocar domínio e pausar

1. No painel do site, clique **Editar site**.
2. Alterar só o nome mantém a confirmação. Alterar protocolo/domínio cria uma nova revisão, exige novo TXT e revoga a chave de eventos anterior.
3. As amostras e eventos antigos continuam no banco associados à revisão anterior; não entram no gráfico do novo endereço. A interface mostra a revisão atual; não há navegador de revisões antigas nesta entrega.
4. Para interromper consultas sem apagar dados, use **Pausar**. **Retomar** volta a permitir a coleta após a confirmação.

## 5. Receber eventos reais da aplicação ou WAF

Esse modo exige acesso ao backend do site do cliente. Uma URL pública, sozinha, não permite observar tentativas de login ou logs privados.

1. Confirme o domínio por TXT.
2. No painel, encontre **Integração do seu site → Gerar chave de integração**.
3. Confirme a senha e, se ativo, o TOTP/código de recuperação. Um TOTP já utilizado não pode ser repetido; aguarde o próximo código.
4. Copie a chave mostrada uma vez. Guarde-a no ambiente privado do backend do cliente. Nunca no JavaScript público, no aplicativo Android distribuído ou em repositório.
5. O endereço de destino é `https://SEU-SENTINELA/api/v1/ingest/events`. Para teste no mesmo computador, use `http://127.0.0.1:8080/api/v1/ingest/events`.
6. Instale/adapte um dos conectores em `connectors/`. O conector deve enviar eventos nos pontos onde a aplicação realmente identificou cada ocorrência.

Tipos permitidos: `AUTH_FAILURE`, `RATE_LIMIT`, `WAF_BLOCK`, `INTEGRITY_ALERT`, `APPLICATION_ERROR`. O corpo contém ID UUID, tipo, horário em milissegundos e `simulated` explícito. Eventos fora da janela de cinco minutos são recusados. A chave fica no cabeçalho `X-Sentinela-Site-Key`. O servidor aplica limites e ignora duplicata de ID já aceita. Não envie senhas, códigos 2FA, tokens humanos, corpos de requisições ou dados pessoais no evento.

### Testar a integração sem inventar um ataque

Instale Python real (o atalho da Microsoft Store, sozinho, não é o interpretador). No PowerShell da raiz do projeto:

```powershell
$segredo=Read-Host 'Chave exclusiva do site mostrada no painel' -AsSecureString
$env:SENTINELA_SITE_KEY=[System.Net.NetworkCredential]::new('', $segredo).Password
$env:SENTINELA_EVENT_URL='http://127.0.0.1:8080/api/v1/ingest/events'
python connectors/site_event_client.py --simulate --type AUTH_FAILURE
```

O evento aparecerá como **Simulação explícita**. A ferramenta de terminal só envia simulações. Não apresenta esse teste como um ataque real. Domínio confirmado, conta/site ativos e chave atual são necessários até para a simulação.

### Integrar ao backend Java do cliente

Copie `connectors/SentinelaReporter.java` para o backend, ajuste o pacote e inicialize uma única instância usando variáveis de ambiente:

```java
var reporter = new SentinelaReporter(
    System.getenv("SENTINELA_EVENT_URL"),
    System.getenv("SENTINELA_SITE_KEY"));
// No ponto onde a sua aplicação RECUSOU uma autenticação real:
reporter.report("AUTH_FAILURE", false);
// Somente em uma demonstração identificada:
reporter.report("AUTH_FAILURE", true);
```

A chamada é assíncrona e retorna um CompletableFuture<Boolean>; uma falha do Sentinela não deve impedir o fluxo de negócio do cliente. O conector de exemplo não substitui um sistema de filas com retries duráveis. Adapte o emissor às regras reais da sua aplicação. Não coloque uma chamada falsa em cada acesso apenas para preencher gráficos.

Para Python, importe `SiteReporter` de `connectors/site_event_client.py` e chame `report('AUTH_FAILURE', simulated=False)` no backend. A identidade vem da chave do site, não de um siteId escolhido pelo emissor. Rotacionar a chave no painel revoga imediatamente a anterior. Ao trocar o domínio, gere outra chave e atualize o backend.

## 6. Administrar usuários e assinaturas

1. Entre no site com a conta ADMIN provisionada pelo procedimento de README/GUIA-DO-USUARIO e configure seu 2FA obrigatório.
2. A entrada é **Central administrativa**, separada de Meus sites.
3. Em **Assinaturas e acesso**, escolha Ativo, Avaliação, Inadimplente ou Suspenso e informe o vencimento se necessário.
4. Clique **Salvar**. Inadimplente/Suspenso desabilita acesso, encerra sessões e impede novas verificações/eventos. Histórico é preservado. Consultas que já estavam em andamento podem concluir a conexão; o resultado é descartado se a situação mudou antes de persistir.
5. Escolher Ativo/Avaliação reabilita acesso. O vencimento é informativo e não muda a situação automaticamente. Não existe gateway, emissão de cobrança ou confirmação automática de pagamento nesta versão.
6. A seção de sites permite conferir cliente, origem e confirmação; a auditoria registra as mudanças administrativas.

## 7. Android, execução e atualização

No Android, entre na conta e escolha **Meus sites · monitoramento** na tela inicial. Há cadastro, edição, confirmação DNS, pausa, métricas, histórico recente em gráfico e assistente do site. A geração de chave e administração de assinaturas são feitas na área web. O app não precisa guardar uma chave de integração de backend.

Execute API e web pelos CMDs. Flyway aplica a migração V2 automaticamente sem apagar análises anteriores. Preserve `.runtime/encryption.key` e o banco. Faça backup antes de atualizar uma instalação. `MONITORING_ENABLED=false` desativa coleta no ambiente de testes; o padrão é true. A coleta usa duas threads e fila limitada. Não há fila distribuída ou garantia de intervalo exato em grande volume.

## Segurança e limites

DNS é validado antes da conexão e o endereço IPv4 público é fixado ao socket. Endereços privados/reservados e portas arbitrárias são recusados; não há navegação de redirects, scripts, leitura do corpo, scanner de portas ou exploração. HTTPS valida certificado e nome do host. Produção também requer regras de saída de rede, HTTPS da própria plataforma, gestão de segredos, retenção e capacidade operacional.

O cliente com chave pode enviar relatos falsos; eles são identificados como reportados pelo conector, não como fatos comprovados independentemente. Nunca diga “100% seguro” a partir desses indicadores. Referências: [OWASP sobre SSRF](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html), [Java SSLParameters](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/net/ssl/SSLParameters.html).
