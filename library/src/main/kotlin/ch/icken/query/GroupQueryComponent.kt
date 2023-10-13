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

sealed class GroupQueryComponent<Entity : PanacheEntityBase, Id : Any> private constructor(
    companion: PanacheCompanionBase<Entity, Id>,
    private val previous: QueryComponent<Entity, Id>,
    private val operator: String,
    expression: BooleanExpression,
    private val groupComponent: QueryComponent<Entity, Id>.() -> QueryComponent<Entity, Id>
) : QueryComponent<Entity, Id>(companion) {
    private val initialComponent = InitialQueryComponent(companion, expression)

    override fun compile(): Compiled {
        val compiledPrevious = previous.compile()
        val compiledGroup = groupComponent.invoke(initialComponent).compile()
        return Compiled(
            query = "${compiledPrevious.query} $operator (${compiledGroup.query})",
            parameters = compiledPrevious.parameters + compiledGroup.parameters
        )
    }

    class AndGroupQueryComponent<Entity : PanacheEntityBase, Id : Any> internal constructor(
        companion: PanacheCompanionBase<Entity, Id>,
        previous: QueryComponent<Entity, Id>,
        expression: BooleanExpression,
        groupComponent: QueryComponent<Entity, Id>.() -> QueryComponent<Entity, Id>
    ) : GroupQueryComponent<Entity, Id>(companion, previous, "AND", expression, groupComponent)

    class OrGroupQueryComponent<Entity : PanacheEntityBase, Id : Any> internal constructor(
        companion: PanacheCompanionBase<Entity, Id>,
        previous: QueryComponent<Entity, Id>,
        expression: BooleanExpression,
        groupComponent: QueryComponent<Entity, Id>.() -> QueryComponent<Entity, Id>
    ) : GroupQueryComponent<Entity, Id>(companion, previous, "OR", expression, groupComponent)
}
