/**
 * API-specific MyBatis persistence helpers.
 *
 * <p>This package is reserved for small persistence support pieces that are
 * specific to the API domain, such as type handlers or mapping helpers that are
 * shared by API persistence paths.</p>
 *
 * <p>Keep out of this package:</p>
 * <ul>
 *   <li>business service orchestration</li>
 *   <li>generic persistence infrastructure shared across unrelated domains</li>
 *   <li>controller-facing DTO assembly</li>
 *   <li>parsing or execution support that belongs under {@code api.support.parser}</li>
 * </ul>
 *
 * <p>The goal is to keep {@code api.support.mybatis} as a narrow API persistence
 * support area until a wider persistence-layer cleanup justifies a broader move.</p>
 */
package io.vanguard.testops.api.support.mybatis;
