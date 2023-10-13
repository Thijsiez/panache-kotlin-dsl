/*
 * Copyright 2023 Thijs Koppen
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

internal fun KSDeclaration.isClass(qualifiedClassName: String): Boolean =
    qualifiedName?.asString() == qualifiedClassName

internal fun KSClassDeclaration.isSubclass(qualifiedSuperclassName: String): Boolean =
    getAllSuperTypes().map { it.declaration }
        .any { it.isClass(qualifiedSuperclassName) }

internal fun KSAnnotation.isClass(qualifiedAnnotationName: String): Boolean =
    annotationType.resolve().declaration.isClass(qualifiedAnnotationName)

internal fun KSAnnotated.hasAnnotation(qualifiedAnnotationName: String): Boolean =
    annotations.any { it.isClass(qualifiedAnnotationName) }

internal val KSPropertyDeclaration.typeName: String
    get() = type.resolve().declaration.simpleName.asString()

fun <T : Annotatable.Builder<T>> T.addAnnotationIf(annotationSpec: AnnotationSpec, condition: Boolean) =
    apply { if (condition) addAnnotation(annotationSpec) }
