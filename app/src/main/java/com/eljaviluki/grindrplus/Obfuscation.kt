package com.eljaviluki.grindrplus

class Obfuscation {
    class GApp {
        object base {
            private const val _base = Constants.GRINDR_PKG + ".base"

            object Experiment {
                private const val _experiment = "$_base.g"

                const val IExperimentManager = "$_experiment.b"
            }
        }

        object experiment {
            private const val _experiment = Constants.GRINDR_PKG + ".experiment"

            const val Experiments = "$_experiment.Experiments"
            object Experiments_ {
                const val uncheckedIsEnabled_expMgr = "a"
            }
        }

        object model {
            private const val _model = Constants.GRINDR_PKG + ".model"

            const val ExpiringPhotoStatusResponse = "$_model.ExpiringPhotoStatusResponse"
            object ExpiringPhotoStatusResponse_ {
                const val getTotal = "getTotal"
                const val getAvailable = "getAvailable"
            }

            const val Feature = "$_model.Feature"
            object Feature_ {
                const val isGranted = "isGranted"
                const val isNotGranted = "isNotGranted"
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
            private const val _R = Constants.GRINDR_PKG + ".m"

            const val color = "$_R\$d"
            object color_ {
                const val grindr_pure_white = "L"
            }
        }

        object storage {
            private const val _storage = Constants.GRINDR_PKG + ".storage"

            const val UserSession = "$_storage.ai"
            const val UserSession2 = "$_storage.aj"

            const val IUserSession = "$_storage.IUserSession"
            object IUserSession_ {
                const val hasFeature_feature = "a"
                const val isFree = "i"
                const val isNoXtraUpsell = "j"
                const val isXtra = "k"
                const val isUnlimited = "l"
            }
        }

        object ui {
            private const val _ui = Constants.GRINDR_PKG + ".ui"

            object profileV2 {
                private const val _profileV2 = "$_ui.profileV2"

                const val ProfileFieldsView = "$_profileV2.ProfileFieldsView"
            }
        }

        object utils {
            private const val _utils = Constants.GRINDR_PKG + ".utils"

            const val Styles = "$_utils.bh"
            object Styles_ {
                const val INSTANCE = "a"
                const val _maybe_pureWhite = "f"
            }

            const val ProfileUtils = "$_utils.at"
            object ProfileUtils_ {
                const val onlineIndicatorDuration = "b"
            }
        }

        object view {
            private const val _view = Constants.GRINDR_PKG + ".view"

            const val ExtendedProfileFieldView = "$_view.bv"
            object ExtendedProfileFieldView_ {
                const val setLabel = "a"
                const val setValue = "b"
            }

            const val TapsAnimLayout = "$_view.TapsAnimLayout"
            object TapsAnimLayout_ {
                const val tapType = "g"

                const val getCanSelectVariants = "getCanSelectVariants"
                const val setTapType = "a"
            }
        }
    }
}