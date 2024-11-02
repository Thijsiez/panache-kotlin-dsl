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

package ch.icken

import ch.icken.model.Employee
import ch.icken.model.generated.*
import ch.icken.query.*
import java.time.LocalDate

//WHERE FIRST_NAME = 'John'
//Using type-safe sealed result wrapper
fun findJohn() = Employee.where { firstName eq "John" }.getSingleSafe()

//SELECT COUNT(*) FROM EMPLOYEE WHERE GENDER != 'M'
fun countNotMen() = Employee.count { gender neq Employee.Gender.M }

//WHERE BIRTH_DATE < 1970-01-01
fun bornBefore(date: LocalDate) = Employee.multiple { birthDate lt date }

//WHERE LAST_NAME LIKE '%son'
//Using find, which allows Panache pagination etc. to be used still
fun findAllSons() = Employee.find { lastName like "%son" }.list()

//WHERE (SALARY > 50000.0 AND SALARY <= 60000.0)
// OR SALARY BETWEEN 75000.0 AND 85000.0
//Then we take the average using Java 8 streams
fun averageOfVerySpecificSalaryRanges() =
    Employee.where {
            salary.gt(50_000.0)
                .and { salary lte 60_000.0 }
        }
        .or { salary.between(75_000.0, 85_000.0) }
        .stream()
        .mapToDouble { it.salary }
        .average()
        .orElse(0.0)
