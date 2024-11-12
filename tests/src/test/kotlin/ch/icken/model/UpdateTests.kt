/*
 * Copyright 2024 Thijs Koppen
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

import ch.icken.model.generated.count
import ch.icken.model.generated.update
import ch.icken.model.generated.updateAll
import ch.icken.model.generated.where
import ch.icken.query.eq
import ch.icken.query.gte
import ch.icken.query.lt
import io.quarkus.test.TestTransaction
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@QuarkusTest
class UpdateTests {

    @Test
    @TestTransaction
    fun testUpdateAll() {

        // Given

        // When
        //UPDATE EMPLOYEE SET SALARY = 0.0
        val entitiesUpdated = Employee.updateAll { salary(0.0) }

        // Then
        assertEquals(7, entitiesUpdated)
        assertEquals(7, Employee.count { salary eq 0.0 })
        assertTrue(Employee.listAll().all { it.salary == 0.0 })
    }

    @Test
    @TestTransaction
    fun testUpdateWhere() {

        // Given

        // When
        //UPDATE EMPLOYEE SET SALARY = 100000.0 WHERE SALARY < 100000.0
        val entitiesUpdated = Employee
            .update { salary(100_000.0) }
            .where { salary lt 100_000.0 }
            .execute()

        // Then
        assertEquals(4, entitiesUpdated)
        assertEquals(7, Employee.count { salary gte 100_000.0 })
    }
}
