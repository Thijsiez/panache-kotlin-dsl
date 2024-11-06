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

@file:Suppress("unused")

package ch.icken.query

import ch.icken.query.Expression.BooleanExpression.BetweenExpression.Between
import ch.icken.query.Expression.BooleanExpression.BetweenExpression.NotBetween
import ch.icken.query.Expression.BooleanExpression.BooleanValueExpression.*
import ch.icken.query.Expression.BooleanExpression.IsExpression.IsNotNull
import ch.icken.query.Expression.BooleanExpression.IsExpression.IsNull

class Column<Columns, T : Any?>(internal val name: String) {
    operator fun invoke(value: T) = Component.UpdateComponent.InitialUpdateComponent.Setter(name, value)
}

//region eq
private fun <Columns> eq(name: String, value: Any?): Expression<Columns> =
    if (value == null) IsNull(name) else EqualTo(name, value)
@JvmName("eq")
infix fun <Columns, T : Any> Column<Columns, T>.eq(value: T) = eq<Columns>(name, value)
@JvmName("eqNullable")
infix fun <Columns, T : Any> Column<Columns, T?>.eq(value: T?) = eq<Columns>(name, value)
//endregion
//region neq
private fun <Columns> neq(name: String, value: Any?): Expression<Columns> =
    if (value == null) IsNotNull(name) else NotEqualTo(name, value)
@JvmName("neq")
infix fun <Columns, T : Any> Column<Columns, T>.neq(value: T) = neq<Columns>(name, value)
@JvmName("neqNullable")
infix fun <Columns, T : Any> Column<Columns, T?>.neq(value: T?) = neq<Columns>(name, value)
//endregion
//region lt
private fun <Columns> lt(name: String, value: Any): Expression<Columns> = LessThan(name, value)
@JvmName("lt")
infix fun <Columns, T : Any> Column<Columns, T>.lt(value: T) = lt<Columns>(name, value)
@JvmName("ltNullable")
infix fun <Columns, T : Any> Column<Columns, T?>.lt(value: T) = lt<Columns>(name, value)
//endregion
//region gt
private fun <Columns> gt(name: String, value: Any): Expression<Columns> = GreaterThan(name, value)
@JvmName("gt")
infix fun <Columns, T : Any> Column<Columns, T>.gt(value: T) = gt<Columns>(name, value)
@JvmName("gtNullable")
infix fun <Columns, T : Any> Column<Columns, T?>.gt(value: T) = gt<Columns>(name, value)
//endregion
//region lte
private fun <Columns> lte(name: String, value: Any): Expression<Columns> = LessThanOrEqualTo(name, value)
@JvmName("lte")
infix fun <Columns, T : Any> Column<Columns, T>.lte(value: T) = lte<Columns>(name, value)
@JvmName("lteNullable")
infix fun <Columns, T : Any> Column<Columns, T?>.lte(value: T) = lte<Columns>(name, value)
//endregion
//region gte
private fun <Columns> gte(name: String, value: Any): Expression<Columns> = GreaterThanOrEqualTo(name, value)
@JvmName("gte")
infix fun <Columns, T : Any> Column<Columns, T>.gte(value: T) = gte<Columns>(name, value)
@JvmName("gteNullable")
infix fun <Columns, T : Any> Column<Columns, T?>.gte(value: T) = gte<Columns>(name, value)
//endregion

//region in
private fun <Columns> `in`(name: String, values: Collection<Any>): Expression<Columns> = In(name, values)
@JvmName("in")
infix fun <Columns, T : Any> Column<Columns, T>.`in`(values: Collection<T>) = `in`<Columns>(name, values)
@JvmName("inNullable")
infix fun <Columns, T : Any> Column<Columns, T?>.`in`(values: Collection<T>) = `in`<Columns>(name, values)
//endregion
//region notIn
private fun <Columns> notIn(name: String, values: Collection<Any>): Expression<Columns> = NotIn(name, values)
@JvmName("notIn")
infix fun <Columns, T : Any> Column<Columns, T>.notIn(values: Collection<T>) = notIn<Columns>(name, values)
@JvmName("notInNullable")
infix fun <Columns, T : Any> Column<Columns, T?>.notIn(values: Collection<T>) = notIn<Columns>(name, values)
//endregion

//region like
private fun <Columns> like(name: String, expression: String?): Expression<Columns> =
    if (expression == null) IsNull(name) else Like(name, expression)
@JvmName("like")
infix fun <Columns> Column<Columns, String>.like(expression: String) = like<Columns>(name, expression)
@JvmName("likeNullable")
infix fun <Columns> Column<Columns, String?>.like(expression: String?) = like<Columns>(name, expression)
//endregion
//region notLike
private fun <Columns> notLike(name: String, expression: String?): Expression<Columns> =
    if (expression == null) IsNotNull(name) else NotLike(name, expression)
@JvmName("notLike")
infix fun <Columns> Column<Columns, String>.notLike(expression: String) = notLike<Columns>(name, expression)
@JvmName("notLikeNullable")
infix fun <Columns> Column<Columns, String?>.notLike(expression: String?) = notLike<Columns>(name, expression)
//endregion

//region between
private fun <Columns> between(name: String, min: Any?, maxIncl: Any?): Expression<Columns> = when {
    min != null && maxIncl != null -> Between(name, min, maxIncl)
    min != null && maxIncl == null -> GreaterThanOrEqualTo(name, min)
    min == null && maxIncl != null -> LessThanOrEqualTo(name, maxIncl)
    else -> IsNull(name)
}
@JvmName("between")
fun <Columns, T : Any> Column<Columns, T>.between(min: T, maxIncl: T) = between<Columns>(name, min, maxIncl)
@JvmName("betweenNullable")
fun <Columns, T : Any> Column<Columns, T?>.between(min: T?, maxIncl: T?) = between<Columns>(name, min, maxIncl)
//endregion
//region notBetween
private fun <Columns> notBetween(name: String, min: Any?, maxIncl: Any?): Expression<Columns> = when {
    min != null && maxIncl != null -> NotBetween(name, min, maxIncl)
    min != null && maxIncl == null -> LessThan(name, min)
    min == null && maxIncl != null -> GreaterThan(name, maxIncl)
    else -> IsNotNull(name)
}
@JvmName("notBetween")
fun <Columns, T : Any> Column<Columns, T>.notBetween(min: T, maxIncl: T) = notBetween<Columns>(name, min, maxIncl)
@JvmName("notBetweenNullable")
fun <Columns, T : Any> Column<Columns, T?>.notBetween(min: T?, maxIncl: T?) = notBetween<Columns>(name, min, maxIncl)
//endregion
