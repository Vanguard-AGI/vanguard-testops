# Layer Restructure Delivery Summary

## Delivery Conclusion

This phase is now complete as a delivered architecture-governance milestone,
not just as a branch-level refactor experiment.

Current final judgment:

- the layer restructure itself is complete
- the post-merge follow-up governance work has also been carried through on
  `master`
- the remaining items are future evolution choices, not blocked architecture
  defects

## Scope

This document summarizes the current delivery state of the
`refactor/layer-restructure` branch so it can be reviewed, merged, or handed
off without reconstructing the refactor history from commit logs.

## What This Phase Changed

### 1. Restored truthful module boundaries

- removed the unstable empty-module experiment around
  `component`, `infrastructure`, and `manager`
- restored directly depended-on code back into `spotter-metersphere-service`
- reduced `spotter-metersphere-adapter` to listener-style transport entrypoints
- kept `spotter-metersphere-web` as HTTP controller entrypoints

### 2. Reorganized service-internal support areas

Main package cleanup themes:

- `system.runtime`
  - scheduling, websocket runtime support, runtime config, serializer support
- `system.security`
  - annotations, aspects, filters, handlers, realms, and security config
- `system.notice`
  - sender contracts, sender clients, template support
- `workflow.support`
  - websocket support, workspace cache support, callback contract support
- `api.support`
  - curl, parser, cache, data, diff, format, mock, regex, scenario, schema
- `functional.support.importexport`
  - Excel and XMind import/export support

### 3. Clarified internal transport-facing contracts

Explicitly moved:

- `WorkflowRunResultCallbackRequest`
  - from workflow DTO space into
    `io.metersphere.workflow.support.callback`
- `PluginNotifiedDTO`
  - from generic system DTO space into
    `io.metersphere.system.support.plugin`

These moves were chosen because they already behaved like transport-facing
message contracts and had a limited enough compatibility surface to move
without creating unnecessary risk.

## Post-Merge Governance Follow-Up

After the mainline merge, this phase also completed a focused cleanup pass on
the most error-prone structural leftovers.

### 1. Workflow runtime boundary was made explicit

- clarified that the production execution path is
  `WorkflowRunService -> remote executor -> callback`
- changed `WorkflowExecutorImpl` and `StepExecutorFactory` from misleading
  partial implementations into explicit local skeletons
- completed workflow cancel follow-up so cancellation now updates run state,
  report state, and WebSocket notifications consistently
- documented that workflow workspace member counting is blocked by missing
  domain relations rather than a missing query

### 2. API notice / report stability was hardened

- API notice flow now resolves both environment IDs and environment-group IDs
- API notice flow now handles missing user / report / API definition data more
  defensively instead of failing through null dereferences
- API report and scenario report views now use the same defensive resolution
  rules for environment names and creator names
- task-report environment fields were corrected so `environmentId` remains the
  real ID and `environmentName` carries the display value

### 3. API delete-cascade truthfulness was improved

- API case deletion now removes direct `api_test_case_record` relations
- API case deletion now also removes direct `test_plan_api_case` relations
- project-level cleanup now deletes API definition blobs from the correct blob
  table
- project-level cleanup now recounts scenario reports through the correct
  scenario-report counter instead of the API-report counter

### 4. Swagger3 import edge cases were corrected

- Basic authentication now uses standard Base64 credentials encoding
- header-token parsing now supports values containing additional `:` characters
- stale TODO comments around already-implemented parsing behavior were removed

## Final Architecture Conclusion

The repository now has a stable practical structure:

- `spotter-metersphere-web`
  - HTTP/controller entrypoints
- `spotter-metersphere-adapter`
  - listener-style / transport adapter entrypoints
- `spotter-metersphere-service`
  - business services plus currently shared runtime/support code
- `spotter-metersphere-dao`
  - persistence model and generated mapper base
- `spotter-metersphere-common`
  - shared common contracts and support

Within `service`, the architecture is now best understood as:

- `runtime`
  - operational runtime support such as schedule, socket, serializer, config
- `security`
  - authorization/security concerns and enforcement wiring
- `support`
  - bounded helper/support packages, no longer a generic dumping ground
- domain service packages
  - API / workflow / functional / plan / project specific business services

This is no longer in a repo-wide migration state. Future work should be
localized and domain-driven.

## What Was Intentionally Not Extracted

### API execution / report notification DTOs

These remain in place for now.

Reason:

- they are still reused by mapper result models
- they are used by notice-template reflection/introspection
- they also participate in downstream DTO inheritance paths

Judgment:

- still a future event-boundary candidate
- not worth extracting during the current refactor phase

### No standalone `event` module

This phase intentionally stops before introducing a dedicated event module.

Reason:

- transport contracts are not yet uniformly stable
- only a subset of contracts are clean enough to extract
- package-level clarity provides most of the current value without module churn

## Package-Level Guidance Added

This phase added package-level guidance across the main reorganized areas,
including:

- `system.runtime`
- `system.security`
- `system.notice`
- `system.notice.sender`
- `workflow.support`
- `workflow.support.socket`
- `workflow.support.callback`
- `system.support.plugin`
- `api.support`
- `functional.support.importexport`
- key Excel/XMind import/export subpackages

The purpose of these `package-info.java` files is to prevent the cleaned
package layout from turning back into another generic bucket over time.

## Validation

This phase was validated with:

```bash
export JAVA_HOME=/Users/jan/Library/Java/JavaVirtualMachines/ms-21.0.8/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -pl spotter-metersphere-service -am -DskipTests compile
```

Validation result:

- build passed successfully on JDK 21
- the earlier Java 8 failure was an environment-selection issue, not a code
  regression in this delivery
- warnings remain for pre-existing deprecated / unchecked usage, but they are
  not blockers for this phase

## Current Merge Judgment

This refactor phase is mergeable as a completed structural cleanup phase.

Reason:

- dependency direction has been repaired
- adapter boundaries are now structurally credible
- highest-value internal transport contracts were clarified
- workflow runtime boundaries now match the real execution model
- API notice/report/delete flows received a real follow-up governance pass
- key support/runtime/security/notice/workflow areas now have boundary guidance
- remaining open questions are future watch items, not blocking structural
  defects

## Future Watchlist

These are not unfinished tasks for the current phase. They are future
observation items:

- whether `api.support` eventually deserves a finer split between parser and
  execution support
- whether API report/notice DTOs become stable enough for contract extraction
- whether a dedicated `event` or `contract` module becomes justified later
- whether workflow workspace should eventually gain an independent member model
  after the domain relation is intentionally designed
