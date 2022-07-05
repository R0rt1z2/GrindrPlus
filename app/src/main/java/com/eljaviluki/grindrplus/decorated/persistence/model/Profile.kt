package com.eljaviluki.grindrplus.decorated.persistence.model

import de.robv.android.xposed.XposedHelpers
import com.eljaviluki.grindrplus.Hooker
import com.eljaviluki.grindrplus.Obfuscation.GApp

class Profile {
    var instance: Any

    constructor(
         profileId: String,
         created: Long,
         remoteUpdatedTime: Long,
         seen: Long,
         isSecretAdmirer: Boolean,
         isFavorite: Boolean,
         isViewedMeFreshFace: Boolean,
         displayName: String?,
         mediaHash: String?,
         age: Int,
         showDistance: Boolean,
         showAge: Boolean,
         distance: Double?,
         isNew: Boolean,
         aboutMe: String?,
         profileTags: List<String>,
         ethnicity: Int,
         lookingFor: List<Int>,
         relationshipStatus: Int,
         grindrTribes: List<Int>,
         identity: Any?, //TODO Implement com.grindrapp.android.model.Identity? decorator
         genderCategory: Int,
         pronounsCategory: Int,
         genderDisplay: String?,
         pronounsDisplay: String?,
         bodyType: Int,
         sexualPosition: Int,
         hivStatus: Int,
         lastTestedDate: Long,
         weight: Double,
         height: Double,
         twitterId: String?,
         facebookId: String?,
         instagramId: String?,
         localUpdatedTime: Long,
         lastViewed: Long?,
         acceptNSFWPics: Int,
         meetAt: List<Int>,
         markDelete: Boolean,
         lastMessageTimestamp: Long,
         singerDisplay: String?,
         songDisplay: String?,
         hashtags: List<String>,
         genders: List<Int>,
         pronouns: List<Int>
    ) {
        instance = XposedHelpers.newInstance(
            CLAZZ,
            profileId,
            created,
            remoteUpdatedTime,
            seen,
            isSecretAdmirer,
            isFavorite,
            isViewedMeFreshFace,
            displayName,
            mediaHash,
            age,
            showDistance,
            showAge,
            distance,
            isNew,
            aboutMe,
            profileTags,
            ethnicity,
            lookingFor,
            relationshipStatus,
            grindrTribes,
            null,  //identity //TODO Implement Identity class decorator
            genderCategory,
            pronounsCategory,
            genderDisplay,
            pronounsDisplay,
            bodyType,
            sexualPosition,
            hivStatus,
            lastTestedDate,
            weight,
            height,
            twitterId,
            facebookId,
            instagramId,
            localUpdatedTime,
            lastViewed,
            acceptNSFWPics,
            meetAt,
            markDelete,
            lastMessageTimestamp,
            singerDisplay,
            songDisplay,
            hashtags,
            genders,
            pronouns
        )
    }

    constructor(instance: Any) {
        this.instance = instance
    }

    var instagramId: String?
        get() = XposedHelpers.getObjectField(instance, "instagramId") as String?
        set(value) = XposedHelpers.setObjectField(instance, "instagramId", value)

    var distance: Double?
        get() = XposedHelpers.getObjectField(instance, "distance") as Double?
        set(value) = XposedHelpers.setObjectField(instance, "distance", value)

    var relationshipStatus: Int
        get() = XposedHelpers.getIntField(instance, "relationshipStatus")
        set(value) = XposedHelpers.setIntField(instance, "relationshipStatus", value)

    var sexualPosition: Int
        get() = XposedHelpers.getIntField(instance, "sexualPosition")
        set(value) = XposedHelpers.setIntField(instance, "sexualPosition", value)

    var markDelete: Boolean
        get() = XposedHelpers.getBooleanField(instance, "markDelete")
        set(value) = XposedHelpers.setBooleanField(instance, "markDelete", value)

    var hivStatus: Int
        get() = XposedHelpers.getIntField(instance, "hivStatus")
        set(value) = XposedHelpers.setIntField(instance, "hivStatus", value)

