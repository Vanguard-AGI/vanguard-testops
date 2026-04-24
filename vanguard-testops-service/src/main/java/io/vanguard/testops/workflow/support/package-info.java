/**
 * Workflow-domain reusable support.
 *
 * <p>This package is reserved for workflow-specific technical support code that is shared by
 * multiple workflow services or runtime entrypoints but is not itself workflow business
 * orchestration.</p>
 *
 * <p>Examples that fit here:</p>
 * <ul>
 *   <li>workflow websocket runtime support</li>
 *   <li>workflow callback bridge helpers</li>
 *   <li>workflow-specific transport or notification support</li>
 * </ul>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>workflow service orchestration</li>
 *   <li>DTOs and persistence models</li>
 *   <li>general utilities with no workflow-domain meaning</li>
 * </ul>
 */
package io.vanguard.testops.workflow.support;
