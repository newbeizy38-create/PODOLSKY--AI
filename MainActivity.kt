package com.wyzenp.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.provider.ContactsContract
import android.net.Uri
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color
import android.content.Context
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var output: TextView
    private lateinit var tts: TextToSpeech
    private var speechReady = false
    private val voiceRequest = 4101
    private val permissionRequest = 4102
    private var pendingContactSearch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildScreen()
        say("WYZEN P iniciada. Estou pronta para receber comandos.")
    }

    private fun buildScreen() {
        val bg = Color.rgb(8, 13, 24)
        val panel = Color.rgb(19, 29, 47)
        val cyan = Color.rgb(53, 201, 255)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(bg)
        }
        val title = TextView(this).apply {
            text = "WYZEN P"
            textSize = 30f
            setTextColor(cyan)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "ASSISTENTE PESSOAL OFFLINE"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 24)
        }
        output = TextView(this).apply {
            text = "Pronta. Toca em OUVIR e diz um comando."
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(16, 18, 16, 18)
            setBackgroundColor(panel)
        }
        val scroll = ScrollView(this).apply { addView(output) }
        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        addButton(layout, "🎙 OUVIR COMANDO", cyan) { listen() }
        addButton(layout, "🔊 REPETIR ÚLTIMA RESPOSTA", panel) {
            say(output.text.toString())
        }
        addButton(layout, "📱 DIAGNÓSTICO DO TELEFONE", panel) { diagnostics() }
        addButton(layout, "📂 ESCOLHER PASTA", panel) { chooseFolder() }
        setContentView(layout)
    }

    private fun addButton(parent: LinearLayout, label: String, color: Int, action: () -> Unit) {
        val b = Button(this).apply {
            text = label
            setTextColor(if (color == Color.rgb(53, 201, 255)) Color.BLACK else Color.WHITE)
            setBackgroundColor(color)
            textSize = 14f
            setOnClickListener { action() }
        }
        val p = LinearLayout.LayoutParams(-1, -2)
        p.topMargin = 12
        parent.addView(b, p)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("pt", "PT"))
            speechReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            if (speechReady) tts.setSpeechRate(0.95f)
        }
    }

    private fun say(message: String) {
        output.text = message
        if (::tts.isInitialized && speechReady) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "wyzen_reply")
        }
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            say("O reconhecimento de voz não está disponível neste telefone.")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), permissionRequest)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-PT")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-PT")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fala com a WYZEN P")
        }
        try { startActivityForResult(intent, voiceRequest) }
        catch (_: Exception) { say("Não consegui iniciar o reconhecimento de voz.") }
    }

    @Deprecated("Deprecated by Android API; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == voiceRequest && resultCode == RESULT_OK) {
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!words.isNullOrBlank()) handleCommand(words)
            else say("Não consegui perceber o comando.")
        }
    }

    private fun handleCommand(raw: String) {
        val command = raw.lowercase(Locale.ROOT)
        when {
            command.contains("bateria") -> battery()
            command.contains("diagnóstico") || command.contains("diagnostico") ||
                    command.contains("informações do telefone") || command.contains("informacoes do telefone") -> diagnostics()
            command.contains("contactos") || command.contains("contatos") -> {
                if (command.contains("quantos") || command.contains("contar")) countContacts()
                else {
                    val name = command.substringAfter("contacto", "").substringAfter("contato", "").trim()
                    if (name.isNotBlank()) searchContact(name)
                    else say("Diz: pesquisar contacto seguido do nome, ou quantos contactos tenho.")
                }
            }
            command.contains("ligar") || command.contains("chamar") -> {
                val name = command.substringAfter("ligar").substringAfter("chamar").trim()
                if (name.isNotBlank()) searchContact(name, true)
                else say("Diz o nome da pessoa a quem queres ligar.")
            }
            command.contains("sms") || command.contains("mensagem") -> say("Diz: mensagem para, seguido do nome e do texto. Para segurança, vais rever e enviar na aplicação de mensagens.")
            command.contains("ficheiro") || command.contains("pasta") || command.contains("documento") -> chooseFolder()
            command.contains("hora") -> say("São " + java.text.SimpleDateFormat("HH:mm", Locale("pt", "PT")).format(java.util.Date()))
            command.contains("data") -> say("Hoje é " + java.text.SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "PT")).format(java.util.Date()))
            else -> say("Ainda não tenho uma ação configurada para: $raw. Podes pedir bateria, diagnóstico, contactos, chamadas ou escolher uma pasta.")
        }
    }

    private fun battery() {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        say("A bateria está a $level por cento.")
    }

    private fun diagnostics() {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val info = "Telefone: ${Build.MANUFACTURER} ${Build.MODEL}. Android ${Build.VERSION.RELEASE}. Bateria: $level por cento."
        say(info)
    }

    private fun countContacts() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), permissionRequest)
            return
        }
        try {
            contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID), null, null, null)?.use {
                say("Tens ${it.count} contactos guardados.")
            } ?: say("Não consegui consultar os contactos.")
        } catch (_: Exception) { say("Não foi possível aceder aos contactos.") }
    }

    private fun searchContact(name: String, dial: Boolean = false) {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            say("Preciso de autorização para consultar os contactos.")
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), permissionRequest)
            return
        }
        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"), null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val foundName = it.getString(0) ?: "Contacto"
                    val number = it.getString(1) ?: ""
                    if (dial) {
                        say("Encontrei $foundName. Vou abrir o marcador para confirmares a chamada.")
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                    } else say("$foundName: $number")
                } else say("Não encontrei esse contacto.")
            } ?: say("Não consegui pesquisar os contactos.")
        } catch (_: Exception) { say("Ocorreu um erro ao pesquisar contactos.") }
    }

    private fun chooseFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        try {
            startActivityForResult(intent, 4103)
            say("Escolhe a pasta que queres disponibilizar à WYZEN P.")
        } catch (_: Exception) { say("Não foi possível abrir o seletor de pastas.") }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequest) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                say("Autorização concedida. Repete o comando.")
            } else say("A autorização não foi concedida.")
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