    var height: Double
        get() = XposedHelpers.getDoubleField(instance, "height")
        set(value) = XposedHelpers.setDoubleField(instance, "height", value)

    var hashtags: List<String>
        get() = XposedHelpers.getObjectField(instance, "hashtags") as List<String>
        set(value) = XposedHelpers.setObjectField(instance, "hashtags", value)

    var grindrTribes: List<Int>
        get() = XposedHelpers.getObjectField(instance, "grindrTribes") as List<Int>
        set(value) = XposedHelpers.setObjectField(instance, "grindrTribes", value)

    var created: Long
        get() = XposedHelpers.getLongField(instance, "created")
        set(value) = XposedHelpers.setLongField(instance, "created", value)

    var songDisplay: String?
        get() = XposedHelpers.getObjectField(instance, "songDisplay") as String?
        set(value) = XposedHelpers.setObjectField(instance, "songDisplay", value)

    var genderCategory: Int
        get() = XposedHelpers.getIntField(instance, "genderCategory")
        set(value) = XposedHelpers.setIntField(instance, "genderCategory", value)

    var facebookId: String?
        get() = XposedHelpers.getObjectField(instance, "facebookId") as String?
        set(value) = XposedHelpers.setObjectField(instance, "facebookId", value)

    var aboutMe: String?
        get() = XposedHelpers.getObjectField(instance, "aboutMe") as String?
        set(value) = XposedHelpers.setObjectField(instance, "aboutMe", value)

    var genderDisplay: String?
        get() = XposedHelpers.getObjectField(instance, "genderDisplay") as String?
        set(value) = XposedHelpers.setObjectField(instance, "genderDisplay", value)

    var lastTestedDate: Long
        get() = XposedHelpers.getLongField(instance, "lastTestedDate")
        set(value) = XposedHelpers.setLongField(instance, "lastTestedDate", value)

    var showDistance: Boolean
        get() = XposedHelpers.getBooleanField(instance, "showDistance")
        set(value) = XposedHelpers.setBooleanField(instance, "showDistance", value)

    var lastMessageTimestamp: Long
        get() = XposedHelpers.getLongField(instance, "lastMessageTimestamp")
        set(value) = XposedHelpers.setLongField(instance, "lastMessageTimestamp", value)

    var showAge: Boolean
        get() = XposedHelpers.getBooleanField(instance, "showAge")
        set(value) = XposedHelpers.setBooleanField(instance, "showAge", value)

    var profileTags: List<String>
        get() = XposedHelpers.getObjectField(instance, "profileTags") as List<String>
        set(value) = XposedHelpers.setObjectField(instance, "profileTags", value)

    var ethnicity: Int
        get() = XposedHelpers.getIntField(instance, "ethnicity")
        set(value) = XposedHelpers.setIntField(instance, "ethnicity", value)

    var remoteUpdatedTime: Long
        get() = XposedHelpers.getLongField(instance, "remoteUpdatedTime")
        set(value) = XposedHelpers.setLongField(instance, "remoteUpdatedTime", value)

    var meetAt: List<Int>
        get() = XposedHelpers.getObjectField(instance, "meetAt") as List<Int>
        set(value) = XposedHelpers.setObjectField(instance, "meetAt", value)

    var lookingFor: List<Int>
        get() = XposedHelpers.getObjectField(instance, "lookingFor") as List<Int>
        set(value) = XposedHelpers.setObjectField(instance, "lookingFor", value)

    var pronounsCategory: Int
        get() = XposedHelpers.getIntField(instance, "pronounsCategory")
        set(value) = XposedHelpers.setIntField(instance, "pronounsCategory", value)

    var age: Int
        get() = XposedHelpers.getIntField(instance, "age")
        set(value) = XposedHelpers.setIntField(instance, "age", value)

    var seen: Long
        get() = XposedHelpers.getLongField(instance, "seen")
        set(value) = XposedHelpers.setLongField(instance, "seen", value)

