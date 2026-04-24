/**
 * Curl-to-request parsing support for the API domain.
 *
 * <p>This package is responsible for translating curl text into API request
 * structures through parser entrypoints, handler chains, parsing constants, and
 * curl-specific request models.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>API business orchestration</li>
 *   <li>controller request composition</li>
 *   <li>generic HTTP helpers without curl parsing meaning</li>
 *   <li>execution flow concerns unrelated to curl conversion</li>
 * </ul>
 *
 * <p>The goal is to keep {@code api.support.curl} focused on curl parsing support
 * rather than letting it become a generic HTTP utility area.</p>
 */
package io.vanguard.testops.api.support.curl;
