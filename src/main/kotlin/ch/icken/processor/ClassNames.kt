/*
 * Copyright 2023-2024 Thijs Koppen
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

import ch.icken.query.*
import com.squareup.kotlinpoet.asClassName
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import io.quarkus.panache.common.Sort
import java.util.stream.Stream

internal object ClassNames {
    val AndQueryComponentClassName = LogicalQueryComponent.AndQueryComponent::class.asClassName()
    val BooleanExpressionClassName = BooleanExpression::class.asClassName()
    val ColumnClassName = Column::class.asClassName()
    val GeneratedClassName = Generated::class.asClassName()
    val JvmNameClassName = JvmName::class.asClassName()
    val ListClassName = List::class.asClassName()
    val LongClassName = Long::class.asClassName()
    val OrQueryComponentClassName = LogicalQueryComponent.OrQueryComponent::class.asClassName()
    val PanacheQueryClassName = PanacheQuery::class.asClassName()
    val PanacheSingleResultClassName = PanacheSingleResult::class.asClassName()
    val QueryComponentClassName = QueryComponent::class.asClassName()
    val SortClassName = Sort::class.asClassName()
    val StreamClassName = Stream::class.asClassName()
    val StringClassName = String::class.asClassName()
    val SuppressClassName = Suppress::class.asClassName()
}
