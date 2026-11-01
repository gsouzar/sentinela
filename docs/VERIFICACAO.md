# Verificação da versão com monitoramento

Data: 31/10/2026. Java21, Spring Boot3.5.16, Next16.4.3, Android Java/AGP8.9.3.

| Verificação | Resultado |
|---|---|
| API com H2 | 38 testes; zero falhas/erros |
| API com PostgreSQL17.6 | 38 testes; zero falhas/erros |
| Testes web | 4 aprovados |
| TypeScript | Aprovado |
| Next production build na cópia OneDrive | Aprovado |
| Android assembleDebug/lintDebug | APK atualizado; zero erros; 11 avisos |
| Python (Colab/Telegram/conector de site) | 18 testes aprovados |
| Conector Java | Compilado com javac21 |
| Migrações | V1 e V2 aplicadas em bancos de teste; sem alteração destrutiva de V1 |
| Navegador com API e banco temporário | Login, sites privados, dashboard pendente, troca de domínio/revisão e assistente observados |
| Responsividade web | Viewport de390px; conteúdo e largura útil iguais a375px, sem overflow global |

## O que foi validado

Além dos testes anteriores de autenticação, MFA, CSRF, refresh, propriedade de análises e reputação: normalização de origem e portas; exclusão de redes privadas/reservadas; parsing limitado de cabeçalhos e redirecionamento sem navegação; respostas1xx intermediárias antes de500; isolamento entre clientes; ausência de uptime inventado; alteração de domínio revogando prova/chave; separação do histórico por revisão; ingestão autorizada/idempotente e eventos simulados separados; rotação de chave; pausa; assinatura administrativa suspendendo sessões sem eliminar sites; falha temporária de DNS preservando confirmação e adiando acesso; amostras agregadas e explicação baseada em evidências. O conector Python foi exercitado com transporte simulado, sem envio a serviço público.

A revisão independente encontrou tratamento incorreto de respostas HTTP provisórias e confusão entre falha de resolução DNS e ausência de prova. Ambos receberam regressões e correções. A segunda revisão não identificou problemas importantes remanescentes nesses ajustes; isso não é certificação de ausência de falhas.

## Evidência visual e estado real

Capturas monitoramento-cliente.png, painel-cliente-resumo.png e monitoramento-mobile.png registram uma conta sintética, em banco de memória, sem domínio confirmado e sem medições inventadas. O domínio de exemplo foi alterado de example.org para example.net e a revisão passou de1 para2. O assistente afirmou corretamente que não havia evidência de ataque. Não foi instalado conector em um cliente real.

A cópia C:/Users/gugu2/OneDrive/Documentos/sentinela recebeu o código, binários e guias; arquivos substituídos foram preservados em .runtime/backups e o sincronizador excluiu bancos, chaves e configuração secreta. Sua compilação web passou. Uma instância de teste da API nessa pasta respondeu UP. Instâncias iniciadas para conferência foram encerradas; os CMDs continuam sendo o caminho para iniciar o projeto.

## Limites

Não houve confirmação DNS de domínio real sob controle do usuário, coleta pública recorrente com seu domínio, instalação em servidor de cliente, Telegram real, Groq/Colab com credenciais reais, teste em aparelho/emulador Android, pentest externo, carga, implantação pública ou Docker/CI hospedado. A disponibilidade é amostrada e depende da API ligada. O conector reporta eventos; ele não comprova invasão. O monitor não explora vulnerabilidades nem bloqueia ataques. Sem prova/dados, as métricas permanecem vazias.

Os avisos Android são de manutenção (textos/localização, preferências e recursos de ícone/SDK); o XML detalha cada caso. Alguns métodos nativos são depreciados, mantendo compatibilidade. Não houve auditoria abrangente de dependências Java/Python. A última consulta npm registrada em evidencias/npm-audit-resumo.json era zero vulnerabilidades reportadas de produção; não representa todo o sistema.

## Evidências e contratos

XMLs JUnit por banco em evidencias/h2 e evidencias/postgresql; lint Android, capturas e resumo npm na mesma árvore. MONITORAMENTO.md explica configuração e limites. openapi.json registra rotas, complementado pelos corpos e exemplos no guia e código. Os relatórios podem conter caminhos locais de compilação; não incluem chaves reais ou bancos.


## Correção do provedor do chatbot

Provedor confirmado pela equipe: Groq (console.groq.com). Notebook e módulo Python agora usam GROQ_API_KEY/GROQ_MODEL e o endpoint HTTPS oficial de chat. Dependência Gemini removida. Seis novos testes de transporte simulado cobrem chamada, cabeçalho, modelo explícito, evidências, resposta inválida/excessiva, falha e ausência de configuração; suíte Python total18 passou. Notebook validado por nbformat/AST, fonte embutida idêntica ao módulo e fallback exercitado. Revisão independente não identificou achados importantes. Nenhuma chave real ou chamada real ao Groq foi usada. O modo COLAB no Java continua significando passagem pelo notebook; providerMode/providerStatus na célula de diagnóstico identificam GROQ/OK ou LOCAL. Manual PDF corrigido renderizado e inspecionado, com19 páginas. Java/web/Android não tiveram mudança de runtime nesta correção.


## QR code TOTP no site — 01/11/2026

QRCodeSVG local com URI retornada pela API, sem servidor externo de imagens. Tela compartilhada USER/ADMIN. Typecheck, quatro testes web e build de produção aprovados. Fluxos no navegador com API simulada: cadastro, código incorreto, confirmação, códigos de recuperação e remoção do QR após ativar passaram para ambos os perfis. As duas imagens foram decodificadas com ZXing e correspondem exatamente à URI TOTP simulada. Não houve leitura física com Google Authenticator. API Java e Android não foram alterados. Manual PDF de 19 páginas regenerado, sem glifos fora das margens, e página alterada inspecionada visualmente.
