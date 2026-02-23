/*
 * Copyright 2024-2026 Thijs Koppen
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

import ch.icken.processor.model.KSClassDeclarationWithSuperTypes
import ch.icken.processor.model.KSClassDeclarationWrapper
import ch.icken.processor.model.withSuperTypes
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.Annotatable
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import java.time.LocalDateTime

internal sealed class Processor(options: Map<String, String>) : SymbolProcessor {

    protected val KSClassDeclarationWrapper.columnsObjectName get() = ksClassDeclaration.columnsObjectName
    protected val KSClassDeclarationWrapper.extensionFileName get() = simpleName + SUFFIX_FILE_EXTENSIONS
    protected val KSClassDeclarationWrapper.generatedPackageName get() = ksClassDeclaration.generatedPackageName
    protected val KSClassDeclarationWrapper.simpleName get() = ksClassDeclaration.simpleName.asString()

    protected val KSDeclaration.columnsObjectName get() = simpleName.asString() + SUFFIX_OBJECT_COLUMNS
    protected val KSDeclaration.generatedPackageName get() = packageName.asString() + SUFFIX_PACKAGE_GENERATED

    protected fun List<KSAnnotated>.filterPanacheEntities(): List<KSClassDeclarationWithSuperTypes> =
        filterIsInstance<KSClassDeclaration>()
            .map { it.withSuperTypes() }
            .filter { it.isSubclass(HIBERNATE_PANACHE_ENTITY_BASE) }

    protected fun KSClassDeclarationWrapper.toClassName(): ClassName = ksClassDeclaration.toClassName()

    //region Options
    protected val addGeneratedAnnotation = options[OPTION_ADD_GENERATED_ANNOTATION].toBoolean()
    //endregion

    //region Annotations
    protected val suppressFileAnnotation = AnnotationSpec.builder(SuppressClassName)
        .addMember("%S", "RedundantVisibilityModifier")
        .addMember("%S", "unused")
        .build()

    protected val generatedAnnotation = AnnotationSpec.builder(GeneratedClassName)
        .addMember("%S", javaClass.name)
        .addMember("%S", LocalDateTime.now().toString())
        .addMember("%S", "Generated using panache-kotlin-dsl")
        .build()

    protected fun <T : Annotatable.Builder<T>> T.addGeneratedAnnotation() =
        apply { if (addGeneratedAnnotation) addAnnotation(generatedAnnotation) }
    //endregion

    internal companion object {
        //region Class Names
        private val GeneratedClassName = ClassName("ch.icken.processor", "Generated")
        internal val LongClassName = ClassName("kotlin", "Long")
        private val SuppressClassName = ClassName("kotlin", "Suppress")
        //endregion
        //region Constants
        private const val SUFFIX_FILE_EXTENSIONS = "Extensions"
        private const val SUFFIX_OBJECT_COLUMNS = "Columns"
        private const val SUFFIX_PACKAGE_GENERATED = ".generated"
        //endregion
        //region Names
        internal const val HIBERNATE_PANACHE_ENTITY_BASE: String =
            "io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase"
        internal const val JAKARTA_PERSISTENCE_ENTITY: String = "jakarta.persistence.Entity"
        protected const val QUERY_PACKAGE: String = "ch.icken.query"
        //endregion
        //region Options
        internal const val OPTION_ADD_GENERATED_ANNOTATION = "addGeneratedAnnotation"
        //endregion
    }
}
