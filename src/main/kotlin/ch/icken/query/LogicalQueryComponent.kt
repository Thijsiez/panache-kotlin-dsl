package ch.icken.query

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase

sealed class LogicalQueryComponent<Entity : PanacheEntityBase, Id : Any> private constructor(
    companion: PanacheCompanionBase<Entity, Id>,
    private val previous: QueryComponent<Entity, Id>,
    private val operator: String,
    private val expression: BooleanExpression
) : QueryComponent<Entity, Id>(companion) {
    override fun compile() = "${previous.compile()} $operator ${expression.compile()}"

    class AndQueryComponent<Entity : PanacheEntityBase, Id : Any> internal constructor(
        companion: PanacheCompanionBase<Entity, Id>,
        previous: QueryComponent<Entity, Id>,
        expression: BooleanExpression
    ) : LogicalQueryComponent<Entity, Id>(companion, previous, "AND", expression)
    class OrQueryComponent<Entity : PanacheEntityBase, Id : Any> internal constructor(
        companion: PanacheCompanionBase<Entity, Id>,
        previous: QueryComponent<Entity, Id>,
        expression: BooleanExpression
    ) : LogicalQueryComponent<Entity, Id>(companion, previous, "OR", expression)
}
