package com.eljaviluki.grindrplus

object Obfuscation {
    object GApp {
        object api {
            private const val _api = Constants.GRINDR_PKG + ".api"

            const val ChatRestService = "$_api.ChatRestService"

            object ChatRestService_ {
                //Annotated with @POST("v3/me/prefs/phrases")
                const val addSavedPhrase = "a"

                //Annotated with @DELETE("v3/me/prefs/phrases/{id}")
                const val deleteSavedPhrase = "r"

                //Annotated with @POST("v4/phrases/frequency/{id}")
                const val increaseSavedPhraseClickCount = "G"
            }

            const val PhrasesRestService = "$_api.n"

            object PhrasesRestService_ {
                //Annotated with @GET("v3/me/prefs"), returns PhrasesResponse
                const val getSavedPhrases = "a"
            }

            //Contains @POST("/v3/logging/mobile/logs")
            const val AnalyticsRestService = "$_api.c"
        }

        object base {
            private const val _base = Constants.GRINDR_PKG + ".base"

            object Experiment {
                private const val _experiment = "$_base.experiment"

                const val IExperimentsManager = "$_experiment.a"
            }
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
            const val BlockInteractor = "$_manager.o"

            object BlockInteractor_ {
                //Calls blockstream$2
                const val blockstream = "n"

                //Calls removeProfilesFromDbTables$2$1
                const val processAndRemoveBlockedProfiles = "D"

                //Calls unblockProfile$2
                const val unblockProfile = "G"
            }
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

                const val Profile = "$_model.Profile"
                const val Phrase = "$_model.Phrase"
            }

            object repository {
                private const val _repository = "$_persistence.repository"

                const val ChatRepo = "$_repository.ChatRepo"

                object ChatRepo_ {
                    const val checkMessageForVideoCall = "checkMessageForVideoCall"
                    const val deleteChatMessageFromLessThanOrEqualToTimestamp = "deleteChatMessageFromLessThanOrEqualToTimestamp"
                }

                const val ProfileRepo = "$_repository.ProfileRepo"

                object ProfileRepo_ {
                    const val delete = "delete"
                    const val recordProfileView = "recordProfileView"
                }
            }
        }

        object R {
            private const val _R = Constants.GRINDR_PKG

            const val color = "$_R.m0"

            object color_ {
                const val grindr_gold_star_gay = "G"
                const val grindr_pure_white = "W"
            }

            const val id = "$_R.q0"

            object id_ {
                const val fragment_favorite_recycler_view = "Ib"
                const val profile_distance = "rk"
                const val profile_online_now_icon = "pl"
                const val profile_last_seen = "Tk"
                const val profile_note_icon = "nl"
                const val profile_display_name = "nk"
            }
        }

        object storage {
            private const val _storage = Constants.GRINDR_PKG + ".storage"

            const val UserSession = "$_storage.x0"

            const val IUserSession = "$_storage.UserSession"

            object IUserSession_ {
                const val hasFeature_feature = "a"
                const val isFree = "r"
                const val isNoXtraUpsell = "g"
                const val isXtra = "o"
                const val isUnlimited = "x"
                const val getProfileId = "e"
            }
        }

        object ui {
            private const val _ui = Constants.GRINDR_PKG + ".ui"

            object profileV2 {
                private const val _profileV2 = "$_ui.profileV2"

                const val ProfileFieldsView = "$_profileV2.ProfileFieldsView"

                object ProfileFieldsView_ {
                    const val setProfile = "setProfile"
                }

                const val ProfilesViewModel = "$_profileV2.ProfilesViewModel"

                object ProfilesViewModel_ {
                    const val recordProfileViewsForViewedMeService = "X1"
                }

                object model {
                    private const val _model = "$_profileV2.model"

                    const val Profile = "$_model.h"

                    object Profile_ {
                        const val getProfileId = "Z"
                    }
                }
            }

            object chat {
                private const val _chat = "$_ui.chat"

                const val ChatBaseFragmentV2 = "$_chat.ChatBaseFragmentV2"

                object ChatBaseFragmentV2_ {
                    const val _canBeUnsent = "X1"
                }
            }
        }

        object utils {
            private const val _utils = Constants.GRINDR_PKG + ".utils"

            const val ProfileUtils = "$_utils.ProfileUtilsV2"

            object ProfileUtils_ {
                //Look for value of 600000
                const val onlineIndicatorDuration = "g"
            }
        }

        object view {
            private const val _view = Constants.GRINDR_PKG + ".view"

            const val ExtendedProfileFieldView = "$_view.y4"

            object ExtendedProfileFieldView_ {
                const val setLabel = "l"
                const val setValue = "n"
            }

            const val TapsAnimLayout = "$_view.TapsAnimLayout"

            object TapsAnimLayout_ {
                const val tapType = "i"

                const val getCanSelectVariants = "getCanSelectVariants"
                const val getDisableVariantSelection = "getDisableVariantSelection"
                const val setTapType = "S"
            }
        }

        object xmpp {
            private const val _xmpp = Constants.GRINDR_PKG + ".xmpp"

            const val ChatMessageManager = "$_xmpp.ChatMessageManager"

            object ChatMessageManager_ {
                const val handleIncomingChatMessage = "p"
                const val handleRetryChatMessage = "B"
            }
        }
    }
}