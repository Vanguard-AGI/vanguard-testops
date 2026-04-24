/**
 * API-domain cache support.
 *
 * <p>This package is reserved for API-specific cache helpers that support API
 * services without containing business orchestration.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>API service orchestration</li>
 *   <li>controller-facing DTO composition</li>
 *   <li>generic cache helpers shared across unrelated domains</li>
 *   <li>parser implementations that belong under {@code api.support.parser}</li>
 * </ul>
 */
package io.vanguard.testops.api.support.cache;
