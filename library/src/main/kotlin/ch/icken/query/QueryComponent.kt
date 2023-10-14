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

package ch.icken.query

import ch.icken.query.GroupQueryComponent.AndGroupQueryComponent
import ch.icken.query.GroupQueryComponent.OrGroupQueryComponent
import ch.icken.query.LogicalQueryComponent.AndQueryComponent
import ch.icken.query.LogicalQueryComponent.OrQueryComponent
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.panache.common.Sort

abstract class QueryComponent<Entity : PanacheEntityBase, Id : Any> internal constructor(
    private val companion: PanacheCompanionBase<Entity, Id>
) {
    abstract fun compile(): Compiled
    data class Compiled internal constructor(val query: String, val parameters: Map<String, Any>)

    //region Intermediate operations
    fun and(expression: BooleanExpression) = AndQueryComponent(companion, this, expression)
    fun andGroup(expression: BooleanExpression,
                 groupComponent: QueryComponent<Entity, Id>.() -> QueryComponent<Entity, Id>) =
        AndGroupQueryComponent(companion, this, expression, groupComponent)

    fun or(expression: BooleanExpression) = OrQueryComponent(companion, this, expression)
    fun orGroup(expression: BooleanExpression,
                groupComponent: QueryComponent<Entity, Id>.() -> QueryComponent<Entity, Id>) =
        OrGroupQueryComponent(companion, this, expression, groupComponent)
    //endregion

    //region Terminal operations
    fun count() = with(compile()) { companion.count(query, parameters) }
    fun delete() = with(compile()) { companion.delete(query, parameters) }
    fun find() = with(compile()) { companion.find(query, parameters) }
    fun find(sort: Sort) = with(compile()) { companion.find(query, sort, parameters) }
    fun stream() = with(compile()) { companion.stream(query, parameters) }
    fun stream(sort: Sort) = with(compile()) { companion.stream(query, sort, parameters) }

    fun getSingle() = find().singleResult()
    fun getSingleSafe() = find().singleResultSafe()
    fun getMultiple() = find().list()
    fun getMultiple(sort: Sort) = find(sort).list()
    //endregion
}
