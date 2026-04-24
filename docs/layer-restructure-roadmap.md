# Layer Restructure Roadmap

## Current Status

This repository has completed the first stabilization phase of the layer refactor.

Mainline status:

- `refactor/layer-restructure` has been merged back into local and remote `master`
- current structural cleanup is already on the mainline, not waiting in a side branch

Completed:

- Removed empty modules:
  - `spotter-metersphere-component`
  - `spotter-metersphere-infrastructure`
  - `spotter-metersphere-manager`
- Restored misplaced code from removed modules back into `spotter-metersphere-service`
- Reduced `spotter-metersphere-adapter` to listener-oriented adapter code only
- Verified the repository with a real `clean compile`
- Ignored `.DS_Store` and removed it from version control
- Regrouped runtime support packages inside `service`
  - `system.schedule` -> `system.runtime.schedule`
  - `system.socket` -> `system.runtime.socket`
  - `system.serializer` -> `system.runtime.serializer`
  - moved runtime Spring configs into `system.runtime.config`
- Regrouped security Spring configs into `system.security.config`
- Continued narrowing `system.config`
  - moved scheduled/session runtime configs into `system.runtime.config`
  - moved permission config/cache into `system.security.config`
  - moved async/rest/i18n/request-trim configs into `system.runtime.config`
  - moved schedule helper support out of `system.utils` into
    `system.runtime.schedule.support`
  - moved object storage configs into `system.infrastructure.storage.config`
  - moved persistence/cache configs into `system.infrastructure.persistence.config` and `system.infrastructure.cache.config`
  - moved MyBatis interceptor config providers into `system.infrastructure.persistence.config.interceptor`
  - moved functional MyBatis manage interceptor config into
    `system.infrastructure.persistence.config.interceptor`
  - moved quota aspect into `system.security.aspect`
  - moved AI configs into `system.ai.config`
  - moved AI prompt template cache support out of `system.utils` into
    `system.ai.support.template`
  - moved MyBatis interceptor config model out of `system.utils` into
    `system.infrastructure.persistence.config.interceptor`
  - moved user Excel import listener support out of `system.utils` into
    `system.excel.listener`
  - moved EasyExcel export helper support out of `system.excel.utils` into
    `system.excel.support`
  - moved shared paging DTO and conversion support out of `system.utils` into
    `system.dto.page` and `system.support.page`
  - moved task runner HTTP client support out of `system.utils` into
    `system.support.taskrunner`
  - moved custom Jackson deserializer support out of `system.utils` into
    `system.support.jackson`
  - moved tree node parsing support out of `system.utils` into
    `system.support.tree`
  - moved relationship graph and rate calculation helpers out of
    `system.utils` into `system.support.graph` and `system.support.metrics`
  - moved batch processing helper support out of `system.utils` into
    `system.support.batch`
  - moved notice channel client support out of `system.notice.utils` into
    `system.notice.sender.client`
  - moved notice template translation support out of `system.notice.utils` into
    `system.notice.support.template`
  - moved metadata async runtime config out of `metadata.config` into
    `metadata.runtime.config`
- Regrouped API support packages inside `service`
  - `api.curl.*` -> `api.support.curl.*`
  - legacy curl handler code was regrouped under `api.support.curl.handler`
  - `api.parser.*` -> `api.support.parser.*`
  - moved export / har / postman parser support under `api.support.parser`
  - removed the now-empty legacy `api.parser` package path
- Regrouped functional import/export support
  - `functional.excel` -> `functional.support.importexport.excel`
  - `functional.xmind` -> `functional.support.importexport.xmind`
  - `functional.config` runtime configs -> `functional.runtime.config`
  - moved functional AI markdown parser support into `functional.support.ai.markdown`
  - `plan.config` runtime config -> `plan.runtime.config`
  - moved test plan xpack factory support into `plan.support.factory`
  - moved test plan report and grouping helpers out of `plan.utils` into
    `plan.support.report` and `plan.support.group`
  - moved bug export helper support into `bug.support.export`
  - moved project file, sort, and string helpers out of `project.utils` into
    `project.support.file`, `project.support.sort`, and `project.support.string`
