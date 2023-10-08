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

    override fun compile(): String {
        val compiledGroup = groupComponent.invoke(initialComponent).compile()
        return "${previous.compile()} $operator ($compiledGroup)"
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
