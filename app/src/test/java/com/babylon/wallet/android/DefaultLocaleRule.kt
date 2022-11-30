package com.babylon.wallet.android

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.Locale

class DefaultLocaleRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                Locale.setDefault(Locale.UK)
                base.evaluate()
            }

        }
    }
}