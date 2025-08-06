package com.github.only52607.vividbatis.services

import com.github.only52607.vividbatis.util.JavaClassAnalyzer
import com.github.only52607.vividbatis.util.MybatisXmlParser
import com.github.only52607.vividbatis.util.TypeUtils
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
        val parameterType = mybatisXmlParser.getParameterType(project, namespace, statementId)
        
        return when {
            parameterType == null -> "{}"
            TypeUtils.isPrimitiveOrWrapper(parameterType) -> generatePrimitiveJson(parameterType)
            parameterType.startsWith("java.lang.") -> generatePrimitiveJson(parameterType)
            parameterType == "java.util.Map" -> "{\"key\": \"value\"}"
            else -> generateObjectJson(parameterType)
        }
    }
    
    private fun generatePrimitiveJson(type: String): String {
        val value = TypeUtils.generatePrimitiveJsonValue(type)
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