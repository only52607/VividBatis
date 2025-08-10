package com.github.only52607.vividbatis.model

@JvmInline
value class StatementQualifyId(
    val fullId: String
) {
    constructor(namespace: String, statementId: String) : this("$namespace.$statementId")

    val namespace: String
        get() = if (fullId.contains(".")) fullId.substringBeforeLast('.') else ""

    val statementId: String
        get() = fullId.substringAfterLast('.')

    override fun toString(): String {
        return fullId
    }
}