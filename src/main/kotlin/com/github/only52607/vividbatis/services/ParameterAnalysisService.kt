package com.github.only52607.vividbatis.services

import com.github.only52607.vividbatis.util.JavaClassAnalyzer
import com.github.only52607.vividbatis.util.MybatisXmlParser
import com.google.gson.GsonBuilder
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

@Service
class ParameterAnalysisService(private val project: Project) {
    
    companion object {
        fun getInstance(project: Project): ParameterAnalysisService {
            return project.getService(ParameterAnalysisService::class.java)
        }
    }
    
    private val mybatisXmlParser = MybatisXmlParser()
    private val javaClassAnalyzer = JavaClassAnalyzer()
    private val gson = GsonBuilder().setPrettyPrinting().create()
    
    fun generateDefaultParameterJson(namespace: String, statementId: String): String {
        val parameterType = getParameterType(namespace, statementId)
        
        return when {
            parameterType == null -> "{}"
            isPrimitiveOrWrapper(parameterType) -> generatePrimitiveJson(parameterType)
            parameterType.startsWith("java.lang.") -> generatePrimitiveJson(parameterType)
            parameterType == "java.util.Map" -> "{\"key\": \"value\"}"
            else -> generateObjectJson(parameterType)
        }
    }
    
    private fun getParameterType(namespace: String, statementId: String): String? {
        return mybatisXmlParser.getParameterType(project, namespace, statementId)
    }
    
    private fun isPrimitiveOrWrapper(type: String): Boolean {
        return type in setOf(
            "int", "java.lang.Integer", "long", "java.lang.Long",
            "double", "java.lang.Double", "float", "java.lang.Float",
            "boolean", "java.lang.Boolean", "byte", "java.lang.Byte",
            "short", "java.lang.Short", "char", "java.lang.Character",
            "java.lang.String"
        )
    }
    
    private fun generatePrimitiveJson(type: String): String {
        val value = when (type) {
            "int", "java.lang.Integer" -> "1"
            "long", "java.lang.Long" -> "1"
            "double", "java.lang.Double" -> "1.0"
            "float", "java.lang.Float" -> "1.0"
            "boolean", "java.lang.Boolean" -> "true"
            "byte", "java.lang.Byte" -> "1"
            "short", "java.lang.Short" -> "1"
            "char", "java.lang.Character" -> "\"A\""
            "java.lang.String" -> "\"示例值\""
            else -> "\"示例值\""
        }
        return "{\n  \"value\": $value\n}"
    }
    
    private fun generateObjectJson(className: String): String {
        val psiClass = findPsiClass(className) ?: return "{}"
        val jsonObject = javaClassAnalyzer.analyzeClass(psiClass)
        return gson.toJson(jsonObject)
    }
    
    private fun findPsiClass(className: String): PsiClass? {
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        return javaPsiFacade.findClass(className, GlobalSearchScope.allScope(project))
    }
} 