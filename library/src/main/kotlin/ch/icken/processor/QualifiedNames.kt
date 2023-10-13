/*
 * Copyright 2023 Thijs Koppen
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
