// src/test/java/com/elysion/user/util/SecurityUtilTest.java
package com.elysion.user.unit.util;

import com.elysion.user.exception.AccessDeniedException;
import com.elysion.user.util.SecurityUtil;
import org.junit.jupiter.api.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityUtilTest {

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUsername_noAuthentication_throws() {
        // SecurityContextHolder.getContext().getAuthentication() == null
        assertThrows(AccessDeniedException.class,
                SecurityUtil::getCurrentUsername);
    }

    @Test
    void getCurrentUsername_anonymousUser_throws() {
        Authentication anon = mock(Authentication.class);
        when(anon.isAuthenticated()).thenReturn(true);
        when(anon.getPrincipal()).thenReturn("anonymousUser");
        SecurityContextHolder.getContext().setAuthentication(anon);

        assertThrows(AccessDeniedException.class,
                SecurityUtil::getCurrentUsername);
    }

    @Test
    void getCurrentUsername_validAuth_returnsName() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("myUser");
        when(auth.getName()).thenReturn("myUser");
        SecurityContextHolder.getContext().setAuthentication(auth);

        String username = SecurityUtil.getCurrentUsername();
        assertEquals("myUser", username);
    }

    @Test
    void getCurrentUsername_notAuthenticated_throws() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(AccessDeniedException.class,
                SecurityUtil::getCurrentUsername);
    }
}