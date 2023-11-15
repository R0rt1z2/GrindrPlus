package com.grindrplus

object Obfuscation {
    object GApp {
        object taps {
            private const val _taps = Constants.GRINDR_PKG + ".taps"
            object model {
                private const val _model = "$_taps.model"

                const val TapType = "$_model.TapType"
            }
        }

        object api {
            private const val _api = Constants.GRINDR_PKG + ".api"

            const val ChatRestService = "$_api.ChatRestService"

            object ChatRestService_ {
                //Annotated with @POST("v3/me/prefs/phrases")
                const val addSavedPhrase = "a"

                //Annotated with @DELETE("v3/me/prefs/phrases/{id}")
                const val deleteSavedPhrase = "s"

                //Annotated with @POST("v4/phrases/frequency/{id}")
                const val increaseSavedPhraseClickCount = "H"
            }

            const val PhrasesRestService = "v3.j"

            object PhrasesRestService_ {
                //Annotated with @GET("v3/me/prefs"), returns PhrasesResponse
                const val getSavedPhrases = "a"
            }

            //Contains @POST("/v3/logging/mobile/logs")
            const val AnalyticsRestService = "v3.b"
        }

        object experiment {
            private const val _experiment = Constants.GRINDR_PKG + ".experiment"

            const val Experiments = "$_experiment.f"

            object Experiments_ {
                const val uncheckedIsEnabled_expMgr = "c"
            }
        }

        object favorites {
            private const val _favorites = Constants.GRINDR_PKG + ".favorites"

            const val FavoritesFragment = "$_favorites.FavoritesFragment"
        }

        object manager {
            private const val _manager = Constants.GRINDR_PKG + ".manager"
            const val BlockInteractor = "$_manager.n"


        }

        object model {
            private const val _model = Constants.GRINDR_PKG + ".model"

            const val ExpiringImageBody = "$_model.ExpiringImageBody"

            object ExpiringImageBody_ {
                const val getDuration = "getDuration"
            }

            const val ExpiringPhotoStatusResponse = "$_model.ExpiringPhotoStatusResponse"

            object ExpiringPhotoStatusResponse_ {
                const val getTotal = "getTotal"
                const val getAvailable = "getAvailable"
            }

            const val Feature = "$_model.Feature"

            object Feature_ {
                const val isGranted = "isGranted"
            }

            const val UpsellsV8 = "$_model.UpsellsV8"

            object UpsellsV8_ {
                const val getMpuFree = "getMpuFree"
                const val getMpuXtra = "getMpuXtra"
            }

            const val Inserts = "$_model.Inserts"

            object Inserts_ {
                const val getMpuFree = "getMpuFree"
                const val getMpuXtra = "getMpuXtra"
            }

            const val AddSavedPhraseRequest = "$_model.AddSavedPhraseRequest"
            const val AddSavedPhraseResponse = "$_model.AddSavedPhraseResponse"
            const val PhrasesResponse = "$_model.PhrasesResponse"
        }

        object network {
            private const val _network = Constants.GRINDR_PKG + ".network"

            object either {
                private const val _either = "$_network.either"

                const val ResultHelper = "$_either.b"

                object ResultHelper_ {
                    const val createSuccess = "b"
                }
            }
        }

        object persistence {
            private const val _persistence = Constants.GRINDR_PKG + ".persistence"

            object model {
                private const val _model = "$_persistence.model"

                const val ChatMessage = "$_model.ChatMessage"

                object ChatMessage_ {
                    const val TAP_TYPE_NONE = "TAP_TYPE_NONE"
                    const val getType = "getType"
                    const val setType = "setType"
                    const val setMessageId = "setMessageId"
                    const val setSender = "setSender"
                    const val setRecipient = "setRecipient"
                    const val setStanzaId = "setStanzaId"
                    const val setConversationId = "setConversationId"
                    const val setTimestamp = "setTimestamp"
                    const val setBody = "setBody"

                    const val clone = "clone"
                }

                const val BlockedProfile = "$_model.BlockedProfile"

                object BlockedProfile_ {
                    const val getProfileId = "getProfileId"
                }

                const val Profile = "$_model.Profile"
                const val Phrase = "$_model.Phrase"
            }

            object repository {
                private const val _repository = "$_persistence.repository"

                const val ChatRepo = "$_repository.ChatRepo"

                object ChatRepo_ {
                    const val checkMessageForVideoCall = "checkMessageForVideoCall"
                    const val deleteChatMessageFromLessThanOrEqualToTimestamp = "deleteChatMessageFromLessThanOrEqualToTimestamp"
                    const val deleteChatMessageFromConversationId = "deleteChatMessageFromConversationId"
                    const val deleteChatMessageListFromConversationId = "deleteChatMessageListFromConversationId"
                    const val deleteMessagesByConversationIds = "deleteMessagesByConversationIds"
                }

