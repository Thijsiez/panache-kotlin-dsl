/*
 * Copyright 2024 Thijs Koppen
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

import ch.icken.query.Column
import ch.icken.query.Component.QueryComponent
import ch.icken.query.Component.UpdateComponent
import ch.icken.query.Expression
import ch.icken.query.PanacheSingleResult
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.asClassName
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import io.quarkus.panache.common.Sort
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.stream.Stream

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

    protected fun jvmNameAnnotation(name: String) = AnnotationSpec.builder(JvmNameClassName)
        .addMember("%S", name)
        .build()
    //endregion

    companion object {
        //region Class Names
        val ColumnClassName = Column::class.asClassName()
        val ExpressionClassName = Expression::class.asClassName()
        val GeneratedClassName = Generated::class.asClassName()
        val JvmNameClassName = JvmName::class.asClassName()
        val ListClassName = List::class.asClassName()
        val LongClassName = Long::class.asClassName()
        val PanacheQueryClassName = PanacheQuery::class.asClassName()
        val PanacheSingleResultClassName = PanacheSingleResult::class.asClassName()
        val QueryComponentClassName = QueryComponent::class.asClassName()
        val SetterClassName = UpdateComponent.Setter::class.asClassName()
        val SortClassName = Sort::class.asClassName()
        val StreamClassName = Stream::class.asClassName()
        val StringClassName = String::class.asClassName()
        val SuppressClassName = Suppress::class.asClassName()
        val UpdateComponentClassName = UpdateComponent::class.asClassName()
        //endregion
        //region Constants
        const val CLASS_NAME_COMPANION = "Companion"
        const val FUNCTION_NAME_AND = "and"
        const val FUNCTION_NAME_AND_EXPRESSION = "andExpression"
        const val FUNCTION_NAME_COUNT = "count"
        const val FUNCTION_NAME_DELETE = "delete"
        const val FUNCTION_NAME_FIND = "find"
        const val FUNCTION_NAME_FIND_SORTED = "findSorted"
        const val FUNCTION_NAME_MULTIPLE = "multiple"
        const val FUNCTION_NAME_MULTIPLE_SORTED = "multipleSorted"
        const val FUNCTION_NAME_OR = "or"
        const val FUNCTION_NAME_OR_EXPRESSION = "orExpression"
        const val FUNCTION_NAME_SINGLE = "single"
        const val FUNCTION_NAME_SINGLE_SAFE = "singleSafe"
        const val FUNCTION_NAME_STREAM = "stream"
        const val FUNCTION_NAME_STREAM_SORTED = "streamSorted"
        const val FUNCTION_NAME_UPDATE = "update"
        const val FUNCTION_NAME_WHERE = "where"
        const val PARAM_NAME_COLUMNS_BASE_CLASS = "parent"
        const val PARAM_NAME_EXPRESSION = "expression"
        const val PARAM_NAME_MAPPED_BY = "mappedBy"
        const val PARAM_NAME_SETTERS = "setters"
        const val PARAM_NAME_SORT = "sort"
        const val PARAM_NAME_TYPE = "type"
        const val SUFFIX_CLASS_COLUMNS_BASE = "Base"
        const val SUFFIX_FILE_EXTENSIONS = "Extensions"
        const val SUFFIX_OBJECT_COLUMNS = "Columns"
        const val SUFFIX_PACKAGE_GENERATED = ".generated"
        const val TYPE_VARIABLE_NAME_COLUMNS = "Columns"
        //endregion
        //region Names
        val HibernatePanacheCompanionBase: String = PanacheCompanionBase::class.java.name
        val HibernatePanacheEntityBase: String = PanacheEntityBase::class.java.name
        val JakartaPersistenceEntity: String = Entity::class.java.name
        val JakartaPersistenceId: String = Id::class.java.name
        val JakartaPersistenceJoinColumn: String = JoinColumn::class.java.name
        val JakartaPersistenceManyToMany: String = ManyToMany::class.java.name
        val JakartaPersistenceOneToMany: String = OneToMany::class.java.name
        val JakartaPersistenceOneToOne: String = OneToOne::class.java.name
        val JakartaPersistenceTransient: String = Transient::class.java.name
        val ProcessorColumnType: String = ColumnType::class.java.name
        //endregion
        //region Options
        const val OPTION_ADD_GENERATED_ANNOTATION = "addGeneratedAnnotation"
        //endregion
    }
}
