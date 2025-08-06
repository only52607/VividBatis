package com.github.only52607.vividbatis

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.components.service
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import com.github.only52607.vividbatis.services.ParameterAnalysisService
import com.github.only52607.vividbatis.services.SqlGenerationService

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testXMLFile() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, "<foo>bar</foo>")
        val xmlFile = assertInstanceOf(psiFile, XmlFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, xmlFile.virtualFile))

        assertNotNull(xmlFile.rootTag)

        xmlFile.rootTag?.let {
            assertEquals("foo", it.name)
            assertEquals("bar", it.value.text)
        }
    }

    fun testRename() {
        myFixture.testRename("foo.xml", "foo_after.xml", "a2")
    }

    fun testParameterAnalysisService() {
        val parameterAnalysisService = ParameterAnalysisService.getInstance(project)
        assertNotNull(parameterAnalysisService)
        
        // 测试基本服务可用性
        val json = parameterAnalysisService.generateDefaultParameterJson("test.namespace", "testStatement")
        assertNotNull(json)
    }
    
    fun testSqlGenerationService() {
        val sqlGenerationService = SqlGenerationService.getInstance(project)
        assertNotNull(sqlGenerationService)
    }

    override fun getTestDataPath() = "src/test/testData/rename"
}
