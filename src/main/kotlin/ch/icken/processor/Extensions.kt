/*
 * Copyright 2023-2025 Thijs Koppen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.icken.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.Annotatable
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName

//region KSAnnotated
internal fun KSAnnotated.annotation(qualifiedAnnotationClassName: String): KSAnnotation? =
    annotations.filter { it.isClass(qualifiedAnnotationClassName) }.singleOrNull()

internal fun KSAnnotated.hasAnnotation(qualifiedAnnotationClassName: String): Boolean =
    annotations.any { it.isClass(qualifiedAnnotationClassName) }
//endregion

//region KSAnnotation
private fun KSAnnotation.isClass(qualifiedAnnotationClassName: String): Boolean =
    annotationType.resolve().declaration.isClass(qualifiedAnnotationClassName)

internal fun KSAnnotation?.isParameterSet(parameterName: String): Boolean =
    this != null && defaultArguments[parameterName] != arguments[parameterName]
//endregion

//region KSClassDeclaration
internal fun KSClassDeclaration.isSubclass(qualifiedSuperclassName: String): Boolean =
    getAllSuperTypes().any { it.declaration.isClass(qualifiedSuperclassName) }

internal fun KSClassDeclaration.superclassType(qualifiedSuperclassName: String): KSType? =
    getAllSuperTypes().firstOrNull { it.declaration.isClass(qualifiedSuperclassName) }
//endregion

//region KSDeclaration
private fun KSDeclaration.isClass(qualifiedClassName: String): Boolean =
    qualifiedName?.asString() == qualifiedClassName
//endregion

//region KSPropertyDeclaration
internal const val PARAM_NAME_TYPE = "type"
internal val ProcessorColumnType: String = ColumnType::class.java.name
internal val KSPropertyDeclaration.columnTypeClassName: ClassName?
    get() = (annotation(ProcessorColumnType)?.arguments?.get(PARAM_NAME_TYPE) as? KSType)?.toClassName()

internal val KSPropertyDeclaration.typeName: String
    get() = type.resolve().declaration.simpleName.asString()
//endregion

//region KSValueArgument
private operator fun List<KSValueArgument>.get(name: String): Any? =
    find { it.name?.asString() == name }?.value
//endregion

internal fun <K, V> Map<out K, V?>.filterValuesNotNull(): Map<K, V> {
    val result = LinkedHashMap<K, V>()
    for (entry in this) entry.value?.let { result[entry.key] = it }
    return result
}

internal fun <T : Annotatable.Builder<T>> T.addAnnotationIf(annotationSpec: AnnotationSpec, condition: Boolean) =
    apply { if (condition) addAnnotation(annotationSpec) }
