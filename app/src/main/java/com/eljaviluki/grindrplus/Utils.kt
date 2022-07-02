
package com.eljaviluki.grindrplus

import java.text.SimpleDateFormat
import java.util.*

object Utils {
    fun toReadableDate(timestamp: Long): String = SimpleDateFormat.getDateTimeInstance().format(Date(timestamp))

    /**
     * Calculates the BMI and returns a description of the BMI.
     *
     * @param weight in grams
     * @param height in cm
     * @return BMI in kg/m^2 and its description
     *
     * @see <a href="https://en.wikipedia.org/wiki/Body_mass_index">BMI - Wikipedia</a>
     * @see <a href="https://www.who.int/europe/news-room/fact-sheets/item/a-healthy-lifestyle---who-recommendations">WHO recommendations</a>
     */
    fun getBmiDescription(weight: Double, height: Double): String {
        val originalBmi = 10 * (weight / (height * height)) //Multiply by 10 to get kg/m^2
        val bmi = Math.round(originalBmi * 100.0) / 100.0 //Round to 2 decimal places
        return when {
            bmi < 18.5 -> "$bmi (Underweight)"
            bmi < 25.0 -> "$bmi (Normal weight)"
            bmi < 30.0 -> "$bmi (Overweight)"
            bmi < 35.0 -> "$bmi (Obesity I)"
            bmi < 40.0 -> "$bmi (Obesity II)"
            else -> "$bmi (Obesity III)"
        }
    }
}