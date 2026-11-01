# Sentinela — projeto nativo Java para Android Studio

Abra esta pasta pelo botão **Open** do Android Studio: nela estão `settings.gradle`, `build.gradle` e `gradlew.bat`. Não importe o APK como projeto.

1. Instale Android SDK 35 e aceite o Gradle Sync; use JDK 17/21 compatível com AGP 8.9.3.
2. Inicie a API Java do projeto completo no computador, na porta 8080.
3. Crie um emulador API 35 e execute o módulo `app` pelo botão Run. O debug padrão acessa `http://10.0.2.2:8080/api/v1`.
4. Cadastre/entre na conta e ative 2FA em Sua segurança antes de analisar links.

Para aparelho USB, execute `adb reverse tcp:8080 tcp:8080` e gere com `gradlew.bat assembleDebug -PapiBase=http://127.0.0.1:8080/api/v1`. Instale o APK em `app/build/outputs/apk/debug`. O APK padrão do pacote usa IP do emulador.

Release exige endpoint HTTPS e assinatura configurada no Studio: `gradlew.bat assembleRelease -PapiBase=https://api.seu-dominio.example/api/v1`. Nunca versionar keystore ou senha. HTTP é bloqueado no release. Sessões são criptografadas com Android Keystore, backup/transferência são excluídos e capturas da janela são bloqueadas.

Build/lint foram executados; teste de navegação e Keystore em dispositivo ainda é necessário. O app depende da API Java do projeto completo. Veja COMO-RODAR.md fornecido junto ao ZIP para detalhes.
