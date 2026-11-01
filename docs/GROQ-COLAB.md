# Chatbot com Groq no Google Colab

O provedor confirmado pela equipe é **Groq**, em **console.groq.com**. Grok/xAI é outro serviço. O código usa o endpoint oficial HTTPS do Groq, sem SDK Groq.

1. Entre em [Groq Console](https://console.groq.com/keys), abra **API Keys** e gere uma chave da sua conta/projeto. Não compartilhe a chave.
2. No Playground ou na [lista oficial de modelos](https://console.groq.com/docs/models), obtenha o **ID exato de um modelo de chat disponível na sua conta**, compatível com JSON Object Mode. Não há modelo escolhido silenciosamente pelo projeto; permissões e cotas dependem da conta.
3. Importe `notebooks/Sentinela_Chatbot_Colab.ipynb` no Colab. Ele contém seu próprio código.
4. Na barra lateral do Colab, abra **Secrets** (ícone de chave) e crie `GROQ_API_KEY` com sua chave e `GROQ_MODEL` com o ID do modelo. Autorize acesso do notebook aos dois Secrets.
5. Adicione `COLAB_CHAT_KEY` com uma chave compartilhada aleatória de pelo menos 32 caracteres, diferente da chave Groq. Se quiser conectar o endpoint temporário à API Java, configure também `NGROK_AUTHTOKEN`.
6. Execute instalação, funções e exemplo sintético. Depois execute a célula que carrega Secrets e a célula de diagnóstico.
7. O diagnóstico deve mostrar **Origem: GROQ | Situação: OK**. Isso confirma uma resposta recebida e validada do modelo. **LOCAL** indica explicação de reserva: `NOT_CONFIGURED` = sem chave; `MODEL_NOT_SET` = sem modelo; `UNAVAILABLE_OR_INVALID` = falha, limite, timeout ou resposta recusada pela validação. Confira modelo/permissão/cota na console sem imprimir chaves ou exceções brutas.
8. Para conectar o site, execute as células do servidor e túnel. Configure no PowerShell da API Java `COLAB_CHAT_URL` com a URL HTTPS exibida terminando em `/explain` e `COLAB_CHAT_KEY` com a mesma chave compartilhada do notebook. Reinicie a API nesse terminal.
9. Faça uma análise de link no site e pergunte sobre o resultado. O modo **COLAB** confirma passagem pelo notebook; o diagnóstico do notebook confirma se Groq respondeu ou se houve fallback local.
10. Encerre túnel e runtime ao terminar. Não há instalação de modelo local nem necessidade de GPU para este notebook. A API Java continua calculando o score, e sua explicação determinística é preservada.

Os testes do projeto usam transporte simulado, não uma chave real. JSON válido e IDs de evidência autorizados não garantem veracidade de toda frase do modelo. O conector não segue redirects, limita a resposta e não registra credenciais.

Referências: [geração de texto](https://console.groq.com/docs/text-chat), [JSON Object Mode e suas limitações](https://console.groq.com/docs/structured-outputs), [modelos disponíveis](https://console.groq.com/docs/models).
