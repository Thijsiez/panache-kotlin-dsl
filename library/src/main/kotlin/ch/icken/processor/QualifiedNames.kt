package ch.icken.processor

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn

internal object QualifiedNames {
    val HibernatePanacheCompanionBase: String = PanacheCompanionBase::class.java.name
    val HibernatePanacheEntityBase: String = PanacheEntityBase::class.java.name
    val JakartaPersistenceColumn: String = Column::class.java.name
    val JakartaPersistenceEntity: String = Entity::class.java.name
    val JakartaPersistenceId: String = Id::class.java.name
    val JakartaPersistenceJoinColumn: String = JoinColumn::class.java.name
}
