# Roteiro de apresentação

Duração sugerida: 8–10 minutos. Teste o ambiente antes da banca e identifique o modo demo.

1. Explique o problema: ajudar pessoas leigas a avaliar URLs antes de fornecer dados.
2. Mostre a arquitetura Next.js, Java, Android Java, banco e chatbot Colab. A IA explica; o motor Java calcula risco.
3. No app, analise `http://banc0-seguranca-login.test/confirmar?token=exemplo`. No modo demo, o score 80 usa correspondência SINTÉTICA identificada. Não apresente como consulta real de phishing conhecido.
4. Mostre HTTP +10, termos +10, imitação +25 e contribuição até o piso 80. Mostre query removida e fontes reais sem chave como não configuradas. Explique cobertura parcial.
5. Pergunte ao assistente por que o link é suspeito. Mostre modo LOCAL ou COLAB conforme a configuração real. A explicação não cria novos indicadores.
6. No web, confira histórico, filtro, detalhes e dashboard. Analise `https://example.org` e explique que zero não significa selo de segurança.
7. Mostre MFA, sessões e trilha administrativa. Demonstre nos testes que outra conta recebe 404 e usuário comum recebe 403 em administração.
8. Abra o relatório de testes e o avaliador de CSV. Explique que eficácia exige dataset independente com fonte, rótulos e coleta documentados. Fixtures comprovam comportamento de engenharia, não taxa científica de acerto.

Perguntas prováveis: Como sabem que é golpe? Por múltiplas evidências, sem garantia absoluta. E se a fonte cair? Cobertura parcial explícita. A IA inventa? Há risco semântico, por isso score e explicação determinística permanecem no backend. Como mediram? Apresente apenas testes e métricas efetivamente executados.

Cada integrante deve conseguir explicar o código da sua frente e a integração. Não use URLs maliciosas ativas durante a apresentação.

Para a apresentação atual, configure o 2FA antes de demonstrar análises, guarde os códigos de recuperação e mantenha o autenticador disponível. Para Telegram, inicie a instância Python com seu token privado e seu ID autorizado; não exiba credenciais na projeção. Guias: COMO-RODAR.md e TELEGRAM.md.
