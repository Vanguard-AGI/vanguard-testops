/**
 * Scheduling runtime services for the system domain.
 *
 * <p>This package contains schedule-oriented runtime services and managers that
 * coordinate Quartz-backed task registration, rescheduling, and removal for
 * system-level scheduled execution.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>generic runtime config that belongs under
 *   {@code system.runtime.config}</li>
 *   <li>controller-facing scheduling DTO assembly</li>
 *   <li>small reusable helper methods that belong under
 *   {@code system.runtime.schedule.support}</li>
 * </ul>
 */
package io.vanguard.testops.system.runtime.schedule;
