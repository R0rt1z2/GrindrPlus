# Anti Block

This feature detects when the user has been blocked and sends a notification
from the Gplus app. The current implementation relies on websocket notifications,
so it works only when the app is open. The notification received is `chat.v1.conversation.delete`
and it is used also when the other user deletes his account.


## Conversations database
Conversations are also stored in the `chat_conversations`, `_messages` etc. tables. 
Conversations get deleted by `ConversationRepo::updateConversationsInternal` 
and/or `::purgeInvalidatedConversations` when the event arrives or when the app refreshes the conversations. 
The method has an argument `isDeleteMissingConversations`, which if set to false, keeps
the conversation in the db. This however is not enough to keep it visible in the app.
The app uses primarily conversation list from the rest service.

If we wanted to keep the conversations visible, we would need to hook the rest service and inject
these not-deleted conversations into the result.

This should do to prevent the conversations from being deleted. This however also applies when
the user himself deletes the conversation - it stays in the db.
```kotlin
findClass("com.grindrapp.android.persistence.repository.ConversationRepo")
    .hook("purgeInvalidatedConversations", HookStage.BEFORE) { param ->
        val emptyList = emptyList<String>()
        param.setArg(0, emptyList)
    }


findClass("com.grindrapp.android.persistence.repository.ConversationRepo\$updateConversationsInternal\$2")
    .hookConstructor(HookStage.BEFORE) { param ->
        param.setArg(1, false) // search for '$isDeleteMissingConversations'
    }
```