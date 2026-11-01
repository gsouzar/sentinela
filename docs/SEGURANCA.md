# Segurança e limites

**Política atual:** na evolução de monitoramento, 2FA é opcional para clientes USER e obrigatório para ADMIN. REQUIRE_MFA=true exige o fator de todos. Leia MONITORAMENTO.md e VERIFICACAO.md; descrições de MFA obrigatório geral abaixo registram a revisão anterior.

Atualização: consulte [revisão de segurança, 2FA obrigatório e Telegram](REVISAO-SEGURANCA.md).

| Risco | Controle |
|---|---|
| SQL injection | JDBC parametrizado e validação de filtros |
| Acesso cruzado e escalada | Proprietário ou ADMIN no servidor; cadastro sempre USER |
| XSS | React/TextView escapam texto, sem execução de HTML; CSP |
| SSRF | Sem navegação ao destino; endpoints externos fixos e sem redirects |
| Senhas vazadas no banco | Argon2id |
| CSRF web | Token Spring, cookie e renovação por mutação |
| Tokens vazados no banco | Hash SHA-256 de tokens aleatórios de 256 bits |
| Replay de refresh | Rotação e revogação da conta |
| Replay de TOTP | Contador atualizado condicionalmente |
| Força bruta | Limites persistidos por conta e IP observado |
| Segredo MFA | AES-GCM com chave externa |
| Entrada excessiva | Validação, paginação e corpo de 16 KB |
| Fonte ausente | Estado explícito e cobertura parcial |
| Prompt injection | Score fora da IA, evidências autorizadas, referências validadas e fallback |

HTTPS, chaves persistentes, origem exata e segredos próprios são necessários para implantação. Nunca remova HttpOnly ou publique o banco. O Compose roda sem privilégios e limita a publicação web ao loopback. Docker não estava disponível para execução dos containers.

O rate limiter usa getRemoteAddr(), sem confiar em X-Forwarded-For arbitrário. Atrás do Next.js, limites por IP podem agrupar visitantes no IP do proxy. Produção distribuída requer configuração de proxy confiável e controle de borda; não houve ensaio de alta carga.

O usuário pode excluir análises e conversas; excluir uma análise remove conversas vinculadas. Auditoria não é editável pelas APIs comuns. Não há expurgo automático completo. A instituição deve definir retenção e backups; scripts/manutencao.sql exemplifica limpeza de tokens/buckets expirados e não roda automaticamente. Guarde a chave MFA separadamente do backup e teste restauração.

Não há varredura de conteúdo, DNS, certificado, idade ou redirecionamento do site, nem recuperação de senha por email, alertas por email ou detecção geográfica de acesso. Regras lexicais têm falsos positivos e negativos. Saída JSON e IDs válidos não garantem veracidade de toda frase da IA. O resultado determinístico permanece separado da complementação Colab.

Os controles não constituem certificação, conformidade LGPD ou garantia de ausência de vulnerabilidades. Consulte VERIFICACAO.md para evidências e limites de execução.
