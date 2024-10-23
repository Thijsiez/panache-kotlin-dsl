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

package ch.icken.query

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase

class WhereQueryComponent<Entity : PanacheEntityBase, Id : Any> internal constructor(
    companion: PanacheCompanionBase<Entity, Id>,
    expression: BooleanExpression
) : InitialQueryComponent<Entity, Id>(companion, expression)

class WhereGroupQueryComponent<Entity : PanacheEntityBase, Id : Any> internal constructor(
    companion: PanacheCompanionBase<Entity, Id>,
    expression: BooleanExpression,
    private val groupComponent: QueryComponent<Entity, Id>.() -> QueryComponent<Entity, Id>
) : QueryComponent<Entity, Id>(companion) {
    private val initialComponent = InitialQueryComponent(companion, expression)

    override fun compile(): Compiled {
        val compiledGroup = groupComponent.invoke(initialComponent).compile()
        return Compiled("(${compiledGroup.query})", compiledGroup.parameters)

    }
}

fun <Entity : PanacheEntityBase, Id : Any> PanacheCompanionBase<Entity, Id>.where(expression: BooleanExpression) =
    WhereQueryComponent(this, expression)

fun <Entity : PanacheEntityBase, Id : Any> PanacheCompanionBase<Entity, Id>.whereGroup(
    expression: BooleanExpression,
    groupComponent: QueryComponent<Entity, Id>.() -> QueryComponent<Entity, Id>
) = WhereGroupQueryComponent(this, expression, groupComponent)
