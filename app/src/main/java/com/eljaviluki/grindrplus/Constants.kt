package com.eljaviluki.grindrplus

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XC_MethodReplacement.*

object Constants {
    const val GRINDR_PKG = "com.grindrapp.android"

    object Returns {
        val RETURN_TRUE: XC_MethodReplacement = returnConstant(true)
        val RETURN_FALSE: XC_MethodReplacement = returnConstant(false)
        val RETURN_INTEGER_MAX_VALUE: XC_MethodReplacement  = returnConstant(Int.MAX_VALUE)
        val RETURN_LONG_MAX_VALUE: XC_MethodReplacement  = returnConstant(Long.MAX_VALUE)
        val RETURN_ZERO: XC_MethodReplacement  = returnConstant(0)
    }
}