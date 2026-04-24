/**
 * System-level websocket runtime support.
 *
 * <p>This package contains websocket handlers and socket-facing runtime helpers
 * that support long-lived technical communication flows such as export progress
 * delivery.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>HTTP controller logic</li>
 *   <li>business orchestration unrelated to websocket delivery</li>
 *   <li>workflow-specific websocket support that belongs under
 *   {@code workflow.support.socket}</li>
 * </ul>
 */
package io.vanguard.testops.system.runtime.socket;
