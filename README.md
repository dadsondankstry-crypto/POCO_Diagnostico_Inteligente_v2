# POCO Diagnóstico — 1.0.0

Aplicativo Android nativo (Java), em português brasileiro, offline, sem anúncios e sem permissões de armazenamento amplo.

## Recursos
- RAM total, disponível, ocupada estimada, limiar de alerta e sinal `lowMemory` do Android.
- Amostras da RAM a cada 3 s **somente enquanto o app está aberto**, até 600 amostras.
- Armazenamento interno total, utilizado e disponível.
- Selecionar uma pasta autorizada pelo seletor de documentos Android (SAF), listar os maiores arquivos e excluir um de cada vez com confirmação.
- Relatório TXT com histórico, sistema e nomes dos arquivos; você escolhe onde salvar e se compartilha.
- Atalhos para configurações do Android e ChatGPT.

## Limitações importantes
- Android 10+ impede aplicativos comuns de medir o consumo de RAM de outros aplicativos. Não há uma lista confiável de RAM por app, nem leitura dos logs privados de travamento do ChatGPT. Não prometemos isso.
- A RAM 'em uso' é calculada como total menos disponível: inclui caches e não significa memória irrecuperável.
- A gravação para ao minimizar/fechar o aplicativo. O sistema pode suspender o processo.
- O analisador de pasta limita a leitura a 5000 arquivos e profundidade de 15 níveis. Algumas pastas do sistema são inacessíveis. A exclusão depende da permissão concedida pelo provedor.
- Nomes de arquivos podem ser sensíveis; revise o relatório antes de compartilhar.
- Não é limpador de RAM e não executa rotinas que forcem o encerramento de outros aplicativos.

## GitHub Actions
Extraia o ZIP na raiz do repositório com a pasta oculta `.github`. Faça push em `main` ou `master`, ou inicie manualmente o workflow `Compilar APK - POCO Diagnostico`. Na aba Actions, baixe o artefato `POCO-Diagnostico-APK`, que contém `app-debug.apk`.

Requisitos: GitHub Actions habilitado. Compilação usa JDK 17, Gradle 8.13, Android Gradle Plugin 8.13.2 e SDK 36. O workflow instala o Gradle pela action; não requer wrapper. APK debug não possui chave de assinatura estável entre instalações: cuidado ao atualizar versões para não perder dados.

Fonte entregue sem teste de compilação Android local; o GitHub Actions fará a validação de build.
