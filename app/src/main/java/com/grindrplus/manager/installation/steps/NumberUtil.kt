package com.grindrplus.manager.installation.steps


private val ones = arrayOf(
    "",
    "One",
    "Two",
    "Three",
    "Four",
    "Five",
    "Six",
    "Seven",
    "Eight",
    "Nine",
    "Ten",
    "Eleven",
    "Twelve",
    "Thirteen",
    "Fourteen",
    "Fifteen",
    "Sixteen",
    "Seventeen",
    "Eighteen",
    "Nineteen"
)

private val tens =
    arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

tailrec fun numberToWords(num: Int, result: String = ""): String {
    return when {
        num == 0 && result == "" -> "Zero" // base case
        num == 0 -> result.trim() // final result
        num >= 1000000000 -> numberToWords(
            num % 1000000000,
            result + numberToWords(num / 1000000000) + " Billion "
        )

        num >= 1000000 -> numberToWords(
            num % 1000000,
            result + numberToWords(num / 1000000) + " Million "
        )

        num >= 1000 -> numberToWords(num % 1000, result + numberToWords(num / 1000) + " Thousand ")
        num >= 100 -> numberToWords(num % 100, result + numberToWords(num / 100) + " Hundred ")
        num >= 20 -> numberToWords(num % 10, result + tens[num / 10] + " ")
        else -> result + ones[num]
    }
}