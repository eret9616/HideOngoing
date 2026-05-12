package dev.local.hideongoing

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Rule(val pkg: String, val channelId: String?) {
    fun encode(): String = "$pkg|${channelId ?: ""}"

    companion object {
        fun decode(s: String): Rule {
            val parts = s.split('|', limit = 2)
            return Rule(parts[0], parts.getOrNull(1)?.ifEmpty { null })
        }
    }
}

class RuleStore(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("rules", Context.MODE_PRIVATE)
    private val _rules = MutableStateFlow(load())
    val rules: StateFlow<Set<Rule>> = _rules.asStateFlow()

    private fun load(): Set<Rule> =
        sp.getStringSet(KEY, emptySet())!!.map(Rule::decode).toSet()

    private fun save(set: Set<Rule>) {
        sp.edit().putStringSet(KEY, set.map(Rule::encode).toSet()).apply()
        _rules.value = set
    }

    fun add(r: Rule) = save(_rules.value + r)
    fun remove(r: Rule) = save(_rules.value - r)

    fun matches(pkg: String, channelId: String?): Boolean =
        _rules.value.any { it.pkg == pkg && (it.channelId == null || it.channelId == channelId) }

    private companion object {
        const val KEY = "set"
    }
}
