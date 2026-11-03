WYZEN P — projeto Android inicial

IMPORTANTE
Esta é uma base funcional inicial, não a aplicação completa prometida.
Inclui:
- aplicação Android nativa em Kotlin;
- interface inicial;
- resposta à hora/data;
- consulta do nível de bateria;
- abertura das Definições do Android;
- leitura das respostas com a voz TTS instalada no telefone;
- GitHub Actions para compilar o APK.

AINDA NÃO INCLUI
- IA online;
- ativação permanente por palavra de ativação;
- execução em segundo plano;
- chamadas, SMS, contactos ou pesquisa de ficheiros.
Essas funções exigem implementação e permissões próprias, que serão adicionadas e testadas por etapas.

ESTRUTURA
app/
  build.gradle.kts
  src/main/AndroidManifest.xml
  src/main/java/com/wyzenp/assistant/MainActivity.kt
build.gradle.kts
settings.gradle.kts
gradle.properties
.github/workflows/android.yml

COMPILAÇÃO
O workflow compila automaticamente quando os ficheiros forem enviados para a branch main/master.
No GitHub: repositório > Actions > Build WYZEN P APK > execução concluída > Artifacts > WYZEN-P-debug-apk.
