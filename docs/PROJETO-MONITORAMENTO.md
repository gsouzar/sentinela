# Evolução aprovada: painel do cliente e monitoramento

Base: usuário confirmou análise + monitoramento de sites, com as duas modalidades (URL pública e integração instalável). Preservar Next.js, Java, Android Java e Colab. O cadastro humano continua USER; administração é separada.

## Comportamento

Cliente entra em um painel de seus sites, cadastra nome e URL e vê orientação de confirmação de domínio. Pode editar nome/domínio, pausar/reativar, consultar gráficos e usar assistente e segurança da própria conta. Troca de origem invalida verificação e chave de eventos; histórico anterior não é atribuído ao novo domínio. 2FA é opcional para cliente e obrigatório para ADMIN, conforme pedido atual; administradores não mudam senha de clientes sem prova nem veem segredos.

Monitoramento externo mede somente disponibilidade amostrada HTTP, tempo da verificação, certificado HTTPS e presença de cabeçalhos. Requer confirmação DNS TXT de propriedade antes de consultas recorrentes. Não executa scripts, não varre portas e não explora vulnerabilidades. HTTP/HTTPS apenas em portas padrão, raiz do domínio, DNS público IPv4 e conexão fixada ao IP validado para impedir SSRF/rebinding; sem seguir redirecionamentos. No início, dashboard mostra sem amostras, nunca fabrica uptime/ataques.

Integração opcional recebe eventos tipados do backend/WAF do cliente, com chave exclusiva rotacionável, escopo por site, limite de requisições e deduplicação por ID. Exibe eventos reportados pelo conector; autenticação rejeitada não comprova invasão. Exemplos de demonstração são explicitamente simulados. Chave e credenciais nunca no código público. Não há afirmação de segurança 100%, proteção ativa ou ataque real sem evidência.

Dashboard escuro com cartões e séries temporais inspiradas em ferramentas de observabilidade: disponibilidade nas amostras, resposta HTTP, validade de certificado, achados de configuração e eventos de segurança. Distinção clara entre fonte externa, evento de integração e ausência de dados. Gráficos usam medições persistidas. Cadastro já permite abrir o painel, com monitoramento pendente até comprovação do domínio.

## Administração

Área própria para listar contas e sites, suspender/reativar e auditar mudanças. Suspensão bloqueia acesso e novas verificações. Situação de assinatura é controle manual nesta etapa, sem cobrança automática ou gateway. Falta de pagamento não elimina dados. Guardar histórico e revogar sessões na suspensão. Contas BOT não equivalem a clientes pagantes.

## Entrega e limites

API e web com testes de autorização, SSRF, domínio, alteração, isolamento, ingestão e métricas. Android usa a mesma API e ganha acesso básico aos sites. Conector de exemplo e guia de DNS/instalação. Atualizar contratos, documentação, binários e ZIPs. Integrações reais dependem do domínio e credenciais do proprietário; banco e chaves existentes não devem ser apagados.
