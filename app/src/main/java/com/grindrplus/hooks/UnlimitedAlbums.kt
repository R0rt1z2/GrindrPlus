package com.grindrplus.hooks

import com.grindrplus.GrindrPlus
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.getObjectField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class UnlimitedAlbums: Hook("Unlimited albums",
    "Allow to be able to view unlimited albums") {
    private val albumModel = "com.grindrapp.android.model.Album"
    private val albumContent = "com.grindrapp.android.model.AlbumContent"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val albumCache = mutableMapOf<Any, MutableMap<String, Any>>()

    override fun init() {
        val albumModelClass = findClass(albumModel)

        findClass(albumContent)
            ?.hook("getRemainingViews", HookStage.AFTER) { param ->
                param.setResult(Int.MAX_VALUE)
            }

        albumModelClass?.hook(
            "getAlbumViewable", HookStage.AFTER) { param ->
                param.setResult(true)
            }

        albumModelClass?.hook("getContent", HookStage.AFTER) { param ->
            /**
             * If we've already processed this object, return the cached value
             */
            val cachedContent = albumCache[param.thisObject()]?.get("content")
            if (cachedContent != null) {
                param.setResult(cachedContent)
                return@hook
            }

            val contentList = param.getResult() as List<*>

            /**
             * The first step is to find out if we're dealing with cover
             * images or not. If we are, we don't have to do anything.
             *
             * Covers usually have only one image, so we can just
             * detect if it's a cover or not by checking the size of
             * the list.
             */
            if (contentList.size == 1) return@hook

            val albumId = getObjectField(param.thisObject(), "albumId") as Long
            if (contentList.isNotEmpty()) {
                // TODO: Check for race conditions
                scope.launch {
                    /**
                     * At this point, we know we're dealing with an actual
                     * album. We can now proceed to save the contents if
                     * we haven't already.
                     */
                    if (GrindrPlus.database.getContentIdListByAlbumId(albumId).isNotEmpty()) {
                        /**
                         * It's possible that the album has been viewed before
                         * but the user added more images to it. In this case,
                         * we should update the album contents.
                         */
                        deleteContentsByList(albumId)
                    }
                    saveContentsByList(albumId, contentList)
                }
            } else {
                /**
                 * If the content list is empty, try to retrieve the contents
                 * from the database and set them as the new content list.
                 */
                val savedContentsList = GrindrPlus.database.getContentIdListByAlbumId(albumId)
                if (savedContentsList.isNotEmpty()) {
                    val newContentsList = mutableListOf<Any>()

                    for (element in savedContentsList) {
                        val savedContent = GrindrPlus.database.getAlbumContent(element)
                        if (savedContent != null) {
                            loadClass(albumContent)?.constructors?.first()?.newInstance(
                                savedContent.getAsLong("contentId"),
                                savedContent.getAsString("contentType"),
                                savedContent.getAsString("url"),
                                savedContent.getAsBoolean("isProcessing"),
                                savedContent.getAsString("thumbUrl"),
                                savedContent.getAsString("coverUrl"),
                                savedContent.getAsInteger("remainingViews")
                            )?.let { newContentsList.add(it) }
                        }
                    }

                    //setObjectField(param.thisObject(), "contentCount", newContentsList.size)
                    albumCache.getOrPut(param.thisObject()) { mutableMapOf() }["content"] =
                        newContentsList

                    // TODO: Should we also hook other getters such as "getContentCount"?

                    param.setResult(newContentsList)
                }
            }
        }
    }

    override fun cleanup() {
        scope.cancel()
    }

    private fun deleteContentsByList(albumId: Long) {
        val contents = GrindrPlus.database.getContentIdListByAlbumId(albumId)
        for (element in contents) {
            GrindrPlus.database.deleteAlbumContent(element)
        }
    }

    private fun saveContentsByList(albumId: Long, content: List<*>) {
        for (element in content) {
            if (getObjectField(element, "url") == null) {
                continue // Skip invalid / empty content
            }
            GrindrPlus.database.addAlbumContent(
                getObjectField(element, "contentId") as Long,
                albumId,
                getObjectField(element, "contentType") as String,
                getObjectField(element, "url") as String,
                getObjectField(element, "isProcessing") as Boolean,
                getObjectField(element, "thumbUrl") as String,
                getObjectField(element, "coverUrl") as String,
                getObjectField(element, "remainingViews") as Int,
            )
        }
    }

}