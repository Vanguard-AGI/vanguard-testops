/**
 * Functional-domain import and export support.
 *
 * <p>This package is reserved for reusable file-oriented support that helps
 * functional case services import from or export to external formats such as
 * Excel and XMind.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>functional business service orchestration</li>
 *   <li>controller-facing request composition</li>
 *   <li>generic file helpers with no functional-domain meaning</li>
 *   <li>persistence or workflow runtime logic</li>
 * </ul>
 *
 * <p>The goal is to keep {@code functional.support.importexport} focused on
 * reusable import/export support instead of becoming a broad document utility
 * bucket.</p>
 */
package io.vanguard.testops.functional.support.importexport;
