/**
 * Workflow websocket runtime support.
 *
 * <p>This package contains workflow-specific websocket entry and delivery
 * support that is shared by workflow runtime flows. It stays under
 * {@code workflow.support} because it is directly invoked by workflow services
 * and acts as workflow-domain runtime support rather than a separate external
 * adapter module boundary.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>workflow business orchestration</li>
 *   <li>controller-layer HTTP request handling</li>
 *   <li>generic websocket helpers with no workflow-domain meaning</li>
 * </ul>
 */
package io.vanguard.testops.workflow.support.socket;
