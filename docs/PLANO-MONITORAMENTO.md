# Plano de implementação — monitoramento do cliente

Objetivo: evoluir o analisador para painel multiusuário de sites verificados, mantendo separação de administração e evidências reais.
Spec: PROJETO-MONITORAMENTO.md. Execução nativa nesta sessão, com testes antes de código sensível e revisão independente ao final.

- [x] 1. API: migração V2 com sites, amostras e eventos; isolamento, revisão de domínio, pausa e assinatura manual. Testar que cliente não vê contas/sites alheios e troca de origem revoga a integração.
- [x] 2. Probe público: normalização de origem, portas padrão, classificação IPv4, DNS TXT e conexão fixada. Testar rede privada/reservada, endereço ambíguo, redirect e parsing limitado antes de ativar agendamento.
- [x] 3. Integração de eventos: chave por site, prova de senha/MFA para rotação, idempotência e limites; testes de chave errada, domínio não confirmado, eventos simulados e isolamento.
- [x] 4. Cliente Next: Meus sites, cadastro, detalhe com séries reais, verificação DNS, editar/pausar, integração e acesso ao assistente. Separar navegação admin, incluir assinatura manual e páginas vazias honestas.
- [x] 5. Android: listagem/cadastro/detalhe/alteração dos sites e instrução DNS usando mesma API. Conector e guia com origem e limites de cada métrica.
- [x] 6. Validar H2/PostgreSQL, web, Android e conector. Revisar código, preservar dados locais e atualizar a cópia ativa e os pacotes.

Foco de revisão: DNS rebinding; IPv4 mapeado/privado; dados de domínio antigo exibidos no novo; solicitação em andamento durante suspensão/edição; chave de integração vazando; porcentagem com zero amostras; eventos simulados apresentados como ataques reais. Estas condições devem ter teste ou tratamento explícito nos respectivos componentes.