- Continued second-phase package cleanup
  - moved workflow websocket entrypoint into `workflow.support.socket`
  - grouped `system.security` root classes into `annotation` / `aspect` / `filter` / `handler` / `interceptor`
  - grouped runtime configuration by concern:
    `system.runtime.schedule.config` for scheduling
    `system.runtime.socket.config` for websocket endpoint config
  - clarified curl parsing support under `api.support.curl.parser`
  - moved parser entry factories into `api.support.parser.factory`
  - moved scenario step parser selection into `api.support.parser.step.factory`
  - cleaned data-import parser naming, including postman import base parser
  - unified data-import parser naming around `*ApiDefinitionImportParser` and `*ApiScenarioImportParser`
  - aligned export implementations to `*ApiDefinitionExportParser`
  - documented `api.support`, `api.support.parser`, `api.support.curl`, and `api.support.mybatis`
    package boundaries with package-level guidance
  - introduced `api.support.cache` and moved API prompt template cache plus
    task-running cache out of `api.utils`
  - moved scenario integrated report step cache into `api.support.cache`
  - moved API schema, mock, request diff, and AI test case parser helpers out
    of `api.utils` into dedicated `api.support` subpackages
  - moved API import and scenario support helpers out of `api.utils` into
    `api.support.parser.dataimport` and `api.support.scenario`
  - moved API definition URL matching and ms converter helpers out of
    `api.utils` into `api.support.scenario` and `api.support.parser.ms`
  - moved API JSON/XML helpers and regex support out of `api.utils` into
    `api.support.format` and `api.support.regex`
  - moved API data serialization helper out of `api.utils` into
    `api.support.data`
  - moved API runtime configuration support out of `api.config` into
    `api.runtime.config`
  - normalized `api.support.parser.ms.http.controller` naming by removing legacy
    `contro` package residue
  - documented `functional.support.importexport`, `functional.support.importexport.excel`,
    and `functional.support.importexport.xmind` package boundaries
  - moved XMind import/export helper utilities out of
    `functional.support.importexport.xmind.utils` into
    `functional.support.importexport.xmind.support`
  - normalized workflow MyBatis XML placement:
    `spotter-metersphere-dao/src/main/resources/mapper/Workflow*.xml` was moved to
    `spotter-metersphere-dao/src/main/java/io/metersphere/workflow/mapper`
  - removed the temporary `mybatis.mapper-locations` runtime override after
    restoring the standard mapper XML package layout
  - normalized functional Excel import field naming by renaming
    `FunctionalCaseImportFiled` to `FunctionalCaseImportField`
  - narrowed `functional.config` to runtime-only concerns by moving remaining
    functional runtime configs into `functional.runtime.config`
  - moved workflow workspace cache support from `workflow.utils` to
    `workflow.support.cache`

Current module topology:

- `spotter-metersphere-web`
  - HTTP controller entrypoints only
- `spotter-metersphere-service`
  - Business services and currently all reusable runtime support code
- `spotter-metersphere-adapter`
  - Kafka / async listener style adapters
- `spotter-metersphere-dao`
  - Persistent model / generated mapper base
- `spotter-metersphere-common`
  - Shared common support
- `spotter-metersphere-plugin`
  - Plugin-related modules
- `start`
  - Application bootstrap assembly

## What Was Fixed

The main problem in the previous structure was not naming, but dependency direction.

Examples of corrected issues:

- `web` depended on classes that were only present in `adapter`
- `service` depended on parser, schedule, socket, security, excel, and job code that had been moved out
- removed modules had no stable responsibility but still influenced build structure

The repository is now back to a truthful structure:

- if `web` or `service` directly depends on a package, that package now lives in `service`
- `adapter` no longer acts as a hidden dependency bucket
- the build passes from a clean state

## Next Phase Goals

The next phase should avoid rebuilding empty modules too early.

Recommended order:

1. Refine `service` package boundaries internally
2. Define which concepts deserve an `event` module
3. Define which packages are true adapter entrypoints
4. Reintroduce new modules only after dependency contracts are stable

## Recommended Internal Refactor Targets

These areas in `spotter-metersphere-service` are now correct in dependency direction, but still deserve naming and boundary review:

- `io.metersphere.system.runtime.schedule`
- `io.metersphere.api.support`
- `io.metersphere.functional.support.importexport` (acceptance review mostly completed)
- `io.metersphere.system.runtime.schedule.config`
- `io.metersphere.system.security` (acceptance review mostly completed)
- `io.metersphere.system.runtime.config` (acceptance review mostly completed)
- `io.metersphere.system.runtime.socket` (acceptance review mostly completed)
- `io.metersphere.workflow.support` (acceptance review mostly completed)

Suggested internal grouping direction:

- `system.runtime`
  - schedule, socket, serializer, runtime support
- `system.security`
  - keep security annotations, aspects, realms, filters together
- `api.support`
  - curl, parser, handler, execution support
- `functional.support.importexport`
  - excel + xmind related file import/export support

This does not require new Maven modules first; it can start as package-level cleanup.

## Remaining Work After Mainline Merge

The remaining work is no longer a broad package migration. It is now a controlled
governance and feature-backlog follow-up phase.

Still worth doing:

1. Continue package-boundary governance
   - keep adding or refining `package-info.java` only where a root support area
     still lacks a clear rule
   - prevent `support` packages from becoming a new generic utility bucket
2. Review root package naming consistency
   - check for newly added classes that bypass the cleaned `runtime` / `support`
     / `config` grouping conventions
