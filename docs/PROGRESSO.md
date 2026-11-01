# Estado atual

Concluída a evolução do analisador para painel de sites do cliente: cadastro/edição/pausa, propriedade por TXT, verificações externas, certificado/cabeçalhos, histórico por revisão e ingestão opcional de eventos tipados. Área administrativa separada, com assinatura manual e suspensão preservando dados. Cliente pode optar por2FA; ADMIN exige fator. Android inclui sites e métricas; analisador de links, Colab e Telegram anteriores continuam disponíveis.

38 testes Java por banco,4 web e18 Python; build Next na cópia do usuário, TypeScript, APK/lint e conector Java. Revisão independente e regressões de HTTP intermediário/DNS. Consulte VERIFICACAO.md para limites. Falta configuração real do domínio e das integrações pelo proprietário, não código fictício ou dados simulados disfarçados.

Groq confirmado no Colab: chave e modelo nos Secrets, endpoint fixo com validação de evidências, diagnóstico de origem e fallback. Manual e pacotes atualizados.
