/**
 * Shared support utilities for the notice subsystem.
 *
 * <p>This package is reserved for reusable notice-domain support code that is
 * neither a core notice model nor a concrete sender implementation. It acts as
 * the home for helper capabilities such as template processing.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>channel transport clients and sender implementations</li>
 *   <li>business orchestration services outside the notice subsystem</li>
 *   <li>generic helpers with no notice-domain meaning</li>
 * </ul>
 */
package io.vanguard.testops.system.notice.support;
