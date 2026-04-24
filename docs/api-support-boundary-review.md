# API Support Boundary Review

## Goal

This document summarizes the current role of `io.metersphere.api.support`, the main boundary risks that still remain after the recent refactor work, and which follow-up actions are worth doing.

The intent is not to reopen large-scale package migration. The intent is to make later decisions cheaper and avoid `api.support` becoming a new catch-all area.

## Current Structure

Current main areas under `spotter-metersphere-service/src/main/java/io/metersphere/api/support`:

- `cache`
  - API task-running and prompt-template cache support
- `curl`
  - curl parsing support
  - includes parser entry, handler chain, constants, and curl request model
- `data`
  - API-specific serialization helpers
- `diff`
  - request and parameter diff support
- `format`
  - JSON / XML formatting helpers
- `mock`
  - mock server support helpers
- `parser`
  - parser contracts
  - import/export parser implementations
  - parser factories
  - step parser selection
  - JMeter / MeterSphere element conversion and parsing support
- `regex`
  - regex support for API-domain parsing and matching
- `scenario`
  - reusable API scenario support helpers
- `schema`
  - JSON schema support
- `mybatis`
  - API-specific MyBatis type handling

From current usage, `api.support` is mainly consumed by these service paths:

- execution path
  - [ApiExecuteService.java](/Users/jan/IdeaProjects/spotter-metersphere/spotter-metersphere-service/src/main/java/io/metersphere/api/service/ApiExecuteService.java)
- import path
  - [ApiDefinitionImportService.java](/Users/jan/IdeaProjects/spotter-metersphere/spotter-metersphere-service/src/main/java/io/metersphere/api/service/definition/ApiDefinitionImportService.java)
  - [ApiScenarioDataTransferService.java](/Users/jan/IdeaProjects/spotter-metersphere/spotter-metersphere-service/src/main/java/io/metersphere/api/service/ApiScenarioDataTransferService.java)
- export path
  - [ApiDefinitionExportService.java](/Users/jan/IdeaProjects/spotter-metersphere/spotter-metersphere-service/src/main/java/io/metersphere/api/service/definition/ApiDefinitionExportService.java)
- scenario step parsing path
  - [ApiScenarioService.java](/Users/jan/IdeaProjects/spotter-metersphere/spotter-metersphere-service/src/main/java/io/metersphere/api/service/scenario/ApiScenarioService.java)
  - [ApiScenarioRunService.java](/Users/jan/IdeaProjects/spotter-metersphere/spotter-metersphere-service/src/main/java/io/metersphere/api/service/scenario/ApiScenarioRunService.java)

## What Is Already Good

- `api.support` is no longer hiding old misplaced `adapter` dependencies.
- `curl` support is now more readable and no longer exposes a vague `util` entrypoint.
- parser entry factories are no longer mixed directly into the parser contract root.
- step parser selection is now separated from step parser implementations.
- data import and export parser naming is much more consistent than before.
- the legacy `api.parser` path has been removed, so the migration no longer has split history inside both old and new package trees.

## Remaining Boundary Risks

### 1. `api.support` still mixes multiple support styles

Today `api.support` contains:

- runtime cache support
- protocol parsing support
- import/export support
- execution serialization / conversion support
- request diff / formatting / schema support
- persistence support via `mybatis`

That is still acceptable, but it means `support` is currently a broad umbrella rather than a sharply defined subdomain.

The risk is not that the current code is wrong. The risk is that future additions will default into `api.support` without a clear rule.

### 2. `parser` is coherent locally, but broad conceptually

`api.support.parser` is much cleaner now, but it still covers several different concerns:

- import contracts
- export contracts
- test element parsing
- scenario step parsing
- JMeter conversion implementation
- HAR / Postman model parsing

These concerns are related, but they are not identical.

This is the strongest candidate for future deeper separation if and only if the API domain keeps growing.

### 3. `mybatis` is technically correct but isolated

