# Plano de implementação do Sentinela
> Execução nesta sessão com superpowers:executing-plans; revisão técnica final independente.

Objetivo: integrar web Next.js, API Java, Android Java e chatbot Colab em um analisador de URLs persistente.
Especificação: ARQUITETURA.md, ajustada pela aprovação do usuário: Android nativo Java substitui Expo; notebook Python/Colab integra a frente de chatbot.

Restrições: português; score não é probabilidade; fontes indisponíveis nunca significam seguro; nenhuma navegação a URLs arbitrárias; sem credenciais no código.

## Foco da revisão
Isolamento de dados por usuário; formatos de URL ambíguos; falhas de provedor; replay de TOTP e refresh; conteúdo não confiável no chat e na interface.

## Tarefas
- [x] 1. API e motor: pom.xml, análise/UrlAnalyzer.java, testes UrlAnalyzerTest. Testar rejeição de loopback, credenciais e esquemas inválidos, score e fontes parciais; executar Maven, implementar e repetir.
- [x] 2. Persistência e segurança: entidades, migração, AuthService, SecurityConfig, MFA, testes de integração MockMvc. Registrar/logar, negar CSRF e IDOR, revogar sessões e validar MFA.
- [x] 3. Serviços de análises, dashboard, auditoria e chatbot: contratos REST /api/v1. Testes exercitam persistência, autorização, filtros e evidências do chat antes da implementação.
- [x] 4. Web: Next.js app, componentes, estilos e cliente de API. Jornadas reais de cadastro → análise → detalhes → chat → histórico; testar compilação e navegador desktop/mobile.
- [x] 5. Android Java: Gradle, Manifest, cliente HTTP, atividades e armazenamento Android Keystore. Mesma API e controles de acesso; compilar quando houver SDK.
- [x] 6. Colab: notebook executável com explicador baseado em evidências, integração opcional com Groq e avaliação de dataset; executar células locais compatíveis sem chave e validar notebook.
- [x] 7. Entrega: Docker, README, OpenAPI, roteiro da banca, matriz de segurança, relatório de testes reais; revisão independente, corrigir achados e empacotar fonte.

Cada tarefa registra arquivos, verificações, limitações e decisões em docs/PROGRESSO.md. Projeto novo em diretório próprio, sem alteração de repositórios existentes. A autorização para seguir já foi dada; escolhas reversíveis serão resolvidas durante a execução.
