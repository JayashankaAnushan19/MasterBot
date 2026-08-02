package com.masterbot.engine

import com.masterbot.engine.model.AdaptationRules
import com.masterbot.engine.model.MasterBotIndex
import java.io.File

/** Reads the real repo files so tests validate the engine against actual source-of-truth data. */
object TestFixtures {
    private val repoRoot: File
        get() = File(System.getProperty("masterbot.repoRoot") ?: error("masterbot.repoRoot system property not set"))

    fun loadRealRules(): AdaptationRules =
        AdaptationRules.parse(File(repoRoot, "rules/adaptation_rules.yaml").readText())

    fun loadRealIndex(): MasterBotIndex =
        MasterBotIndex.parse(File(repoRoot, "index.json").readText())
}
