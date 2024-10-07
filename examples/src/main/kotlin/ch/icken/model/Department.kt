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

package ch.icken.model

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.*

@Entity
@Table(name = "DEPARTMENT")
data class Department(

    @Column(name = "NAME", unique = true,  nullable = false)
    var name: String,

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "department", orphanRemoval = true)
    val employees: MutableSet<Employee> = HashSet()

) : PanacheEntity() {

    operator fun plusAssign(employee: Employee) {
        employees.add(employee)
    }
    operator fun minusAssign(employee: Employee) {
        employees.remove(employee)
    }

    companion object : PanacheCompanion<Department>
}
