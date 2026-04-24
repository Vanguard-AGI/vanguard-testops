/**
 * API definition and scenario data import parsers.
 *
 * <p>This package contains reusable import implementations for external formats
 * such as HAR, JMeter, MeterSphere, Postman, and Swagger. These classes are
 * responsible for translating source payloads into API-domain import models.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>export logic</li>
 *   <li>service-layer orchestration</li>
 *   <li>controller request assembly</li>
 *   <li>execution-time runtime behavior</li>
 * </ul>
 */
package io.vanguard.testops.api.support.parser.dataimport;