                const val ProfileRepo = "$_repository.ProfileRepo"
                // com.grindrapp.android.persistence.repository.ProfileRepo

                object ProfileRepo_ {
                    const val delete = "delete"
                    const val recordProfileView = "recordProfileView"
                }

                const val ConversationRepo = "$_repository.ConversationRepo"
                object ConversationRepo_ {
                    const val deleteConversation = "deleteConversation"
                    const val deleteConversations = "deleteConversations"
                }

                const val IncomingChatMarkerRepo = "$_repository.IncomingChatMarkerRepo"
                object IncomingChatMarkerRepo_ {
                    const val deleteIncomingChatMarker = "deleteIncomingChatMarker"
                }

                const val BlockRepo = "$_repository.BlockRepo"

                // com.grindrapp.android.persistence.repository.BlockRepo
                object BlockRepo_ {
                    const val add = "add"
                    const val delete = "delete"
                }
            }
        }

        object R {
            private const val _R = Constants.GRINDR_PKG

            const val color = "$_R.o0"

            object color_ {
                const val grindr_gold_star_gay = "G"
                const val grindr_pure_white = "V"
            }

            const val id = "$_R.s0"

            object id_ {
                const val fragment_favorite_recycler_view = "rb"
                const val profile_distance = "ik"
                const val profile_online_now_icon = "wl"
                const val profile_last_seen = "el"
                const val profile_note_icon = "ul"
                const val profile_display_name = "dk"
            }
        }

        object storage {
            private const val _storage = Constants.GRINDR_PKG + ".storage"

            const val UserSession = "$_storage.b"

            const val IUserSession = "$_storage.UserSession"

            object IUserSession_ {
                const val hasFeature_feature = "a"
                const val isFree = "r"
                const val isNoPlusUpsell = "A"
                const val isNoXtraUpsell = "h"
                const val isPlus = "y"
                const val isXtra = "p"
                const val isUnlimited = "x"
                const val getProfileId = "e"
            }
        }

        object profile {
            val _profile = "com.grindrapp.android.profile"
            object experiments {
                val _experiments = "$_profile.experiments"

                val InaccessibleProfileManager =  "$_experiments.InaccessibleProfileManager"

                object InaccessibleProfileManager_ {

                    val isProfileEnabled =  "a"
                    val shouldShowProfile = "b"
                }
            }
        }
        object ui {
            private const val _ui = Constants.GRINDR_PKG + ".ui"

            object profileV2 {
                private const val _profileV2 = "$_ui.profileV2"

                val ProfileFieldsView = "$_profileV2.ProfileFieldsView"
                val ProfileQuickbarView = "$_profileV2.ProfileQuickbarView"

                object ProfileFieldsView_ {
                    const val setProfile = "setProfile"
                }

                const val ProfilesViewModel = "$_profileV2.ProfilesViewModel"

                // com.grindrapp.android.ui.profileV2.ProfilesViewModel

                object ProfilesViewModel_ {
                    const val recordProfileViewsForViewedMeService = "r2"
                }

                object model {
                    private const val _model = "$_profileV2.model"

                    const val Profile = "$_model.Profile"

                    object Profile_ {
                        const val getProfileId = "getProfileId"
                    }
                }
            }

            object chat {
                private const val _chat = "$_ui.chat"

                const val ChatBaseFragmentV2 = "$_chat.ChatBaseFragmentV2"
                const val BlockViewModel = "$_chat.BlockViewModel"

                object ChatBaseFragmentV2_ {
                    const val _canBeUnsent = "B1"
                }
            }
        }

        object utils {
            private const val _utils = Constants.GRINDR_PKG + ".utils"

            const val ProfileUtils = "$_utils.ProfileUtilsV2"
            // com.grindrapp.android.utils.ProfileUtilsV2

            object ProfileUtils_ {
                //Look for value of 600000
                const val onlineIndicatorDuration = "d"
            }
        }

        object view {
            private const val _view = Constants.GRINDR_PKG + ".view"


            const val TapsAnimLayout = "$_view.TapsAnimLayout"

            object TapsAnimLayout_ {
                const val tapType = "i"

                const val getCanSelectVariants = "getCanSelectVariants"
                const val getDisableVariantSelection = "getDisableVariantSelection"
                const val setTapType = "k"
            }
        }

        object xmpp {
            private const val _xmpp = Constants.GRINDR_PKG + ".xmpp"

            const val ChatMessageManager = "$_xmpp.ChatMessageManager"

            object ChatMessageManager_ {
                const val handleIncomingChatMessage = "a"
            }

            const val ChatMarkersManager = "$_xmpp.a"

            object ChatMarkersManager_ {
                const val addDisplayedExtension = "d"
                const val addReceivedExtension = "f"
            }
        }
    }
}