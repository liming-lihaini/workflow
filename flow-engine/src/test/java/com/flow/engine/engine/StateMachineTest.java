package com.flow.engine.engine;

import com.flow.engine.common.BusinessException;
import com.flow.engine.common.enums.ProcessStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 状态机测试（ISSUE-004）
 */
class StateMachineTest {

    @Test
    @DisplayName("RUNNING -> COMPLETED 合法")
    void testRunningToCompleted() {
        assertTrue(ProcessStatus.RUNNING.canTransitionTo(ProcessStatus.COMPLETED));
    }

    @Test
    @DisplayName("RUNNING -> SUSPENDED 合法")
    void testRunningToSuspended() {
        assertTrue(ProcessStatus.RUNNING.canTransitionTo(ProcessStatus.SUSPENDED));
    }

    @Test
    @DisplayName("RUNNING -> TERMINATED 合法")
    void testRunningToTerminated() {
        assertTrue(ProcessStatus.RUNNING.canTransitionTo(ProcessStatus.TERMINATED));
    }

    @Test
    @DisplayName("SUSPENDED -> RUNNING 合法（恢复）")
    void testSuspendedToRunning() {
        assertTrue(ProcessStatus.SUSPENDED.canTransitionTo(ProcessStatus.RUNNING));
    }

    @Test
    @DisplayName("SUSPENDED -> TERMINATED 合法")
    void testSuspendedToTerminated() {
        assertTrue(ProcessStatus.SUSPENDED.canTransitionTo(ProcessStatus.TERMINATED));
    }

    @Test
    @DisplayName("TERMINATED -> 任何状态 不合法（终态不可逆）")
    void testTerminatedIsFinal() {
        assertFalse(ProcessStatus.TERMINATED.canTransitionTo(ProcessStatus.RUNNING));
        assertFalse(ProcessStatus.TERMINATED.canTransitionTo(ProcessStatus.COMPLETED));
        assertFalse(ProcessStatus.TERMINATED.canTransitionTo(ProcessStatus.SUSPENDED));
    }

    @Test
    @DisplayName("COMPLETED -> 任何状态 不合法（终态）")
    void testCompletedIsFinal() {
        assertFalse(ProcessStatus.COMPLETED.canTransitionTo(ProcessStatus.RUNNING));
        assertFalse(ProcessStatus.COMPLETED.canTransitionTo(ProcessStatus.SUSPENDED));
        assertFalse(ProcessStatus.COMPLETED.canTransitionTo(ProcessStatus.TERMINATED));
    }

    @Test
    @DisplayName("RUNNING -> RUNNING 不合法")
    void testRunningToRunning() {
        assertFalse(ProcessStatus.RUNNING.canTransitionTo(ProcessStatus.RUNNING));
    }

    @Test
    @DisplayName("fromValue 正确映射")
    void testFromValue() {
        assertEquals(ProcessStatus.RUNNING, ProcessStatus.fromValue(0));
        assertEquals(ProcessStatus.COMPLETED, ProcessStatus.fromValue(1));
        assertEquals(ProcessStatus.SUSPENDED, ProcessStatus.fromValue(2));
        assertEquals(ProcessStatus.TERMINATED, ProcessStatus.fromValue(3));
        assertNull(ProcessStatus.fromValue(99));
    }
}
