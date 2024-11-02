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

package ch.icken.query

sealed class LogicalExpression<Columns> private constructor(
    private val previous: Expression<Columns>,
    private val operator: String,
    private val expression: Expression<Columns>
) : Expression<Columns>() {
    override fun compileExpression(): Compiled {
        val compiledPrevious = previous.compileExpression()
        val compiledExpression = expression.compileExpression()
        return Compiled(
            expression = "${compiledPrevious.expression} $operator ${compiledExpression.expression}",
            parameters = compiledPrevious.parameters + compiledExpression.parameters
        )
    }

    class AndExpression<Columns> internal constructor(
        previous: Expression<Columns>,
        expression: Expression<Columns>
    ) : LogicalExpression<Columns>(previous, "AND", expression)
    class OrExpression<Columns> internal constructor(
        previous: Expression<Columns>,
        expression: Expression<Columns>
    ) : LogicalExpression<Columns>(previous, "OR", expression)
}
