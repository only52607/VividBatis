package com.github.only52607.vividbatis.model

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiParameter

enum class ParameterType {
    MAP,
    ANNOTATION,
    JAVA_BEAN,
    MIXED,
    POSITIONAL,
    SINGLE_PRIMITIVE
}

data class ParameterInfo(
    val type: ParameterType,
    val parameterClass: PsiClass? = null,
    val parameters: List<MethodParameter> = emptyList(),
    val parameterTypeString: String? = null
)

data class MethodParameter(
    val name: String,
    val type: String,
    val paramAnnotation: String? = null,
    val position: Int,
    val psiParameter: PsiParameter? = null
)
