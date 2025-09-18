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
    private val phrasesRestService = "x5.k" // search for 'v3/me/prefs'
    private val createSuccessResult = "xb.a\$b" // search for 'Success(successValue='
    private val chatRestService = "com.grindrapp.android.chat.data.datasource.api.service.ChatRestService"
    private val addSavedPhraseResponse =
        "com.grindrapp.android.chat.api.model.AddSavedPhraseResponse"
    private val phrasesResponse = "com.grindrapp.android.model.PhrasesResponse"
    private val phraseModel = "com.grindrapp.android.persistence.model.Phrase"

    override fun init() {
        val chatRestServiceClass = findClass(chatRestService)
        val createSuccess = findClass(createSuccessResult).constructors.firstOrNull() ?: return
        val phrasesRestServiceClass = findClass(phrasesRestService)

        findClass(RETROFIT_NAME).hook("create", HookStage.AFTER) { param ->
            val service = param.getResult()
            if (service != null) {
                param.setResult(when {
                    chatRestServiceClass.isAssignableFrom(service.javaClass) ->
                        createChatRestServiceProxy(service, createSuccess)

                    phrasesRestServiceClass.isAssignableFrom(service.javaClass) ->
                        createPhrasesRestServiceProxy(service, createSuccess)

                    else -> service
                })
            }
        }
    }

    private fun createChatRestServiceProxy(
        originalService: Any,
        createSuccess: Constructor<*>
    ): Any {
        val invocationHandler = Proxy.getInvocationHandler(originalService)
        return Proxy.newProxyInstance(
            originalService.javaClass.classLoader,
            arrayOf(findClass(chatRestService))
        ) { proxy, method, args ->
            when {
                method.isPOST("v3/me/prefs/phrases") -> {
                    val phrase = getObjectField(args[0], "phrase") as String

                    runBlocking {
                        val index = getCurrentPhraseIndex() + 1
                        addPhrase(index, phrase, 0, System.currentTimeMillis())
                        val response = findClass(addSavedPhraseResponse).constructors.first()
                            ?.newInstance(index.toString())
                        createSuccess.newInstance(response)
                    }
                }

                method.isDELETE("v3/me/prefs/phrases/{id}") -> {
                    runBlocking {
                        val index = getCurrentPhraseIndex()
                        deletePhrase(index)
                        createSuccess.newInstance(Unit)
                    }
                }

                method.isPOST("v4/phrases/frequency/{id}") -> {
                    runBlocking {
                        val index = getCurrentPhraseIndex()
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
        val invocationHandler = Proxy.getInvocationHandler(originalService)
        return Proxy.newProxyInstance(
            originalService.javaClass.classLoader,
            arrayOf(findClass(phrasesRestService))
        ) { proxy, method, args ->
            when {
                method.isGET("v3/me/prefs") -> {
                    runBlocking {
                        val currentPhrases = getPhraseList()
                        val phrases = currentPhrases.associateWith { phrase ->
                            GrindrPlus.loadClass(phraseModel).constructors.first()?.newInstance(
                                phrase.phraseId.toString(), phrase.text, phrase.timestamp, phrase.frequency
                            )
                        }
                        val phrasesResponse = findClass(phrasesResponse)
                            .constructors.find { it.parameterTypes.size == 1 }?.newInstance(phrases)
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