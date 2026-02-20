package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.persistence.model.SavedPhraseEntity
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.RetrofitUtils.RETROFIT_NAME
import com.grindrplus.utils.RetrofitUtils.isDELETE
import com.grindrplus.utils.RetrofitUtils.isGET
import com.grindrplus.utils.RetrofitUtils.isPOST
import com.grindrplus.utils.hook
import com.grindrplus.core.*
import de.robv.android.xposed.XposedHelpers.getObjectField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.reflect.Constructor
import java.lang.reflect.Proxy

class LocalSavedPhrases : Hook(
    "Local saved phrases",
    "Save unlimited phrases locally"
) {
    private val phrasesRestService = "sk.j" // search for 'v3/me/prefs'
    private val createSuccessResult = "g10.a\$b" // search for 'Success(successValue='
    private val chatRestService = "vn.a" // search for '"v4/chat/conversation/{conversationId}"'
    private val addSavedPhraseResponse =
        "com.grindrapp.android.chat.data.datasource.api.model.AddSavedPhraseResponse"
    private val phrasesResponse = "com.grindrapp.android.model.PhrasesResponse"
    private val phraseModel = "com.grindrapp.android.persistence.model.Phrase"

    override fun init() {
        logi("Initializing Local Saved Phrases Hook...")

        val chatRestServiceClass = findClass(chatRestService)
        val createSuccess = findClass(createSuccessResult).constructors.firstOrNull() ?: run {
            loge("Failed to find Success result constructor (check obfuscation)")
            return
        }
        val phrasesRestServiceClass = findClass(phrasesRestService)

        findClass(RETROFIT_NAME).hook("create", HookStage.AFTER) { param ->
            val service = param.getResult()
            if (service != null) {
                param.setResult(when {
                    chatRestServiceClass.isAssignableFrom(service.javaClass) -> {
                        logd("Proxying ChatRestService for local storage")
                        createChatRestServiceProxy(service, createSuccess)
                    }

                    phrasesRestServiceClass.isAssignableFrom(service.javaClass) -> {
                        logd("Proxying PhrasesRestService for local retrieval")
                        createPhrasesRestServiceProxy(service, createSuccess)
                    }

                    else -> service
                })
            }
        }
    }

    private fun createChatRestServiceProxy(
        originalService: Any,
        createSuccess: Constructor<*>
    ): Any {
        val savedPhraseConstructor = findClass(addSavedPhraseResponse).constructors.first()

        val invocationHandler = Proxy.getInvocationHandler(originalService)

        return Proxy.newProxyInstance(
            originalService.javaClass.classLoader,
            arrayOf(findClass(chatRestService))
        ) { proxy, method, args ->
            when {
                method.isPOST("v3/me/prefs/phrases") -> {
                    val phrase = getObjectField(args[0], "phrase") as String
                    logi("Adding new local phrase: \"$phrase\"")

                    runBlocking {
                        val index = getCurrentPhraseIndex() + 1
                        addPhrase(index, phrase, 0, System.currentTimeMillis())
                        logs("Phrase saved locally with ID: $index")

                        val response = savedPhraseConstructor?.newInstance(index.toString())
                        createSuccess.newInstance(response)
                    }
                }

                method.isDELETE("v3/me/prefs/phrases/{id}") -> {
                    val id = args[0] as? String ?: "unknown"
                    logi("Deleting local phrase ID: $id")

                    runBlocking {
                        val index = id.toLongOrNull() ?: getCurrentPhraseIndex()
                        deletePhrase(index)
                        logs("Deleted local phrase ID: $index")
                        createSuccess.newInstance(Unit)
                    }
                }

                method.isPOST("v4/phrases/frequency/{id}") -> {
                    val id = args[0] as? String ?: "0"
                    logd("Incrementing usage frequency for ID: $id")

                    runBlocking {
                        val index = id.toLongOrNull() ?: 0L
                        val phrase = getPhrase(index)
                        if (phrase != null) {
                            updatePhrase(
                                index,
                                phrase.text,
                                phrase.frequency + 1,
                                System.currentTimeMillis()
                            )
                        }
                        createSuccess.newInstance(Unit)
                    }
                }

                else -> invocationHandler.invoke(proxy, method, args)
            }
        }
    }

    private fun createPhrasesRestServiceProxy(
        originalService: Any,
        createSuccess: Constructor<*>
    ): Any {
        val phraseModelConstructor = GrindrPlus.loadClass(phraseModel).constructors.first()
        val phraseResponseConstructor = findClass(phrasesResponse)
            .constructors.find { it.parameterTypes.size == 1 }

        val invocationHandler = Proxy.getInvocationHandler(originalService)
        return Proxy.newProxyInstance(
            originalService.javaClass.classLoader,
            arrayOf(findClass(phrasesRestService))
        ) { proxy, method, args ->
            when {
                method.isGET("v3/me/prefs") -> {
                    logd("Intercepted GET v3/me/prefs - pulling from local database")
                    runBlocking {
                        val currentPhrases = getPhraseList()
                        logd("Loaded ${currentPhrases.size} local phrases")

                        val phrases = currentPhrases.associateWith { phrase ->
                            phraseModelConstructor?.newInstance(
                                phrase.phraseId.toString(), phrase.text, phrase.timestamp, phrase.frequency
                            )
                        }
                        val phrasesResponse = phraseResponseConstructor?.newInstance(phrases)
                        createSuccess.newInstance(phrasesResponse)
                    }
                }

                else -> invocationHandler.invoke(proxy, method, args)
            }
        }
    }

    private suspend fun getPhraseList(): List<SavedPhraseEntity> = withContext(Dispatchers.IO) {
        return@withContext GrindrPlus.database.savedPhraseDao().getPhraseList()
    }

    private suspend fun getPhrase(phraseId: Long): SavedPhraseEntity? = withContext(Dispatchers.IO) {
        return@withContext GrindrPlus.database.savedPhraseDao().getPhrase(phraseId)
    }

    private suspend fun getCurrentPhraseIndex(): Long = withContext(Dispatchers.IO) {
        return@withContext GrindrPlus.database.savedPhraseDao().getCurrentPhraseIndex() ?: 0L
    }

    private suspend fun addPhrase(phraseId: Long, text: String, frequency: Int, timestamp: Long) = withContext(Dispatchers.IO) {
        val phrase = SavedPhraseEntity(
            phraseId = phraseId,
            text = text,
            frequency = frequency,
            timestamp = timestamp
        )
        GrindrPlus.database.savedPhraseDao().upsertPhrase(phrase)
    }

    private suspend fun updatePhrase(phraseId: Long, text: String, frequency: Int, timestamp: Long) = withContext(Dispatchers.IO) {
        val phrase = SavedPhraseEntity(
            phraseId = phraseId,
            text = text,
            frequency = frequency,
            timestamp = timestamp
        )
        GrindrPlus.database.savedPhraseDao().upsertPhrase(phrase)
    }

    private suspend fun deletePhrase(phraseId: Long) = withContext(Dispatchers.IO) {
        GrindrPlus.database.savedPhraseDao().deletePhrase(phraseId)
    }
}