`api.support.mybatis` is small and harmless right now. The main question is not whether it is misplaced, but whether it belongs under `api.support` long term or whether API persistence support should eventually sit closer to a broader infrastructure convention.

This is not urgent, but it is one of the few remaining places where `support` is being used as a generic bucket.

### 4. execution support is still implicit rather than explicit

The current code clearly shows parsing support, but there is no equally explicit package concept for "execution support" inside `api`.

That does not mean a new package must be created right now. It means the architecture currently makes parsing responsibilities easier to point at than execution responsibilities.

If the execution path grows further, this asymmetry will matter more.

## Recommended Boundary Interpretation

For the current codebase, the most practical interpretation is:

- `api.support`
  - reusable API-domain support that is not itself an API service
- `api.support.cache`
  - API-domain runtime cache support
- `api.support.curl`
  - curl-to-request parsing support
- `api.support.data`
  - API-specific serialization helpers
- `api.support.diff`
  - request diff support
- `api.support.format`
  - JSON / XML formatting helpers
- `api.support.mock`
  - API mock support
- `api.support.parser`
  - request/test/scenario conversion and import/export parsing support
- `api.support.regex`
  - API regex support
- `api.support.scenario`
  - reusable scenario support helpers
- `api.support.schema`
  - schema support
- `api.support.mybatis`
  - API-specific persistence helpers

This interpretation is good enough for now and does not require immediate code movement.

## Recommended Follow-up Actions

### Execute

1. Keep `api.support` as the main umbrella for now, but document what may enter it.

Suggested rule:

- allowed:
  - API-domain reusable parsing or conversion support
  - API-specific helper contracts used by multiple API services
  - API-specific persistence helpers that do not justify a bigger infrastructure move
- not allowed:
  - business orchestration logic
  - controller-facing request composition logic
  - long workflow services
  - generic helpers with no API-domain meaning

2. Treat `api.support.parser` as stable for now, not as a mandatory next split.

The recent naming and factory cleanup already improved this area a lot. There is no strong payoff in immediately splitting it further unless one of these happens:

- a new execution conversion path appears alongside JMeter conversion
- import/export implementations grow substantially
- multiple teams need to work in this area in parallel and start stepping on each other

3. Revisit `api.support.mybatis` only when there is a wider persistence-layer cleanup.

On its own, this area does not justify refactor work.

### Evaluate Later

1. Consider a future `api.support.execution` or similar area only if execution-related reusable code starts to grow faster than parser-related code.

This should be triggered by code growth, not by symmetry preference.

2. Consider splitting `parser` into deeper layers only when one of these becomes large enough:

- import/export support
- scenario step parsing
- test-element engine conversion

The threshold should be maintenance friction, not package aesthetics.

### Do Not Do Now

1. Do not start another broad package migration inside `api.support`.

The current structure is already significantly better than before, and the marginal value of more movement is now much lower.

2. Do not create a dedicated `event` layer for API yet.

There is not enough evidence of stable, shared, cross-boundary event contracts.

3. Do not split Maven modules based on the current `api.support` shape.

The package structure is cleaner now, but the contractual boundaries are not yet strong enough to justify module boundaries.

## Priority Judgment

Priority for future work related to `api.support`:

1. High
   - define and document what belongs in `api.support`
2. Medium
   - decide later whether execution support needs its own clearer package concept
3. Low
   - further split parser internals for stylistic reasons only

## Recommendation

The best next step is not more migration.

The best next step is to treat `api.support` as "good enough structurally, but still needing a documented boundary rule".

That means:

- stop broad package movement here
- preserve the current structure
- only reopen deeper changes if actual code growth creates pressure

## Decision Summary

- `api.support` should remain in place.
- `api.support.parser` should remain in place for now.
- the newer `cache/data/diff/format/mock/regex/scenario/schema` subpackages are
  consistent with the current umbrella interpretation and do not require
  another migration round.
- no immediate deeper split is recommended.
- the next meaningful improvement is boundary governance, not package motion.
