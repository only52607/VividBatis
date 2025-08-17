package com.github.only52607.vividbatis.mybatis.util

import com.github.only52607.vividbatis.model.StatementParameterDeclaration
import com.github.only52607.vividbatis.model.StatementParameterType
import com.github.only52607.vividbatis.model.StatementPath
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.CommonClassNames


object ParameterAnalyzer {
    const val TYPE_IBATIS_PARAM = "org.apache.ibatis.annotations.Param"

    fun getStatementParameterInfo(project: Project, statementPath: StatementPath): StatementParameterType {
        val parameterTypeInXml = project.findMybatisMapperXml(statementPath.namespace)
            ?.findMybatisStatementById(statementPath.statementId)
            ?.getAttributeValue("parameterType")
        if (parameterTypeInXml?.isNotEmpty() == true) {
            return StatementParameterType.JavaBean(
                project.findPsiClass(parameterTypeInXml)?.let(PsiTypesUtil::getClassType)
            )
        }
        val method = project.findPsiMethod(statementPath.namespace, statementPath.statementId)
            ?: return StatementParameterType.Map()
        val parameters = method.parameterList.parameters
        if (parameters.isEmpty()) {
            return StatementParameterType.Map()
        }
        if (parameters.size == 1 && findParameterName(parameters[0]) == null) {
            val param = parameters[0]
            val type = param.type
            when (type) {
                is PsiPrimitiveType -> return StatementParameterType.Multiple(
                    StatementParameterDeclaration(name = "_parameter", psiParameter = param)
                )

                is PsiClassType -> {
                    return when (type.className) {
                        CommonClassNames.JAVA_UTIL_MAP, CommonClassNames.JAVA_UTIL_HASH_MAP -> StatementParameterType.Map(
                            type.parameters.getOrNull(1)
                        )

                        else -> StatementParameterType.JavaBean(param.type)
                    }
                }
            }
        }
        return StatementParameterType.Multiple(
            parameters.map { param ->
                StatementParameterDeclaration(
                    name = findParameterName(param) ?: if (parameters.size == 1) "_parameter" else null,
                    psiParameter = param
                )
            }
        )
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

    private fun findParameterName(parameter: PsiParameter): String? {
        return parameter.annotations.find { it.qualifiedName == TYPE_IBATIS_PARAM }
            ?.findAttributeValue("value")?.text?.removeSurrounding("\"")
    }
} 
