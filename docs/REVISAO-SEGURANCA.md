# Revisão de segurança — atualização 2FA e Telegram

**Política atual:** na evolução de monitoramento, 2FA é opcional para clientes USER e obrigatório para ADMIN. REQUIRE_MFA=true exige o fator de todos. Leia MONITORAMENTO.md e VERIFICACAO.md; descrições de MFA obrigatório geral abaixo registram a revisão anterior.

Revisão de código, testes de regressão e revisão independente em 31/10/2026. Escopo: autenticação, sessões, CSRF, concorrência em operações sensíveis, Android e integração Telegram. Não é pentest externo nem prova de ausência de falhas.

| Achado | Correção e evidência |
|---|---|
| Cabeçalho de cliente Android podia desviar a verificação CSRF em requisição com cookie web | Cookie de sessão mantém obrigatoriedade de CSRF. Teste envia cookie + cabeçalho Android e exige 403. |
| Login concorrente podia observar estado antigo de MFA durante ativação | Login bloqueia a linha da conta e relê o estado de segurança antes de emitir sessão. Teste ativa o fator enquanto o login aguarda e exige 428 sem código. |
| Configuração concorrente podia substituir segredo de fator recém-ativado | Operações de segurança serializadas por usuário; setup revalida MFA e sessão dentro da transação. Teste confirma 409 e preservação do segredo. |
| Replay concorrente do refresh podia deixar a sessão vencedora utilizável | Refresh serializado por usuário, token consumido registrado e reutilização revoga sessões. Teste paralelo exige 200/401 e depois 401 para a sessão vencedora. |
| Tentativas de login recusadas eram removidas da auditoria por rollback | Falhas de credencial e MFA são auditadas em transação independente. Teste confirma persistência do evento após rejeição. Achado confirmado por revisão independente. |
| Chave curta de criptografia podia ser configurada | Configuração com menos de 32 caracteres é recusada. Inicializador gera 32 bytes aleatórios em hexadecimal. Comprimento não substitui entropia: use geração criptográfica. |
| 2FA era opcional e estado local podia atravessar troca de conta | TOTP obrigatório por padrão; administração sempre exige fator. Web bloqueia navegação funcional antes de matrícula e limpa filtros, dados e estado entre contas. Android direciona para configuração. |
| Dados sensíveis podiam aparecer em backup/transferência/captura Android | Backup e transferência de dados excluídos explicitamente; FLAG_SECURE bloqueia captura da janela; tokens permanecem criptografados com Android Keystore. |

## 2FA entregue

Senha + TOTP de seis dígitos/30 segundos; segredo AES-GCM com chave externa; contador anti-replay; oito códigos de recuperação aleatórios, armazenados por hash e consumidos uma vez. Ativação exige senha e código, revoga outras sessões e recusa substituição silenciosa do fator. A conta recém-criada só pode concluir segurança e gerenciar sessão até ativar o fator. O servidor aplica a política mesmo com cliente modificado. `REQUIRE_MFA=true` é o padrão; administradores sempre exigem MFA. Desativar essa variável reduz a exigência para usuários comuns e não deve ser feito na apresentação.

O guia COMO-RODAR.md explica configuração por QR code no site e alternativa manual no autenticador. Não há recuperação por email nem bypass administrativo de MFA. Preserve chave, banco e códigos de recuperação. TOTP não é resistente a todo phishing e não protege uma sessão já roubada de todas as formas; HTTPS e proteção do dispositivo continuam necessários. Referência: [OWASP sobre MFA](https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html).

## Telegram

Chave de máquina separada com comparação de tempo constante; lista obrigatória de IDs no bot; apenas conversa privada; identidade técnica pseudônima derivada por HMAC; nenhum login humano emitido para papel BOT; análise e chat com autorização pela identidade da conversa; limite por conversa e global; respostas sem HTML/Markdown interpretado e sem preview. A API Java valida chave e igualdade dos IDs, mas confia no processo bot autorizado para atestar a identidade Telegram: proteger essa chave é essencial.

O bot não segue redirects HTTP, não acessa o destino informado e não registra URLs da Bot API contendo token. HTTP no conector Python só é permitido em loopback local; Colab exige API HTTPS. `/apagar CONFIRMAR` remove análises/chat da API, não mensagens Telegram/auditoria. A chave pseudonimizadora não torna dados anônimos. O bot não lê histórico de contas web/mobile.

## Validação e limites

25 testes Java passaram com H2 e novamente com PostgreSQL 17.6; quatro testes web; dez Python (incluindo cinco do bot); production build Next e build/lint Android. `npm audit --omit=dev` não reportou vulnerabilidades nessa consulta. O JAR atualizado foi iniciado e respondeu health UP. Relatórios detalhados em VERIFICACAO.md e evidencias/.

Não foi feito teste de dispositivo Android, Telegram real, provedores com chaves reais, carga, análise completa de vulnerabilidades das dependências Java/Python, pentest externo ou implantação pública. A confirmação da entrega é restrita aos controles e testes descritos. Em produção, configure HTTPS, Secure cookies, gestão e rotação de segredos, retenção, backups testados e controle de borda. O rate limiter atrás do Next pode agrupar o IP de visitantes; configuração de proxy confiável continua pendente para hospedagem distribuída.
