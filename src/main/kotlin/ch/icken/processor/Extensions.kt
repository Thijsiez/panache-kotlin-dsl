/*
 * Copyright 2023-2026 Thijs Koppen
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

import com.google.devtools.ksp.symbol.*

//region KSAnnotated
internal fun KSAnnotated.annotation(qualifiedAnnotationClassName: String): KSAnnotation? =
    annotations.singleOrNull { it.isClass(qualifiedAnnotationClassName) }

internal fun KSAnnotated.hasAnnotation(qualifiedAnnotationClassName: String): Boolean =
    annotations.any { it.isClass(qualifiedAnnotationClassName) }
//endregion

//region KSAnnotation
private fun KSAnnotation.isClass(qualifiedAnnotationClassName: String): Boolean =
    annotationType.resolve().declaration.isClass(qualifiedAnnotationClassName)

internal fun KSAnnotation?.isParameterSet(parameterName: String): Boolean =
    this != null && defaultArguments[parameterName] != arguments[parameterName]
//endregion

//region KSDeclaration
internal fun KSDeclaration.isClass(qualifiedClassName: String): Boolean =
    this is KSClassDeclaration && qualifiedName?.asString() == qualifiedClassName
//endregion

//region KSValueArgument
internal operator fun List<KSValueArgument>.get(name: String): Any? =
    find { it.name?.asString() == name }?.value
//endregion