3. Track true functional TODOs separately from structure work
   - workflow executor placeholder implementations
     current judgment: the active runtime already executes through
     `WorkflowRunService -> remote executor -> callback`; `WorkflowExecutorImpl`
     and `StepExecutorFactory` should currently be treated as an explicit local
     skeleton, not as a half-finished production path
   - workflow workspace member-table related TODOs
     current judgment: this is blocked by missing domain model rather than a
     missing query; the codebase has project-level membership via
     `user_role_relation`, but no workspace-level membership relation yet
   - functional and API delete-cascade TODOs
     current progress: API case deletion now clears direct `api_test_case_record`
     and `test_plan_api_case` relations, and project cleanup now uses the
     correct API definition blob table plus the correct scenario-report recount
     loop; remaining follow-up should only target relations that can be proven
     from the domain model
   - notice / report logging and retry follow-ups
     current progress: API notice, API report, and API scenario report flows now
     handle environment-group lookup and missing user/environment/report data
     more defensively; remaining work is mostly consistency review rather than
     boundary migration
4. Revisit event-boundary extraction only when a contract is truly stable
   - do not reopen another module split just for symmetry

Not recommended now:

- recreating `component`, `infrastructure`, or `manager` as empty modules
- splitting `api.support` again without real maintenance pressure
- introducing a dedicated `event` Maven module before contracts stabilize

Current judgment:

- the migration itself is complete
- the remaining work is mostly governance plus domain-feature TODO cleanup
- future changes should be smaller, localized follow-up tasks rather than
  another repo-wide movement

## Event Layer Criteria

Do not create an `event` module until at least one of the following is true:

- a model is passed across process boundaries
- multiple modules consume the same event contract
- the event payload no longer depends on service-internal DTOs

Likely future candidates:

- workflow run result callbacks
- plugin notified messages
- API execution notification payloads

Progress update:

- workflow result callback payload has been moved to
  `io.metersphere.workflow.support.callback`
- plugin notified payload has been moved to
  `io.metersphere.system.support.plugin`
- API execution notification payloads have been reviewed and are intentionally
  left in place for now because they still serve mapper result models,
  notice-template reflection, and downstream DTO inheritance paths

Non-candidates for a future `event` module:

- controller-only request DTOs
- MyBatis mappers
- Spring config classes
- parser implementations tightly coupled to business services

## Mapper XML Convention

MyBatis XML files should live beside their mapper interfaces under
`src/main/java/io/metersphere/**/mapper`.

Current repository rule:

- use package-co-located mapper XML as the default convention
- do not add new business mapper XML files under `src/main/resources/mapper`
- if a mapper needs XML, keep the interface and XML in the same package so the
  default MyBatis scan path continues to work without extra runtime overrides

## Adapter Layer Criteria

Packages should remain in `adapter` only if they primarily act as external entrypoints.

Good adapter examples in the current codebase:

- Kafka listeners
- asynchronous consumers
- integration callbacks that translate external messages into service calls

Packages should not stay in `adapter` when they are directly imported by `web` or `service`.

## Rebuild Conditions For New Modules

Only rebuild a dedicated module when all conditions are met:

- the package cluster has a stable responsibility
- downstream dependencies point one way
- public contracts are explicit
- a `clean compile` still works without relying on historical output

## Current Completion Judgment

The current layer-restructure phase can stop here.

Reason:

- dependency direction has been repaired
- adapter entrypoints are thin and stable
- the highest-value internal transport contracts have been clarified
- the main support/runtime/security/notice/workflow boundary areas now have
  package-level guidance
- the remaining open items are judgment calls for future growth, not blocking
  structural defects

## Immediate Next Steps

Short term:

1. Keep `adapter` limited to listener / consumer / callback adapters
2. Avoid reopening broad package migration without new structural pressure
3. Extract event contracts only after identifying genuinely stable message payloads

In progress now:

1. second-phase package cleanup is functionally completed
2. acceptance review is functionally completed for the current refactor phase
3. remaining work is now a future watchlist rather than an active migration plan

Acceptance notes:

- empty legacy package directories created during refactors have been removed
- the remaining `system.utils` classes are intentionally limited to a small set of
  high-coupling or low-payoff helpers rather than being broad migration leftovers
- `uid.utils` is currently treated as internal UID subsystem infrastructure, not as
  a general shared utility bucket that needs further package splitting
- current adapter transport contracts have been reviewed:
  internal service-owned payloads have been moved to clearer support packages,
  while external SDK-owned contracts remain in `sdk` by design
- API execution / report notification payloads are intentionally not being
  extracted right now because their compatibility surface is still broader than
  the workflow and plugin transport contracts
- `system.security`, `system.runtime.config`, `system.runtime.socket`,
  `workflow.support`, `system.notice`, and
  `functional.support.importexport` now have package-level boundary guidance in
  place; remaining review is mostly consistency confirmation rather than
  structural migration
- the current phase has been revalidated with a full
  `./mvnw -q -DskipTests clean compile`

Medium term:

1. Reassess whether an `event` module is justified
2. Review whether `api.support` needs a finer split between parser support and execution support
3. Decide whether any runtime areas still need deeper split beyond the current package cleanup

These are future observation items, not unfinished work for the current phase.

## Verification Rule

For every future refactor step, use:

```bash
./mvnw -q -DskipTests clean compile
```

Do not treat a plain incremental compile as sufficient verification for layer work.
