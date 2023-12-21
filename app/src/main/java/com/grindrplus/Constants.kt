package com.grindrplus

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XC_MethodReplacement.*

object Constants {
    const val GRINDR_PKG = "com.grindrapp.android"
    const val GRINDR_PKG_VERSION_NAME = "9.18.0"

    object Returns {
        val RETURN_TRUE: XC_MethodReplacement = returnConstant(true)
        val RETURN_FALSE: XC_MethodReplacement = returnConstant(false)
        val RETURN_INTEGER_MAX_VALUE: XC_MethodReplacement  = returnConstant(Int.MAX_VALUE)
        val RETURN_LONG_MAX_VALUE: XC_MethodReplacement  = returnConstant(Long.MAX_VALUE)
        val RETURN_ZERO: XC_MethodReplacement  = returnConstant(0)
        val RETURN_ONE: XC_MethodReplacement  = returnConstant(1)
        val RETURN_UNIT: XC_MethodReplacement  = returnConstant(Unit)
        val RETURN_NULL: XC_MethodReplacement  = returnConstant(null)
    }
}