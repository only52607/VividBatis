package com.github.only52607.vividbatis.message

import com.intellij.util.messages.Topic

/**
 * SQL 语句选择事件
 */
data class SqlStatementSelectedEvent(
    val namespace: String,
    val statementId: String,
    val statementType: String, // select, insert, update, delete
    val xmlFilePath: String
)

/**
 * SQL 语句选择监听器接口
 */
interface SqlStatementSelectedListener {
    fun onStatementSelected(event: SqlStatementSelectedEvent)
    
    companion object {
        @JvmField
        val TOPIC = Topic.create("SqlStatementSelected", SqlStatementSelectedListener::class.java)
    }
} 