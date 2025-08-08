package com.github.only52607.vividbatis.util

import com.intellij.openapi.project.Project
import com.intellij.psi.xml.XmlFile

data class SqlTemplate(
    val namespace: String,
    val statementId: String,
    val statementType: String,
    val mapperFile: XmlFile,
    val project: Project
)

