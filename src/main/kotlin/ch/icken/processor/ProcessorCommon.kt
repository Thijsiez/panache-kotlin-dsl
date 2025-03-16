/*
 * Copyright 2024-2025 Thijs Koppen
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

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.asClassName
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Entity
import java.time.LocalDateTime

abstract class ProcessorCommon(options: Map<String, String>) {

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
    //endregion

    companion object {
        //region Class Names
        internal val GeneratedClassName = Generated::class.asClassName()
        internal val SuppressClassName = Suppress::class.asClassName()
        //endregion
        //region Constants
        internal const val PARAM_NAME_TYPE = "type"
        internal const val SUFFIX_OBJECT_COLUMNS = "Columns"
        internal const val SUFFIX_PACKAGE_GENERATED = ".generated"
        //endregion
        //region Names
        internal val HibernatePanacheEntityBase: String = PanacheEntityBase::class.java.name
        internal val JakartaPersistenceEntity: String = Entity::class.java.name
        internal val ProcessorColumnType: String = ColumnType::class.java.name
        //endregion
        //region Options
        internal const val OPTION_ADD_GENERATED_ANNOTATION = "addGeneratedAnnotation"
        //endregion
    }
}
