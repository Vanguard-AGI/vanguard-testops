/**
 * System-domain reusable technical support.
 *
 * <p>This package is reserved for system-level support code that is shared by
 * multiple system services or runtime entrypoints but is not itself business
 * orchestration. Typical contents here include bounded support for paging,
 * batch processing, tree parsing, task-runner integration, and transport-facing
 * plugin support.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>system service orchestration</li>
 *   <li>controller-facing request or response DTOs</li>
 *   <li>generic helpers with no explicit system-domain meaning</li>
 * </ul>
 *
 * <p>The goal is to keep {@code system.support} as a bounded system support area
 * rather than allowing it to turn back into a catch-all utility bucket.</p>
 */
package io.vanguard.testops.system.support;
