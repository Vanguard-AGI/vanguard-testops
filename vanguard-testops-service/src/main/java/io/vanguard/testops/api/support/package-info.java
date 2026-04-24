/**
 * API-domain reusable support.
 *
 * <p>This package is reserved for support code that is shared by multiple API services
 * but is not itself business orchestration logic. Typical contents here include parsing,
 * conversion, and API-specific technical helpers.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>service-layer orchestration</li>
 *   <li>controller-facing request composition</li>
 *   <li>generic helpers with no API-domain meaning</li>
 * </ul>
 *
 * <p>The main goal is to keep {@code api.support} as a bounded API support area rather than
 * letting it become a new catch-all bucket.</p>
 */
package io.vanguard.testops.api.support;
