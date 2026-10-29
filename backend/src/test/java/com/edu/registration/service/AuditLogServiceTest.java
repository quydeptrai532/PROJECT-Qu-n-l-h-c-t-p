package com.edu.registration.service;

import com.edu.registration.entity.AuditLog;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.AuditLogRepository;
import com.edu.registration.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository repository;
    @Mock private CurrentUser currentUser;
    @Mock private PlatformTransactionManager transactionManager;

    private AuditLogService auditLogService;
    private User actor;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(repository, currentUser, new ObjectMapper(), transactionManager);
        actor = new User();
        actor.setId(47L);
        actor.setUsername("admin.qa");
        actor.setRole(Role.QUAN_TRI);
        when(currentUser.get()).thenReturn(actor);
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void successCapturesServerActorTargetChangesAndCorrelationWithoutSensitiveFields() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "req-47.qa");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String value = auditLogService.execute("COURSE_SECTION_UPDATED", "COURSE_SECTION", 12L, audit -> {
            audit.changes(
                    Map.of("maLop", "IT201.01", "siSoToiDa", 30, "password", "do-not-store", "token", "secret"),
                    Map.of("maLop", "IT201.01", "siSoToiDa", 35, "email", "private@example.test"));
            return "updated";
        });

        assertEquals("updated", value);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).saveAndFlush(captor.capture());
        AuditLog event = captor.getValue();
        assertEquals(47L, event.getActorUserId());
        assertEquals("admin.qa", event.getActorUsername());
        assertEquals("QUAN_TRI", event.getActorRole());
        assertEquals("COURSE_SECTION_UPDATED", event.getAction());
        assertEquals("COURSE_SECTION", event.getEntityType());
        assertEquals("12", event.getEntityId());
        assertEquals("SUCCESS", event.getResult());
        assertEquals("req-47.qa", event.getCorrelationId());
        assertTrue(event.getDetailsJson().contains("35"));
        assertFalse(event.getDetailsJson().contains("password"));
        assertFalse(event.getDetailsJson().contains("token"));
        assertFalse(event.getDetailsJson().contains("email"));
    }

    @Test
    void businessFailureIsRecordedWithoutExceptionMessageThenRethrown() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        doNothing().when(transactionManager).commit(any(TransactionStatus.class));
        ApiException failure = ApiException.conflict("message contains private value");

        ApiException thrown = assertThrows(ApiException.class, () -> auditLogService.execute(
                "COURSE_REGISTRATION", "COURSE_SECTION", 88L,
                audit -> { throw failure; }));

        assertEquals(failure, thrown);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).saveAndFlush(captor.capture());
        AuditLog event = captor.getValue();
        assertEquals("FAILURE", event.getResult());
        assertEquals("88", event.getEntityId());
        assertTrue(event.getDetailsJson().contains("HTTP_409"));
        assertFalse(event.getDetailsJson().contains("private value"));
    }

    @Test
    void auditPersistenceFailureDoesNotReturnBusinessSuccess() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        AtomicBoolean operationRan = new AtomicBoolean();
        when(repository.saveAndFlush(any(AuditLog.class)))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThrows(DataAccessResourceFailureException.class, () -> auditLogService.execute(
                "COURSE_SECTION_UPDATED", "COURSE_SECTION", 12L,
                audit -> { operationRan.set(true); return "must not be returned"; }));

        assertTrue(operationRan.get());
        verify(repository, org.mockito.Mockito.times(2)).saveAndFlush(any(AuditLog.class));
    }
}
