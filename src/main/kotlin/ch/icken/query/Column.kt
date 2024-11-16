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

class Column<Columns, Type : Any?>(internal val name: String) {
    /**
     * Adds a setter expression to this update query
     *
     * @param value the new value for this column
     */
    infix fun set(value: Type) = Component.UpdateComponent.InitialUpdateComponent.Setter(name, value)
}

//region eq
private fun <Columns> eq(name: String, value: Any?): Expression<Columns> =
    if (value == null) IsNull(name) else EqualTo(name, value)
/**
 * TODO
 */
@JvmName("eq")
infix fun <Columns, Type : Any> Column<Columns, Type>.eq(value: Type) = eq<Columns>(name, value)
/**
 * TODO
 */
@JvmName("eqNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.eq(value: Type?) = eq<Columns>(name, value)
//endregion
//region neq
private fun <Columns> neq(name: String, value: Any?): Expression<Columns> =
    if (value == null) IsNotNull(name) else NotEqualTo(name, value)
/**
 * TODO
 */
@JvmName("neq")
infix fun <Columns, Type : Any> Column<Columns, Type>.neq(value: Type) = neq<Columns>(name, value)
/**
 * TODO
 */
@JvmName("neqNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.neq(value: Type?) = neq<Columns>(name, value)
//endregion
//region lt
private fun <Columns> lt(name: String, value: Any): Expression<Columns> = LessThan(name, value)
/**
 * TODO
 */
@JvmName("lt")
infix fun <Columns, Type : Any> Column<Columns, Type>.lt(value: Type) = lt<Columns>(name, value)
/**
 * TODO
 */
@JvmName("ltNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.lt(value: Type) = lt<Columns>(name, value)
//endregion
//region gt
private fun <Columns> gt(name: String, value: Any): Expression<Columns> = GreaterThan(name, value)
/**
 * TODO
 */
@JvmName("gt")
infix fun <Columns, Type : Any> Column<Columns, Type>.gt(value: Type) = gt<Columns>(name, value)
/**
 * TODO
 */
@JvmName("gtNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.gt(value: Type) = gt<Columns>(name, value)
//endregion
//region lte
private fun <Columns> lte(name: String, value: Any): Expression<Columns> = LessThanOrEqualTo(name, value)
/**
 * TODO
 */
@JvmName("lte")
infix fun <Columns, Type : Any> Column<Columns, Type>.lte(value: Type) = lte<Columns>(name, value)
/**
 * TODO
 */
@JvmName("lteNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.lte(value: Type) = lte<Columns>(name, value)
//endregion
//region gte
private fun <Columns> gte(name: String, value: Any): Expression<Columns> = GreaterThanOrEqualTo(name, value)
/**
 * TODO
 */
@JvmName("gte")
infix fun <Columns, Type : Any> Column<Columns, Type>.gte(value: Type) = gte<Columns>(name, value)
/**
 * TODO
 */
@JvmName("gteNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.gte(value: Type) = gte<Columns>(name, value)
//endregion

//region in
private fun <Columns> `in`(name: String, values: Collection<Any>): Expression<Columns> = In(name, values)
/**
 * TODO
 */
@JvmName("in")
infix fun <Columns, Type : Any> Column<Columns, Type>.`in`(values: Collection<Type>) = `in`<Columns>(name, values)
/**
 * TODO
 */
@JvmName("inNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.`in`(values: Collection<Type>) = `in`<Columns>(name, values)
//endregion
//region notIn
private fun <Columns> notIn(name: String, values: Collection<Any>): Expression<Columns> = NotIn(name, values)
/**
 * TODO
 */
@JvmName("notIn")
infix fun <Columns, Type : Any> Column<Columns, Type>.notIn(values: Collection<Type>) = notIn<Columns>(name, values)
/**
 * TODO
 */
@JvmName("notInNullable")
infix fun <Columns, Type : Any> Column<Columns, Type?>.notIn(values: Collection<Type>) = notIn<Columns>(name, values)
//endregion

//region like
private fun <Columns> like(name: String, expression: String?): Expression<Columns> =
    if (expression == null) IsNull(name) else Like(name, expression)
/**
 * TODO
 */
@JvmName("like")
infix fun <Columns> Column<Columns, String>.like(expression: String) = like<Columns>(name, expression)
/**
 * TODO
 */
@JvmName("likeNullable")
infix fun <Columns> Column<Columns, String?>.like(expression: String?) = like<Columns>(name, expression)
//TODO startsWith
//TODO contains
//TODO endsWith
//endregion
//region notLike
private fun <Columns> notLike(name: String, expression: String?): Expression<Columns> =
    if (expression == null) IsNotNull(name) else NotLike(name, expression)
/**
 * TODO
 */
@JvmName("notLike")
infix fun <Columns> Column<Columns, String>.notLike(expression: String) = notLike<Columns>(name, expression)
/**
 * TODO
 */
@JvmName("notLikeNullable")
infix fun <Columns> Column<Columns, String?>.notLike(expression: String?) = notLike<Columns>(name, expression)
//TODO notStartsWith
//TODO notContains
//TODO notEndsWith
//endregion

//region between
private fun <Columns> between(name: String, min: Any?, maxIncl: Any?): Expression<Columns> = when {
    min != null && maxIncl != null -> Between(name, min, maxIncl)
    min != null && maxIncl == null -> GreaterThanOrEqualTo(name, min)
    min == null && maxIncl != null -> LessThanOrEqualTo(name, maxIncl)
    else -> IsNull(name)
}
/**
 * TODO
 */
@JvmName("between")
fun <Columns, Type : Any> Column<Columns, Type>.between(min: Type, maxIncl: Type) =
    between<Columns>(name, min, maxIncl)
/**
 * TODO
 */
@JvmName("betweenNullable")
fun <Columns, Type : Any> Column<Columns, Type?>.between(min: Type?, maxIncl: Type?) =
    between<Columns>(name, min, maxIncl)
//endregion
//region notBetween
private fun <Columns> notBetween(name: String, min: Any?, maxIncl: Any?): Expression<Columns> = when {
    min != null && maxIncl != null -> NotBetween(name, min, maxIncl)
    min != null && maxIncl == null -> LessThan(name, min)
    min == null && maxIncl != null -> GreaterThan(name, maxIncl)
    else -> IsNotNull(name)
}
/**
 * TODO
 */
@JvmName("notBetween")
fun <Columns, Type : Any> Column<Columns, Type>.notBetween(min: Type, maxIncl: Type) =
    notBetween<Columns>(name, min, maxIncl)
/**
 * TODO
 */
@JvmName("notBetweenNullable")
fun <Columns, Type : Any> Column<Columns, Type?>.notBetween(min: Type?, maxIncl: Type?) =
    notBetween<Columns>(name, min, maxIncl)
//endregion
