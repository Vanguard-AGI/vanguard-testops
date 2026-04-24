/**
 * System-domain security support and enforcement components.
 *
 * <p>This package contains the security subsystem pieces that implement
 * authentication, authorization, ownership checks, and security-related request
 * interception for the application.</p>
 *
 * <p>Typical subpackage responsibilities:</p>
 * <ul>
 *   <li>{@code annotation}: security and ownership annotations</li>
 *   <li>{@code aspect}: aspect-based ownership and quota enforcement</li>
 *   <li>{@code config}: Shiro and permission-related configuration</li>
 *   <li>{@code filter}/{@code interceptor}/{@code realm}: request and subject
 *   enforcement components</li>
 * </ul>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>business service orchestration</li>
 *   <li>generic runtime configuration unrelated to security</li>
 *   <li>controller-facing DTO assembly</li>
 * </ul>
 */
package io.vanguard.testops.system.security;
