package com.eljaviluki.grindrplus

object Obfuscation {
    object GApp {
        object base {
            private const val _base = Constants.GRINDR_PKG + ".base"

            object Experiment {
                private const val _experiment = "$_base.experiment"

                const val IExperimentsManager = "$_experiment.c"
            }
        }

        object experiment {
            private const val _experiment = Constants.GRINDR_PKG + ".experiment"

            const val Experiments = "$_experiment.a"
            object Experiments_ {
                const val uncheckedIsEnabled_expMgr = "c"
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
        }

        object persistence {
            private const val _persistence = Constants.GRINDR_PKG + ".persistence"

            object model {
                private const val _model = "$_persistence.model"

                const val ChatMessage = "$_model.ChatMessage"
                object ChatMessage_ {
                    const val TAP_TYPE_NONE = "TAP_TYPE_NONE"
                }

                const val Profile = "$_model.Profile"
            }

            object repository {
                private const val _repository = "$_persistence.repository"

                const val ChatRepo = "$_repository.ChatRepo"
                object ChatRepo_ {
                    const val checkMessageForVideoCall = "checkMessageForVideoCall"
                }
            }
        }

        object R {
            private const val _R = Constants.GRINDR_PKG

            const val color = "$_R.h0"
            object color_ {
                const val grindr_gold_star_gay = "x"
                const val grindr_pure_white = "M"
            }
        }

        object storage {
            private const val _storage = Constants.GRINDR_PKG + ".storage"

            const val UserSession = "$_storage.w0"
            const val UserSession2 = "$_storage.x0"

            const val IUserSession = "$_storage.IUserSession"
            object IUserSession_ {
                const val hasFeature_feature = "a"
                const val isFree = "j"
                const val isNoXtraUpsell = "p"
                const val isXtra = "g"
                const val isUnlimited = "q"
            }
        }

        object ui {
            private const val _ui = Constants.GRINDR_PKG + ".ui"

            object profileV2 {
                private const val _profileV2 = "$_ui.profileV2"

                const val ProfileFieldsView = "$_profileV2.ProfileFieldsView"
                object ProfileFieldsView_ {
                    const val setProfile = "h"
                }
            }
        }

        object utils {
            private const val _utils = Constants.GRINDR_PKG + ".utils"

            const val ProfileUtils = "$_utils.u0"
            object ProfileUtils_ {
                const val onlineIndicatorDuration = "b"
            }
        }

        object view {
            private const val _view = Constants.GRINDR_PKG + ".view"

            const val ExtendedProfileFieldView = "$_view.d4"
            object ExtendedProfileFieldView_ {
                const val setLabel = "l"
                const val setValue = "n"
            }

            const val TapsAnimLayout = "$_view.TapsAnimLayout"
            object TapsAnimLayout_ {
                const val tapType = "h"

                const val getCanSelectVariants = "getCanSelectVariants"
                const val setTapType = "S"
            }
        }
    }
}