    var mediaHash: String?
        get() = XposedHelpers.getObjectField(instance, "mediaHash") as String?
        set(value) = XposedHelpers.setObjectField(instance, "mediaHash", value)

    var pronouns: List<Int>
        get() = XposedHelpers.getObjectField(instance, "pronouns") as List<Int>
        set(value) = XposedHelpers.setObjectField(instance, "pronouns", value)

    var singerDisplay: String?
        get() = XposedHelpers.getObjectField(instance, "singerDisplay") as String?
        set(value) = XposedHelpers.setObjectField(instance, "singerDisplay", value)

    var acceptNSFWPics: Int
        get() = XposedHelpers.getIntField(instance, "acceptNSFWPics")
        set(value) = XposedHelpers.setIntField(instance, "acceptNSFWPics", value)

    var profileId: String
        get() = XposedHelpers.getObjectField(instance, "profileId") as String
        set(value) = XposedHelpers.setObjectField(instance, "profileId", value)

    var isViewedMeFreshFace: Boolean
        get() = XposedHelpers.getBooleanField(instance, "isViewedMeFreshFace")
        set(value) = XposedHelpers.setBooleanField(instance, "isViewedMeFreshFace", value)

    //TODO: Decorate Identity with a proper class
    /*private final var identity: com.grindrapp.android.model.Identity?
        get() = XposedHelpers.getObjectField(instance, "identity") as com.grindrapp.android.model.Identity?
        set(value) = XposedHelpers.setObjectField(instance, "identity", value)*/

    var isFavorite: Boolean
        get() = XposedHelpers.getBooleanField(instance, "isFavorite")
        set(value) = XposedHelpers.setBooleanField(instance, "isFavorite", value)

    var localUpdatedTime: Long
        get() = XposedHelpers.getLongField(instance, "localUpdatedTime")
        set(value) = XposedHelpers.setLongField(instance, "localUpdatedTime", value)

    var genders: List<Int>
        get() = XposedHelpers.getObjectField(instance, "genders") as List<Int>
        set(value) = XposedHelpers.setObjectField(instance, "genders", value)

    var displayName: String?
        get() = XposedHelpers.getObjectField(instance, "displayName") as String?
        set(value) = XposedHelpers.setObjectField(instance, "displayName", value)

    var pronounsDisplay: String?
        get() = XposedHelpers.getObjectField(instance, "pronounsDisplay") as String?
        set(value) = XposedHelpers.setObjectField(instance, "pronounsDisplay", value)

    var isSecretAdmirer: Boolean
        get() = XposedHelpers.getBooleanField(instance, "isSecretAdmirer")
        set(value) = XposedHelpers.setBooleanField(instance, "isSecretAdmirer", value)

    val isAvailable: Boolean //Actually it's a val
        get() = XposedHelpers.callMethod(instance, "isAvailable") as Boolean

    var twitterId: String?
        get() = XposedHelpers.getObjectField(instance, "twitterId") as String?
        set(value) = XposedHelpers.setObjectField(instance, "twitterId", value)

    var weight: Double
        get() = XposedHelpers.getDoubleField(instance, "weight")
        set(value) = XposedHelpers.setDoubleField(instance, "weight", value)

    var bodyType: Int
        get() = XposedHelpers.getIntField(instance, "bodyType")
        set(value) = XposedHelpers.setIntField(instance, "bodyType", value)

    var isNew: Boolean
        get() = XposedHelpers.getBooleanField(instance, "isNew")
        set(value) = XposedHelpers.setBooleanField(instance, "isNew", value)

    var lastViewed: Long?
        get() = XposedHelpers.getObjectField(instance, "lastViewed") as Long?
        set(value) = XposedHelpers.setObjectField(instance, "lastViewed", value)

    companion object {
        val CLAZZ: Class<*> by lazy {
            XposedHelpers.findClass(GApp.persistence.model.Profile, Hooker.pkgParam.classLoader)
        }
    }
}


