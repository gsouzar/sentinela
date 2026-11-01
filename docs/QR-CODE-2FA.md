# Configurar Google Authenticator com QR code

O site oferece o mesmo cadastro de autenticador para ADMIN e usuário. Cada conta recebe sua própria chave. O QR code é gerado no navegador, sem serviço externo de imagens.

1. Entre no Sentinela e abra **Minha segurança**. Para ADMIN sem segundo fator, essa tela abre automaticamente.
2. Informe sua senha e clique **Configurar segundo fator**.
3. No celular, abra **Google Authenticator → + → Ler QR code**. Escaneie o código exibido no site. Confira a conta Sentinela e seu email no aplicativo.
4. Digite o código de seis dígitos do aplicativo no Sentinela e clique **Confirmar e ativar**. Escanear apenas adiciona a conta ao aplicativo; esta confirmação conclui a proteção no servidor.
5. Guarde os códigos de recuperação exibidos. Cada um só pode ser usado uma vez. O QR code desaparece após a ativação.
6. Nos próximos acessos, entre com email, senha e o código atual do aplicativo. Ative data e hora automáticas no celular. Se acabou de usar um código, aguarde o próximo.

Sem câmera ou usando o mesmo celular para abrir o site? Expanda **Não consegue escanear? Use a chave manual** e cadastre a chave no autenticador com tipo baseado em tempo (TOTP). No aplicativo Android nativo, a configuração por chave manual continua disponível.

Contas com 2FA já ativo continuam usando o autenticador cadastrado; esta atualização não troca a chave nem desativa a proteção existente. Não compartilhe QR code, chave ou códigos de recuperação.

## Atualizar uma instalação existente

Pare o terminal do site com Ctrl+C. Na pasta `apps\web`, execute `npm ci` e depois `npm run build`. Abra novamente `2-INICIAR-WEB.cmd` na raiz do projeto. Preserve banco, `.env` e `.runtime/encryption.key`. Não é necessário gerar nova chave Groq nem mudar configurações do Colab para usar este recurso.
