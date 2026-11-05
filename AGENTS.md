# Continuidade do Sentinela

Leia `docs/CONTEXTO-COMPLETO.md`, `docs/ESTADO-ATUAL.md`, `docs/MAPA-TECNICO.md` e `docs/OPERACAO-COMPLETA.md` antes de alterar. Código e estado real prevalecem sobre planos históricos.

- Preserve stack acadêmica: Next.js/TypeScript, Java 21/Spring Boot/JDBC/Flyway, Android Java, Python Colab/Groq.
- Groq (console.groq.com) é o provedor confirmado. O Java calcula score; IA explica.
- Existem análise pontual e monitoramento com propriedade DNS. URL/HEAD não comprovam ataque ou segurança absoluta.
- Cadastro cria USER; ADMIN tem MFA obrigatório, USER opcional salvo REQUIRE_MFA=true. Não enfraquecer autorização/CSRF/TLS para contornar erro.
- Preserve ENCRYPTION_KEY exatamente em migrações; ela protege segredos MFA existentes.
- Não colocar credenciais, dumps, H2, `.runtime`, `.env`, QR MFA, keystore ou dados pessoais em Git/documentos/contexto para IA.
- Testes de integração escrevem/apagam dados. Use banco isolado, nunca produção ou branch com contas reais.
- Consulte `docs/MIGRACAO-NEON.md` antes de mudar conexão. Não trocar para banco vazio; backup, ensaio, congelamento de escrita, validação e retorno são obrigatórios.
- Mantenha documentação atualizada com data, comandos/resultados observados e pendências. Não afirmar testes não executados.
- Não contratar plano pago, excluir recursos de origem ou sobrescrever dados de produção sem autorização específica.

Comandos: `services/api/mvnw.cmd test`; em `apps/web`: `npm test` e `npm run build`; na raiz: `python -m unittest discover -s notebooks/tests`; em `apps/android`: `gradlew.bat assembleDebug lintDebug`. Consulte o manual para dependências e variáveis.
