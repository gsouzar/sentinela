# Atualização de segurança, 2FA e Telegram

**Política atual:** na evolução de monitoramento, 2FA é opcional para clientes USER e obrigatório para ADMIN. REQUIRE_MFA=true exige o fator de todos. Leia MONITORAMENTO.md e VERIFICACAO.md; descrições de MFA obrigatório geral abaixo registram a revisão anterior.

Escopo autorizado: revisar e corrigir falhas do Sentinela, reforçar 2FA, entregar projeto Android Studio separado e chatbot Telegram integrado ao Colab. Execução direta conforme autorização anterior.

Decisões: TOTP obrigatório por padrão para contas humanas, sempre obrigatório para administradores; sessão sem fator só permite concluir configuração e sair. Contas técnicas Telegram não recebem sessão humana. Endpoint de integração exige chave separada e identidade pseudônima por conversa privada, sem acesso ao histórico de pessoas do web/app. Bot recebe mensagens por long polling, sem precisar publicar webhook. Credenciais nunca são pedidas pela conversa.

## Plano
- [x] Reproduzir e testar CSRF, concorrência de login/MFA/refresh e vazamento de estado entre contas.
- [x] Serializar operações de segurança por usuário com transações e bloqueio de linha; reforçar Argon2id, chave MFA e política de 2FA.
- [x] Atualizar web e Android para matrícula obrigatória em 2FA, limpar dados entre contas e bloquear backups/capturas sensíveis.
- [x] Implementar endpoints Telegram de escopo restrito e bot Python com allowlist, proteção de token, isolamento e comandos de privacidade.
- [x] Atualizar notebook Colab e instruções BotFather, autenticação e Android Studio.
- [x] Executar testes e builds, revisão independente final e gerar ZIPs separados e pacote completo sem dados/segredos locais.

Verificação: testes Java com H2/PostgreSQL, testes web/Python, build Next.js, assembleDebug/lint Android, validação dos notebooks e arquivos ZIP. A ativação real do Telegram depende do token local do proprietário; nenhum bot real será configurado nem mensagem enviada nesta revisão sem essas credenciais.
