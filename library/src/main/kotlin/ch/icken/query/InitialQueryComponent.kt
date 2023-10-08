package ch.icken.query

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase

open class InitialQueryComponent<Entity : PanacheEntityBase, Id : Any> internal constructor(
    companion: PanacheCompanionBase<Entity, Id>,
    private val expression: BooleanExpression
) : QueryComponent<Entity, Id>(companion) {
    override fun compile() = expression.compile()
}
