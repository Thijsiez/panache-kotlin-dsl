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

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.*
import java.time.LocalDate

@Suppress("unused")
@Entity
@Table(name = "EMPLOYEE")
class Employee(

    @Column(name = "EMPLOYEE_NO", unique = true, nullable = false, updatable = false)
    val employeeNumber: Int,

    @Column(name = "FIRST_NAME", nullable = false)
    var firstName: String,

    @Column(name = "LAST_NAME", nullable = false)
    var lastName: String,

    @Column(name = "GENDER", nullable = false)
    @Enumerated(EnumType.STRING)
    var gender: Gender,

    @Column(name = "BIRTH_DATE", nullable = false, updatable = false)
    var birthDate: LocalDate,

    @ManyToOne(optional = false)
    @JoinColumn(name = "DEPARTMENT_ID", nullable = false)
    var department: Department,

    @Column(name = "SALARY", nullable = false)
    var salary: Double,

    @OneToOne(cascade = [CascadeType.ALL], mappedBy = "employee", orphanRemoval = true)
    var assignment: Assignment? = null

) : PanacheEntity() {

    companion object : PanacheCompanion<Employee>

    enum class Gender {
        M, F, X
    }
}
