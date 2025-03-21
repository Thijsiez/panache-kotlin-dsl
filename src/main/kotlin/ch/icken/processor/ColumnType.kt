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

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.reflect.KClass

/**
 * Specifies the generic type of the generated [Column][ch.icken.query.Column] to match the type as used by Hibernate.
 *
 * The intended use case for this annotation is when the column's Kotlin type is different from your database.
 * When using JPA's [@Convert][jakarta.persistence.Convert] annotation, Hibernate converts to and from the type as
 * specified by the [@Converter][jakarta.persistence.Converter]. Panache uses this other type in its queries,
 * so it needs to be known during `Column` generation.
 *
 * @property    type    the generic type to be used by the generated `Column`
 */
@Retention(SOURCE)
@Target(PROPERTY)
annotation class ColumnType(
    val type: KClass<*>
)
