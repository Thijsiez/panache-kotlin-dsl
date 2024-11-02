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

sealed class LogicalQueryComponent<Entity : PanacheEntityBase, Id : Any, Columns> private constructor(
    companion: PanacheCompanionBase<Entity, Id>,
    private val previous: QueryComponent<Entity, Id, Columns>,
    private val operator: String,
    private val expression: Expression<Columns>
) : QueryComponent<Entity, Id, Columns>(companion) {
    override fun compile(): Compiled {
        val compiledPrevious = previous.compile()
        val compiledExpression = expression.compile()
        return Compiled(
            query = "${compiledPrevious.query} $operator ${compiledExpression.expression}",
            parameters = compiledPrevious.parameters + compiledExpression.parameters
        )
    }

    class AndQueryComponent<Entity : PanacheEntityBase, Id : Any, Columns> internal constructor(
        companion: PanacheCompanionBase<Entity, Id>,
        previous: QueryComponent<Entity, Id, Columns>,
        expression: Expression<Columns>
    ) : LogicalQueryComponent<Entity, Id, Columns>(companion, previous, "AND", expression)

    class OrQueryComponent<Entity : PanacheEntityBase, Id : Any, Columns> internal constructor(
        companion: PanacheCompanionBase<Entity, Id>,
        previous: QueryComponent<Entity, Id, Columns>,
        expression: Expression<Columns>
    ) : LogicalQueryComponent<Entity, Id, Columns>(companion, previous, "OR", expression)
}
