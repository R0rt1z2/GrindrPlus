package com.grindrplus.hooks

import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import com.grindrplus.GrindrPlus
import com.grindrplus.core.Config
import com.grindrplus.ui.Utils
import com.grindrplus.utils.Hook
import com.grindrplus.utils.HookStage
import com.grindrplus.utils.hook
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.getObjectField
import kotlin.math.roundToInt
import androidx.core.view.isGone

class Favorites : Hook(
    "Favorites",
    "Customize layout for the favorites tab"
) {
    private val recyclerViewLayoutParams =
        "androidx.recyclerview.widget.RecyclerView\$LayoutParams"
    private val favoritesFragment = "com.grindrapp.android.favorites.presentation.ui.FavoritesFragment"

    override fun init() {
        val recyclerViewLayoutParamsConstructor = findClass(recyclerViewLayoutParams)
            .getDeclaredConstructor(Int::class.java, Int::class.java)

        findClass(favoritesFragment)
            .hook("onViewCreated", HookStage.AFTER) { param ->
                val columnsNumber = (Config.get("favorites_grid_columns", 3) as Number).toInt()
                val view = param.arg<View>(0)
                val recyclerView = view.findViewById<View>(
                    Utils.getId(
                        "fragment_favorite_recycler_view",
                        "id", GrindrPlus.context
                    )
                )
                val gridLayoutManager = callMethod(
                    recyclerView, "getLayoutManager"
                )

                callMethod(gridLayoutManager, "setSpanCount", columnsNumber)
                val adapter = callMethod(recyclerView, "getAdapter")

                adapter::class.java
                    .hook("onBindViewHolder", HookStage.AFTER) { param ->
                        val size = GrindrPlus.context
                            .resources.displayMetrics.widthPixels / columnsNumber
                        val rootLayoutParams = recyclerViewLayoutParamsConstructor
                            ?.newInstance(size, size) as? ViewGroup.LayoutParams

                        val itemView = getObjectField(
                            param.arg(
                                0
                            ), "itemView"
                        ) as View
                        itemView.layoutParams = rootLayoutParams

                        val distanceTextView =
                            itemView.findViewById<TextView>(
                                Utils.getId(
                                    "profile_distance", "id", GrindrPlus.context
                                )
                            )

                        var linearLayout = distanceTextView.parent as LinearLayout
                        linearLayout.orientation = LinearLayout.VERTICAL
                        linearLayout.children.forEach { child ->
                            child.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                        }

                        distanceTextView.gravity = Gravity.START

                        val profileOnlineNowIcon = itemView.findViewById<ImageView>(
                            Utils.getId(
                                "profile_online_now_icon",
                                "id", GrindrPlus.context
                            )
                        )
                        val profileLastSeen = itemView.findViewById<TextView>(
                            Utils.getId("profile_last_seen", "id", GrindrPlus.context)
                        )

                        val lastSeenLayoutParams = profileLastSeen
                            .layoutParams as LinearLayout.LayoutParams
                        if (profileOnlineNowIcon.isGone) {
                            lastSeenLayoutParams.topMargin = 0
                        } else {
                            lastSeenLayoutParams.topMargin = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 5f,
                                GrindrPlus.context.resources.displayMetrics
                            ).roundToInt()
                        }
                        profileLastSeen.layoutParams = lastSeenLayoutParams

                        val profileNoteIcon = itemView.findViewById<ImageView>(
                            Utils.getId(
                                "profile_note_icon",
                                "id", GrindrPlus.context
                            )
                        )
                        val profileDisplayName = itemView.findViewById<TextView>(
                            Utils.getId(
                                "profile_display_name",
                                "id", GrindrPlus.context
                            )
                        )

                        val displayNameLayoutParams = profileDisplayName
                            .layoutParams as LinearLayout.LayoutParams
                        if (profileNoteIcon.isGone) {
                            displayNameLayoutParams.topMargin = 0
                        } else {
                            displayNameLayoutParams.topMargin = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 4f,
                                GrindrPlus.context.resources.displayMetrics
                            ).roundToInt()
                        }
                        profileDisplayName.layoutParams = displayNameLayoutParams
                    }
            }
    }
}
