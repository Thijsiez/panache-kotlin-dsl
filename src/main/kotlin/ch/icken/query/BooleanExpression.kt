package ch.icken.query

sealed class BooleanExpression private constructor(
    private val key: String,
    private val operator: String,
    private val value: String
) {
    fun compile() = "$key $operator $value"

    class EqualTo internal constructor(key: String, value: String) : BooleanExpression(key, "=", value)
    class NotEqualTo internal constructor(key: String, value: String) : BooleanExpression(key, "!=", value)
    class LessThan internal constructor(key: String, value: String) : BooleanExpression(key, "<", value)
    class GreaterThan internal constructor(key: String, value: String) : BooleanExpression(key, ">", value)
    class LessThanOrEqualTo internal constructor(key: String, value: String) : BooleanExpression(key, "<=", value)
    class GreaterThanOrEqualTo internal constructor(key: String, value: String) : BooleanExpression(key, ">=", value)

    class IsNull internal constructor(key: String) : BooleanExpression(key, "IS", "NULL")
    class IsNotNull internal constructor(key: String) : BooleanExpression(key, "IS NOT", "NULL")

    class Like internal constructor(key: String, expression: String) : BooleanExpression(key, "LIKE", expression)
    class NotLike internal constructor(key: String, expression: String) : BooleanExpression(key, "NOT LIKE", expression)

    class Between internal constructor(key: String, a: String, b: String) :
        BooleanExpression(key, "BETWEEN", "$a AND $b")
    class NotBetween internal constructor(key: String, a: String, b: String) :
        BooleanExpression(key, "NOT BETWEEN", "$a AND $b")

    class In internal constructor(key: String, values: Collection<String>) :
        BooleanExpression(key, "IN", "(${values.joinToString()})")
    class NotIn internal constructor(key: String, values: Collection<String>) :
        BooleanExpression(key, "NOT IN", "(${values.joinToString()})")
}
