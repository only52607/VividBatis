import com.google.gson.*
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil

object PsiTypeToJsonConverter {
    private val processedTypes = mutableSetOf<String>()

    fun convert(psiType: PsiType): JsonElement = when {
        psiType is PsiArrayType -> convertArray(psiType)
        psiType.equalsToText("boolean") || psiType.equalsToText(CommonClassNames.JAVA_LANG_BOOLEAN) -> JsonPrimitive(true)
        psiType.equalsToText("byte") || psiType.equalsToText(CommonClassNames.JAVA_LANG_BYTE) -> JsonPrimitive(1)
        psiType.equalsToText("short") || psiType.equalsToText(CommonClassNames.JAVA_LANG_SHORT) -> JsonPrimitive(1)
        psiType.equalsToText("int") || psiType.equalsToText(CommonClassNames.JAVA_LANG_INTEGER) -> JsonPrimitive(1)
        psiType.equalsToText("long") || psiType.equalsToText(CommonClassNames.JAVA_LANG_LONG) -> JsonPrimitive(1L)
        psiType.equalsToText("float") || psiType.equalsToText(CommonClassNames.JAVA_LANG_FLOAT) -> JsonPrimitive(1.0f)
        psiType.equalsToText("double") || psiType.equalsToText(CommonClassNames.JAVA_LANG_DOUBLE) -> JsonPrimitive(1.0)
        psiType.equalsToText("char") || psiType.equalsToText(CommonClassNames.JAVA_LANG_CHARACTER) -> JsonPrimitive("A")
        psiType.equalsToText(CommonClassNames.JAVA_LANG_STRING) -> JsonPrimitive("string")
        psiType.equalsToText(CommonClassNames.JAVA_UTIL_DATE) -> JsonPrimitive("2025-01-01T00:00:00Z")
        psiType.equalsToText(CommonClassNames.JAVA_TIME_LOCAL_DATE_TIME) -> JsonPrimitive("2025-01-01T00:00:00")
        psiType.equalsToText(CommonClassNames.JAVA_TIME_LOCAL_DATE) -> JsonPrimitive("2025-01-01")
        psiType.equalsToText("java.math.BigDecimal") -> JsonPrimitive("100.00")
        isCollection(psiType) -> convertCollection(psiType)
        isMap(psiType) -> convertMap(psiType)
        else -> convertObject(psiType)
    }

    private fun convertArray(arrayType: PsiArrayType): JsonArray {
        val componentType = arrayType.componentType
        return JsonArray().apply { add(convert(componentType)) }
    }

    private fun convertCollection(collectionType: PsiType): JsonArray {
        val psiClass = PsiTypesUtil.getPsiClass(collectionType)
        val elementType =
            PsiUtil.substituteTypeParameter(collectionType, CommonClassNames.JAVA_UTIL_COLLECTION, 0, false)
                ?: psiClass?.manager?.let { manager ->
                    collectionType.resolveScope?.let { scope ->
                        PsiType.getJavaLangObject(manager, scope)
                    }
                }
                ?: return JsonArray()
        return JsonArray().apply { add(convert(elementType)) }
    }

    private fun convertMap(mapType: PsiType): JsonObject {
        val keyType = PsiUtil.substituteTypeParameter(mapType, CommonClassNames.JAVA_UTIL_MAP, 0, false)
        val psiClass = PsiTypesUtil.getPsiClass(mapType)
        val valueType = PsiUtil.substituteTypeParameter(mapType, CommonClassNames.JAVA_UTIL_MAP, 1, false)
            ?: psiClass?.manager?.let { manager ->
                mapType.resolveScope?.let { scope ->
                    PsiType.getJavaLangObject(manager, scope)
                }
            }
            ?: return JsonObject()

        val keyString = when {
            keyType?.equalsToText(CommonClassNames.JAVA_LANG_STRING) == true -> "key"
            keyType?.equalsToText("int") == true || keyType?.equalsToText(CommonClassNames.JAVA_LANG_INTEGER) == true -> "1"
            else -> "key"
        }

        return JsonObject().apply {
            add(keyString, convert(valueType))
        }
    }

    private fun convertObject(psiType: PsiType): JsonElement {
        val psiClass = PsiTypesUtil.getPsiClass(psiType) ?: return JsonNull.INSTANCE
        val typeName = psiClass.qualifiedName ?: return JsonNull.INSTANCE

        if (processedTypes.contains(typeName)) return JsonObject()
        processedTypes.add(typeName)

        return try {
            JsonObject().apply {
                psiClass.fields
                    .filterNot { it.hasModifierProperty(PsiModifier.STATIC) }
                    .forEach { field ->
                        val fieldType = field.type
                        add(field.name, convert(fieldType))
                    }
            }
        } finally {
            processedTypes.remove(typeName)
        }
    }

    private fun isCollection(psiType: PsiType): Boolean {
        val psiClass = PsiTypesUtil.getPsiClass(psiType) ?: return false
        return psiClass.qualifiedName?.let { name ->
            name.startsWith(CommonClassNames.JAVA_UTIL_LIST) ||
                    name.startsWith(CommonClassNames.JAVA_UTIL_SET) ||
                    name.startsWith(CommonClassNames.JAVA_UTIL_COLLECTION) ||
                    name == CommonClassNames.JAVA_UTIL_ARRAY_LIST ||
                    name == CommonClassNames.JAVA_UTIL_LINKED_LIST ||
                    name == CommonClassNames.JAVA_UTIL_HASH_SET ||
                    name == "java.util.TreeSet"
        } ?: false
    }

    private fun isMap(psiType: PsiType): Boolean {
        val psiClass = PsiTypesUtil.getPsiClass(psiType) ?: return false
        return psiClass.qualifiedName?.let { name ->
            name.startsWith(CommonClassNames.JAVA_UTIL_MAP) ||
                    name == CommonClassNames.JAVA_UTIL_HASH_MAP ||
                    name == "java.util.TreeMap" ||
                    name == CommonClassNames.JAVA_UTIL_LINKED_HASH_MAP
        } ?: false
    }
}