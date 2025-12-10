package com.grindrplus.manager.utils

import android.Manifest
import com.grindrplus.core.Constants
import com.grindrplus.core.Constants.GRINDR_PACKAGE_NAME
import pxb.android.axml.AxmlReader
import pxb.android.axml.AxmlVisitor
import pxb.android.axml.AxmlWriter
import pxb.android.axml.NodeVisitor

// Taken by https://github.com/revenge-mod/revenge-manager/blob/main/app/src/main/java/app/revenge/manager/installer/util/ManifestPatcher.kt
object ManifestPatcher {
    private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    private const val USES_CLEARTEXT_TRAFFIC = "usesCleartextTraffic"
    private const val DEBUGGABLE = "debuggable"
    private const val REQUEST_LEGACY_EXTERNAL_STORAGE = "requestLegacyExternalStorage"
    private const val LABEL = "label"
    private const val PACKAGE = "package"
    private const val COMPILE_SDK_VERSION = "compileSdkVersion"
    private const val COMPILE_SDK_VERSION_CODENAME = "compileSdkVersionCodename"
    private const val DRNE_PERMISSION = "DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
    private const val GRINDR_DRNE_PERMISSION = "$GRINDR_PACKAGE_NAME.$DRNE_PERMISSION"

    fun patchManifest(
        manifestBytes: ByteArray,
        packageName: String,
        appName: String,
        debuggable: Boolean,
    ): ByteArray {
        val reader = AxmlReader(manifestBytes)
        val writer = AxmlWriter()

        reader.accept(object : AxmlVisitor(writer) {
            override fun child(ns: String?, name: String?) =
                object : ReplaceAttrsVisitor(
                    super.child(ns, name),
                    mapOf(
                        PACKAGE to packageName,
                        COMPILE_SDK_VERSION to 23,
                        COMPILE_SDK_VERSION_CODENAME to "6.0-2438415"
                    )
                ) {
                    private var addExternalStoragePerm = false

                    override fun child(ns: String?, name: String): NodeVisitor {
                        val nv = super.child(ns, name)

                        // Add MANAGE_EXTERNAL_STORAGE when necessary
                        if (addExternalStoragePerm) {
                            super
                                .child(null, "uses-permission")
                                .attr(
                                    ANDROID_NAMESPACE,
                                    "name",
                                    android.R.attr.name,
                                    TYPE_STRING,
                                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                                )
                            addExternalStoragePerm = false
                        }

                        return when (name) {
                            "permission" -> object : NodeVisitor(nv) {
                                override fun attr(
                                    ns: String?,
                                    name: String?,
                                    resourceId: Int,
                                    type: Int,
                                    value: Any?,
                                ) {
                                    if (name == "name" && value == GRINDR_DRNE_PERMISSION) {
                                        super.attr(
                                            ns,
                                            name,
                                            resourceId,
                                            type,
                                            "$packageName.$DRNE_PERMISSION"
                                        )
                                    } else if (name == "name" && value == GRINDR_PACKAGE_NAME) {
                                        super.attr(
                                            ns,
                                            name,
                                            resourceId,
                                            type,
                                            packageName
                                        )
                                    } else {
                                        super.attr(ns, name, resourceId, type, value)
                                    }
                                }
                            }

                            "uses-permission" -> object : NodeVisitor(nv) {
                                override fun attr(
                                    ns: String?,
                                    name: String?,
                                    resourceId: Int,
                                    type: Int,
                                    value: Any?,
                                ) {
                                    if (name == "name" && value == GRINDR_DRNE_PERMISSION) {
                                        super.attr(
                                            ns,
                                            name,
                                            resourceId,
                                            type,
                                            "$packageName.$DRNE_PERMISSION"
                                        )
                                    }

                                    if (name == "name" && value == GRINDR_PACKAGE_NAME) {
                                        super.attr(
                                            ns,
                                            name,
                                            resourceId,
                                            type,
                                            packageName
                                        )
                                    }

                                    if (name != "maxSdkVersion") {
                                        super.attr(ns, name, resourceId, type, value)
                                    }

                                    // Set the add external storage permission to be added after WRITE_EXTERNAL_STORAGE (which is after read)
                                    if (name == "name" && value == Manifest.permission.READ_EXTERNAL_STORAGE) {
                                        addExternalStoragePerm = true
                                    }
                                }
                            }

                            "application" -> object : ReplaceAttrsVisitor(
                                nv,
                                mapOf(
                                    LABEL to appName,
                                    DEBUGGABLE to debuggable,
                                    USES_CLEARTEXT_TRAFFIC to true,
                                    REQUEST_LEGACY_EXTERNAL_STORAGE to true
                                )
                            ) {
                                private var addDebuggable = debuggable
                                private var addLegacyStorage = true
                                private var addUseClearTextTraffic = true

                                override fun attr(
                                    ns: String?,
                                    name: String,
                                    resourceId: Int,
                                    type: Int,
                                    value: Any?,
                                ) {
                                    super.attr(ns, name, resourceId, type, value)
                                    if (name == REQUEST_LEGACY_EXTERNAL_STORAGE) addLegacyStorage =
                                        false
                                    if (name == DEBUGGABLE) addDebuggable = false
                                    if (name == USES_CLEARTEXT_TRAFFIC) addUseClearTextTraffic =
                                        false
                                }

                                override fun child(ns: String?, name: String): NodeVisitor {
                                    val visitor = super.child(ns, name)

                                    return when (name) {
                                        "activity-alias" -> object : NodeVisitor(visitor) {
                                            override fun attr(
                                                ns: String?,
                                                name: String?,
                                                resourceId: Int,
                                                type: Int,
                                                value: Any?,
                                            ) {
                                                if (name == "label") {
                                                    super.attr(ns, name, resourceId, TYPE_STRING, appName)
                                                } else {
                                                    super.attr(ns, name, resourceId, type, value)
                                                }
                                            }
                                        }

                                        "provider" -> object : NodeVisitor(visitor) {
                                            override fun attr(
                                                ns: String?,
                                                name: String,
                                                resourceId: Int,
                                                type: Int,
                                                value: Any?,
                                            ) {
                                                super.attr(
                                                    ns,
                                                    name,
                                                    resourceId,
                                                    type,
                                                    if (name == "authorities") {
                                                        (value as String).replace(
                                                            GRINDR_PACKAGE_NAME,
                                                            packageName
                                                        )
                                                    } else {
                                                        value
                                                    }
                                                )
                                            }
                                        }

                                        else -> visitor
                                    }
                                }

                                override fun end() {
                                    if (addLegacyStorage) super.attr(
                                        ANDROID_NAMESPACE,
                                        REQUEST_LEGACY_EXTERNAL_STORAGE,
                                        -1,
                                        TYPE_INT_BOOLEAN,
                                        1
                                    )
                                    if (addDebuggable) super.attr(
                                        ANDROID_NAMESPACE,
                                        DEBUGGABLE,
                                        -1,
                                        TYPE_INT_BOOLEAN,
                                        1
                                    )
                                    if (addUseClearTextTraffic) super.attr(
                                        ANDROID_NAMESPACE,
                                        USES_CLEARTEXT_TRAFFIC,
                                        -1,
                                        TYPE_INT_BOOLEAN,
                                        1
                                    )
                                    super.end()
                                }
                            }

                            else -> nv
                        }
                    }
                }
        })

        return writer.toByteArray()
    }

    fun renamePackage(
        manifestBytes: ByteArray,
        packageName: String,
    ): ByteArray {
        val reader = AxmlReader(manifestBytes)
        val writer = AxmlWriter()

        reader.accept(
            object : AxmlVisitor(writer) {
                override fun child(ns: String?, name: String?): NodeVisitor {
                    val nv = super.child(ns, name)

                    if (name == "manifest") {
                        return ReplaceAttrsVisitor(nv, mapOf("package" to packageName))
                    }

                    return nv
                }
            }
        )

        return writer.toByteArray()
    }

    private open class ReplaceAttrsVisitor(
        nv: NodeVisitor,
        private val attrs: Map<String, Any>,
    ) : NodeVisitor(nv) {
        override fun attr(ns: String?, name: String, resourceId: Int, type: Int, value: Any?) {
            val replace = attrs.containsKey(name)
            val newValue = attrs[name]

            super.attr(
                ns,
                name,
                resourceId,
                if (newValue is String) TYPE_STRING else type,
                if (replace) newValue else value
            )
        }
    }
}