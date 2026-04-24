/**
 * Parser selection factories for the API domain.
 *
 * <p>This package is reserved for lightweight factories that choose reusable
 * parser implementations based on API-domain format or parser type.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>parser implementations themselves</li>
 *   <li>service-layer branching logic</li>
 *   <li>execution orchestration</li>
 *   <li>generic dependency wiring unrelated to parser selection</li>
 * </ul>
 */
package io.vanguard.testops.api.support.parser.factory;
