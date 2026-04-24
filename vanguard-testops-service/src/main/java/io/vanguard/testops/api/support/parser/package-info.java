/**
 * Parsing and conversion support for the API domain.
 *
 * <p>This package currently groups:</p>
 * <ul>
 *   <li>import/export parser contracts</li>
 *   <li>parser selection factories</li>
 *   <li>scenario step parsing support</li>
 *   <li>JMeter and MeterSphere test-element conversion support</li>
 * </ul>
 *
 * <p>This package is intentionally broader than a single parser implementation area, but it
 * should remain focused on reusable parsing/conversion responsibilities. New execution
 * orchestration logic should not be added here by default.</p>
 */
package io.vanguard.testops.api.support.parser;
