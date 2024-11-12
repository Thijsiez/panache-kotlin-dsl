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

import ch.icken.model.generated.*
import ch.icken.query.*
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.time.LocalDate

@QuarkusTest
class QueryTests {

    @Test
    fun findJohn() {

        //WHERE FIRST_NAME = 'John'
        //Using type-safe sealed result wrapper
        val john = Employee.where { firstName eq "John" }.singleSafe()

        assertInstanceOf(PanacheSingleResult.Result::class.java, john)
    }

    @Test
    fun countNotMen() {

        //SELECT COUNT(*) FROM EMPLOYEE WHERE GENDER != 'M'
        val numberOfNotMen = Employee.count { gender neq Employee.Gender.M }

        assertEquals(4, numberOfNotMen)
    }

    @Test
    fun bornBeforeEpoch() {

        //WHERE BIRTH_DATE < 1970-01-01
        val bornBeforeEpoch = Employee.multiple { birthDate lt LocalDate.EPOCH }

        assertEquals(2, bornBeforeEpoch.size)
    }

    @Test
    fun findAllSons() {

        //WHERE LAST_NAME LIKE '%son'
        //Using find, which allows Panache pagination etc. to be used still
        val sons = Employee.find { lastName like "%son" }.list()

        assertEquals(2, sons.size)
    }

    @Test
    fun averageSalary() {

        //WHERE (SALARY > 50000.0 AND SALARY <= 60000.0)
        // OR SALARY BETWEEN 75000.0 AND 85000.0
        //Then we take the average using Java 8 streams
        val averageSalary = Employee
            .where {
                salary.gt(50_000.0)
                    .and { salary lte 60_000.0 }
            }
            .or { salary.between(75_000.0, 85_000.0) }
            .stream()
            .mapToDouble { it.salary }
            .average()
            .orElse(0.0)

        assertEquals(67_500.0, averageSalary)
    }
}
