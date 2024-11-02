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

sealed class Expression<Columns> {
    internal fun compile() = when (this) {
        is BooleanExpression -> compileExpression()
        is LogicalExpression -> {
            val compiledExpression = compileExpression()
            Compiled("(${compiledExpression.expression})", compiledExpression.parameters)
        }
    }

    internal abstract fun compileExpression(): Compiled
    data class Compiled internal constructor(val expression: String, val parameters: Map<String, Any>)

    fun and(expression: Expression<Columns>) = LogicalExpression.AndExpression(this, expression)
    fun or(expression: Expression<Columns>) = LogicalExpression.OrExpression(this, expression)
}
