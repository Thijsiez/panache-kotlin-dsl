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

package ch.icken.model

import ch.icken.processor.ColumnType
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.*

@Entity
@Table(name = "ASSIGNMENT")
data class Assignment(

    @OneToOne(optional = false)
    @JoinColumn(name = "EMPLOYEE_ID", unique = true, nullable = false)
    val employee: Employee,

    @ManyToOne(optional = false)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    val client: Client,

    @Column(name = "ROLE", nullable = false)
    @ColumnType(type = CharSequence::class)
    var role: String,

    @Column(name = "DESCRIPTION")
    var description: String? = null

) : PanacheEntity() {

    companion object : PanacheCompanion<Assignment>
}
