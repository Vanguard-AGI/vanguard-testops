/**
 * Runtime support for the system domain.
 *
 * <p>This package is intended for reusable runtime concerns that support the application
 * lifecycle and technical execution environment, such as scheduling, websocket runtime
 * support, serializer helpers, and runtime-oriented configuration.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>business service orchestration</li>
 *   <li>domain-specific workflow logic</li>
 *   <li>security authorization rules</li>
 *   <li>generic infrastructure code that belongs under broader system infrastructure areas</li>
 * </ul>
 *
 * <p>The goal is to keep {@code system.runtime} focused on operational runtime support rather
 * than letting it become a second general-purpose system bucket.</p>
 */
package io.vanguard.testops.system.runtime;
