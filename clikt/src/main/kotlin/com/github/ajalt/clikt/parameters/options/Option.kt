package com.github.ajalt.clikt.parameters.options

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.output.HelpFormatter
import com.github.ajalt.clikt.parsers.OptionParser
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Option {
    val metavar: String?
    val help: String
    val parser: OptionParser
    val names: Set<String>
    val secondaryNames: Set<String>
    val nargs: Int
    val hidden: Boolean
    val parameterHelp: HelpFormatter.ParameterHelp.Option?
        get() = if (hidden) null
        else HelpFormatter.ParameterHelp.Option(names, secondaryNames, metavar, help, nargs > 1)

    fun finalize(context: Context, invocations: List<OptionParser.Invocation>)
}

interface OptionDelegate<out T> : Option, ReadOnlyProperty<CliktCommand, T> {
    /** Implementations must call [CliktCommand.registerOption] */
    operator fun provideDelegate(thisRef: CliktCommand, prop: KProperty<*>): ReadOnlyProperty<CliktCommand, T>
}

internal fun inferOptionNames(names: Set<String>, propertyName: String): Set<String> {
    if (names.isNotEmpty()) {
        val invalidName = names.find { !it.matches(Regex("\\p{Punct}{1,2}\\w+")) }
        require(invalidName == null) { "Invalid option name \"$invalidName\"" }
        return names
    }
    val normalizedName = propertyName.split(Regex("(?<=[a-z])(?=[A-Z])"))
            .joinToString("-", prefix = "--") { it.toLowerCase() }
    return setOf(normalizedName)
}

internal fun inferEnvvar(names: Set<String>, envvar: String?, autoEnvvarPrefix: String?): String? {
    if (envvar != null) return envvar
    if (names.isEmpty() || autoEnvvarPrefix == null) return null
    val name = splitOptionPrefix(names.maxBy { it.length }!!).second
    if (name.isEmpty()) return null
    return autoEnvvarPrefix + "_" + name.replace(Regex("\\W"), "_").toUpperCase()

}

/** Split an option token into a pair of prefix to simple name. */
internal fun splitOptionPrefix(name: String): Pair<String, String> =
        when {
            name.length < 2 || name[0].isLetterOrDigit() -> "" to name
            name.length > 2 && name[0] == name[1] -> name.slice(0..1) to name.substring(2)
            else -> name.slice(0..0) to name.substring(1)
        }
