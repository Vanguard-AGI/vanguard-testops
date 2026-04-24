/**
 * Notice sending orchestration for system-level notification channels.
 *
 * <p>This package contains the sender abstraction, common sender base logic,
 * and sending aspects that coordinate delivery across different notice
 * channels.</p>
 *
 * <p>Typical subpackage responsibilities:</p>
 * <ul>
 *   <li>{@code sender.client}: external channel HTTP clients</li>
 *   <li>{@code sender.impl}: channel-specific sender implementations</li>
 * </ul>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>template rendering support that belongs under
 *   {@code system.notice.support.template}</li>
 *   <li>controller-facing request or response DTOs</li>
 *   <li>generic networking helpers with no notice-domain meaning</li>
 * </ul>
 */
package io.vanguard.testops.system.notice.sender;
