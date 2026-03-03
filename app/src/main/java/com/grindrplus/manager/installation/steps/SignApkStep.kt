package com.grindrplus.manager.installation.steps

import android.content.Context
import com.grindrplus.manager.installation.BaseStep
import com.grindrplus.manager.installation.Print
import com.grindrplus.manager.utils.KeyStoreUtils
import java.io.File

/**
 * sign the apk files (in-place)
 */
class SignApkStep(
    val keyStoreUtils: KeyStoreUtils,
    val dir: File
): BaseStep() {
    override val name: String = "Sign modified Grindr APK"


    override suspend fun doExecute(
        context: Context,
        print: Print,
    ) {
        for (file in dir.listFiles()!!) {
            if (!file.name.endsWith(".apk")) {
                print("Skipping ${file.name} as it is not an APK")
                continue
            }

//            val outFile = File(outputDir, "${file.name}-aligned.apk")
//            val zipIn = RandomAccessFile(file, "r");
//            val zipOut = outFile.outputStream();
//
//            print("Aligning ${file.name}...")
//
//            ZipAlign.alignZip(zipIn, zipOut)
//
//            print("Signing ${outFile.name}...")

            try {
                keyStoreUtils.signApk(file, File(dir, "${file.name}-signed.apk"))
                //outFile.delete()
                file.delete()
            } catch (e: Exception) {
                print("Failed to sign ${file.name}: ${e.localizedMessage}")
                throw e
            }
        }
    }
}