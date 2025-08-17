package com.github.only52607.vividbatis.mybatis.util

import com.github.only52607.vividbatis.model.StatementParameterDeclaration
import com.github.only52607.vividbatis.model.StatementParameterType
import com.github.only52607.vividbatis.model.StatementPath
import com.github.only52607.vividbatis.util.ParamClassNames
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID


object ParameterAnalyzer {
    fun getStatementParameterInfo(project: Project, statementPath: StatementPath): StatementParameterType {
        val psiParameters = project
            .findPsiMethod(statementPath.namespace, statementPath.statementId)
            ?.parameterList
            ?.parameters
        if (psiParameters == null || psiParameters.isEmpty()) {
            project.findMybatisMapperXml(statementPath.namespace)
                ?.findMybatisStatementById(statementPath.statementId)
                ?.getAttributeValue("parameterType")
                ?.let { project.findPsiClass(it) }
                ?.let(PsiTypesUtil::getClassType)
                ?.let(::fromSingleParameter)
                ?.let { return@getStatementParameterInfo it }
            return StatementParameterType.Map()
        }
        if (psiParameters.size == 1 && psiParameters[0].findMyBatisParameterName() == null) {
            val param = psiParameters[0]
            val type = param.type
            fromSingleParameter(type, param)?.let { return@getStatementParameterInfo it }
        }
        return StatementParameterType.Multiple(
            psiParameters.map { param ->
                StatementParameterDeclaration(
                    name = param.findMyBatisParameterName(),
                    psiParameter = param
                )
            }
        )
    }

    private fun fromSingleParameter(type: PsiType, param: PsiParameter? = null): StatementParameterType? {
        when (type) {
            is PsiPrimitiveType -> return StatementParameterType.Multiple(
                StatementParameterDeclaration(name = null, psiParameter = param)
            )

            is PsiArrayType -> return StatementParameterType.Multiple(
                StatementParameterDeclaration(name = "array", psiParameter = param)
            )

            is PsiClassType -> return when (type.rawType().canonicalText) {
                CommonClassNames.JAVA_LANG_STRING,
                CommonClassNames.JAVA_LANG_CHARACTER,
                CommonClassNames.JAVA_LANG_INTEGER,
                CommonClassNames.JAVA_LANG_LONG,
                CommonClassNames.JAVA_LANG_FLOAT,
                CommonClassNames.JAVA_LANG_DOUBLE,
                CommonClassNames.JAVA_LANG_BOOLEAN,
                CommonClassNames.JAVA_LANG_BYTE,
                CommonClassNames.JAVA_LANG_SHORT,
                ParamClassNames.JAVA_MATH_BIG_DECIMAL,
                ParamClassNames.JAVA_MATH_BIG_INTEGER,
                CommonClassNames.JAVA_UTIL_DATE,
                ParamClassNames.JAVA_TIME_LOCAL_DATE,
                ParamClassNames.JAVA_TIME_LOCAL_DATE_TIME,
                ParamClassNames.JAVA_TIME_INSTANT,
                ParamClassNames.JAVA_UTIL_UUID -> return StatementParameterType.Multiple(
                    StatementParameterDeclaration(name = null, psiParameter = param)
                )

                CommonClassNames.JAVA_UTIL_MAP, CommonClassNames.JAVA_UTIL_HASH_MAP -> StatementParameterType.Map(
                    type.parameters.getOrNull(1)
                )

                CommonClassNames.JAVA_UTIL_LIST,
                CommonClassNames.JAVA_UTIL_ARRAY_LIST,
                ParamClassNames.JAVA_UTIL_LINKED_LIST,
                ParamClassNames.JAVA_UTIL_QUEUE -> StatementParameterType.Multiple(
                    StatementParameterDeclaration(name = "list", psiParameter = param)
                )

                CommonClassNames.JAVA_UTIL_SET,
                CommonClassNames.JAVA_UTIL_HASH_SET,
                ParamClassNames.JAVA_UTIL_LINKED_HASH_SET,
                ParamClassNames.JAVA_UTIL_SORTED_SET,
                ParamClassNames.JAVA_UTIL_TREE_SET -> StatementParameterType.Multiple(
                    StatementParameterDeclaration(name = "set", psiParameter = param)
                )

                else -> StatementParameterType.JavaBean(param?.type ?: type)
            }
        }
        return null
    }

    private fun Project.findPsiClass(className: String): PsiClass? {
        return JavaPsiFacade.getInstance(this).findClass(className, GlobalSearchScope.allScope(this))
    }

    private fun findPsiMethod(psiClass: PsiClass, methodName: String): PsiMethod? {
        return psiClass.findMethodsByName(methodName, false).firstOrNull()
    }

    private fun Project.findPsiMethod(className: String, methodName: String): PsiMethod? {
        val mapperInterface = findPsiClass(className) ?: return null
        return findPsiMethod(mapperInterface, methodName)
    }
} 
