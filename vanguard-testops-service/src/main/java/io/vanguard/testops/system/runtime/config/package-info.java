/**
 * Runtime-oriented Spring configuration for the system domain.
 *
 * <p>This package contains technical configuration that supports the
 * application's runtime behavior, such as async execution, i18n, request
 * normalization, HTTP client setup, and session-related runtime wiring.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>security-specific configuration that belongs under
 *   {@code system.security.config}</li>
 *   <li>business service orchestration</li>
 *   <li>domain-specific runtime helpers that deserve their own support package</li>
 * </ul>
 */
package io.vanguard.testops.system.runtime.config;
