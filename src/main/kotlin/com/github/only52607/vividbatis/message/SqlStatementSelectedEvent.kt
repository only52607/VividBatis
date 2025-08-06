package com.github.only52607.vividbatis.message

import com.intellij.util.messages.Topic

data class SqlStatementSelectedEvent(
    val namespace: String,
    val statementId: String,
    val statementType: String,
    val xmlFilePath: String
)

interface SqlStatementSelectedListener {
    fun onStatementSelected(event: SqlStatementSelectedEvent)
    
    companion object {
        @JvmField
        val TOPIC = Topic.create("SqlStatementSelected", SqlStatementSelectedListener::class.java)
    }
} 