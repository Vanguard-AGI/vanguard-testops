/**
 * Transport-facing plugin support contracts.
 *
 * <p>This package is reserved for plugin-related payloads or helper contracts
 * that cross technical boundaries such as Kafka listeners and service
 * notification entrypoints.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>plugin business orchestration services</li>
 *   <li>controller-facing request or response DTOs</li>
 *   <li>plugin loader implementation details</li>
 * </ul>
 *
 * <p>The goal is to keep transport payloads such as
 * {@code PluginNotifiedDTO} out of the generic {@code system.dto} package
 * while avoiding premature module splitting.</p>
 */
package io.vanguard.testops.system.support.plugin;
