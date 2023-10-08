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

import ch.icken.query.BooleanExpression
import ch.icken.query.ColumnName
import ch.icken.query.LogicalQueryComponent
import ch.icken.query.QueryComponent
import com.squareup.kotlinpoet.asClassName

internal object ClassNames {
    val AndQueryComponentClassName = LogicalQueryComponent.AndQueryComponent::class.asClassName()
    val BooleanExpressionClassName = BooleanExpression::class.asClassName()
    val ColumnNameClassName = ColumnName::class.asClassName()
    val EnumTypeOrdinalClassName = ColumnName.EnumTypeOrdinal::class.asClassName()
    val EnumTypeStringClassName = ColumnName.EnumTypeString::class.asClassName()
    val JvmNameClassName = JvmName::class.asClassName()
    val OrQueryComponentClassName = LogicalQueryComponent.OrQueryComponent::class.asClassName()
    val QueryComponentClassName = QueryComponent::class.asClassName()
    val StringClassName = String::class.asClassName()
    val SuppressClassName = Suppress::class.asClassName()
}
