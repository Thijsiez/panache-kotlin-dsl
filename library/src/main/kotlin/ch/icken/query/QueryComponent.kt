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

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.panache.common.Sort

abstract class QueryComponent<Entity : PanacheEntityBase, Id : Any> internal constructor(
    private val companion: PanacheCompanionBase<Entity, Id>
) {
    abstract fun compile(): String

    //region Intermediate operations
    fun and(expression: BooleanExpression) = LogicalQueryComponent.AndQueryComponent(companion, this, expression)
    fun or(expression: BooleanExpression) = LogicalQueryComponent.OrQueryComponent(companion, this, expression)

    fun andGroup(expression: BooleanExpression,
                 groupComponent: QueryComponent<Entity, Id>.() -> QueryComponent<Entity, Id>) =
        GroupQueryComponent.AndGroupQueryComponent(companion, this, expression, groupComponent)
    fun orGroup(expression: BooleanExpression,
                groupComponent: QueryComponent<Entity, Id>.() -> QueryComponent<Entity, Id>) =
        GroupQueryComponent.OrGroupQueryComponent(companion, this, expression, groupComponent)
    //endregion

    //region Terminal operations
    fun count() = companion.count(compile())
    fun delete() = companion.delete(compile())
    fun find() = companion.find(compile())
    fun find(sort: Sort) = companion.find(compile(), sort)
    fun stream() = companion.stream(compile())
    fun stream(sort: Sort) = companion.stream(compile(), sort)
    fun update() = companion.update(compile())

    fun getSingle() = find().singleResult()
    fun getMultiple() = find().list()
    fun getMultiple(sort: Sort) = find(sort).list()

    fun printQuery() = println("WHERE ${compile()}")
    //endregion
}
