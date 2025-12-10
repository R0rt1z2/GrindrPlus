package com.grindrplus.commands

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Command(val name: String, val aliases: Array<String> = [], val help: String = "")