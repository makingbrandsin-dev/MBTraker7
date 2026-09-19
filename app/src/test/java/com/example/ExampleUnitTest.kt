package com.example

import com.example.data.auth.AppRole
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying Firestore Auth Provider role parsing and UI redirection routing.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `admin role strings redirect to manager dashboard`() {
    val adminRoles = listOf("admin", "ADMIN", "MB Admin", "Manager", "Executive Director", "Lead")
    for (role in adminRoles) {
      val parsed = AppRole.parse(role)
      assertEquals("Expected ADMIN for $role", AppRole.ADMIN, parsed)
      assertTrue("Expected isAdmin=true for $role", parsed.isAdmin)
      assertEquals("Expected manager route for $role", "manager", parsed.targetRoute)
    }
  }

  @Test
  fun `employee role strings redirect to home employee workspace`() {
    val employeeRoles = listOf("employee", "EMPLOYEE", "Developer", "Designer", null, "")
    for (role in employeeRoles) {
      val parsed = AppRole.parse(role)
      assertEquals("Expected EMPLOYEE for $role", AppRole.EMPLOYEE, parsed)
      assertFalse("Expected isAdmin=false for $role", parsed.isAdmin)
      assertEquals("Expected home route for $role", "home", parsed.targetRoute)
    }
  }
}
