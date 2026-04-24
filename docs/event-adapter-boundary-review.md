# Event And Adapter Boundary Review

## Goal

This document records the current adapter entrypoints, the message-shaped DTOs
already present in the codebase, and which areas are realistic candidates for a
future `event` boundary.

The goal is not to split modules immediately. The goal is to reduce guesswork
when the repository is ready for a stronger contract boundary.

## Current Adapter Entrypoints

Current code under `spotter-metersphere-adapter` is now focused on listener-style
entrypoints:

- `io.metersphere.workflow.listener.WorkflowRunResultKafkaListener`
  - consumes workflow run result messages from Kafka
  - hands off to `WorkflowRunService.applyWorkflowRunResult`
- `io.metersphere.system.listener.PluginListener`
  - consumes plugin add/delete notifications
  - hands off to `PluginLoadService`
- `io.metersphere.api.listener.MessageListener`
  - consumes API report task notifications
  - coordinates notice sending and queue continuation logic
- `io.metersphere.api.listener.DebugListener`
  - consumes debug execution messages
  - bridges results into websocket delivery and environment parsing

This means the adapter module is already behaving as an external transport
entrypoint layer instead of a hidden dependency bucket.

## Existing Message Candidates

The codebase already contains several DTOs that behave like transport payloads
or message contracts:

- `io.metersphere.workflow.support.callback.WorkflowRunResultCallbackRequest`
- `io.metersphere.system.support.plugin.PluginNotifiedDTO`
- `io.metersphere.system.dto.sdk.ApiReportMessageDTO`
- `io.metersphere.system.dto.sdk.ApiScenarioMessageDTO`
- `io.metersphere.system.dto.sdk.FunctionalCaseMessageDTO`
- `io.metersphere.system.dto.sdk.TestPlanMessageDTO`
- `io.metersphere.system.dto.BugMessageDTO`

These types are not yet organized as a dedicated event contract layer, but they
already provide evidence of message-shaped boundaries in the current design.

## Strongest Event Candidates

### 1. Workflow Run Result Callback

Why it stands out:

- it already crosses a Kafka transport boundary
- it enters through a dedicated adapter listener
- it is consumed by a focused service method:
  `WorkflowRunService.applyWorkflowRunResult`

Risk if left implicit:

- callback payload shape can drift together with service-internal workflow code
- later consumers would have no clearly owned contract package

Judgment:

- strongest current candidate for a future explicit event contract
- still does not require a new module today

### 2. Plugin Notification Payload

Why it stands out:

- it already represents a transport notification rather than a controller DTO
- adapter and service responsibilities are cleanly separated

Current limitation:

- the payload is still tightly coupled to plugin loading behavior
- there is only one main consumer path today

Judgment:

- good candidate for a future event-style contract package
- not strong enough yet to justify a standalone module

### 3. API Execution / Report Notice Payloads

Why they matter:

- API execution already has asynchronous queue and notification behavior
- message-style DTOs already exist in `system.dto.sdk`

Current limitation:

- these DTOs are still mixed with broader service-facing SDK DTOs
- execution orchestration is still closely tied to API service internals
- several of them are also reused by mapper `resultType`, notice-template field
  introspection, and downstream business DTO inheritance

Judgment:

- worth revisiting later if API execution keeps growing
- not recommended for immediate extraction
- moving them now would create a larger compatibility surface than the workflow
  or plugin payload moves

Current acceptance judgment:

- these payloads are intentionally staying in their current packages for now
- the current codebase would pay more compatibility cost than architectural
  benefit from extracting them during this refactor phase

## What Should Stay Out Of An Event Layer

These should remain outside a future event boundary unless their role changes:

- controller request DTOs
- MyBatis mapper models
- Spring config classes
- parser implementation DTOs tied directly to service internals
- helper DTOs that exist only to support notice-template rendering
- external SDK contracts such as `io.metersphere.sdk.dto.api.notice.ApiNoticeDTO`
  and `io.metersphere.sdk.dto.SocketMsgDTO`

Acceptance note:

- current adapter listeners now fall into two clear groups:
  internal service-owned contracts
  (`WorkflowRunResultCallbackRequest`, `PluginNotifiedDTO`)
  and external SDK-owned contracts
  (`ApiNoticeDTO`, `SocketMsgDTO`)
- this means the current internal transport payload cleanup is effectively
  complete without introducing a standalone `event` module
- API report / notice message DTOs remain a documented future candidate, but
  they are not part of the current extraction set

## Adapter Boundary Assessment

The adapter layer is currently in a healthy state:

- adapter code is transport-entry oriented
- service continues to own business orchestration
- listener classes are thin and easy to reason about

This is a good sign. The next adapter-related improvement should be contract
clarity, not more package motion.

## Recommended Next Step

Do not create a new Maven `event` module yet.

Instead:

1. keep adapter listeners thin
2. preserve message DTO ownership intentionally
3. when a payload gains multiple consumers or transport independence, move it
   first into a clearer contract package inside `service`
4. only consider a standalone event module after those contracts stabilize

Current implementation note:

- the workflow result callback payload has been moved into
  `io.metersphere.workflow.support.callback`
  as a clearer transport-facing contract package inside `service`
  without introducing a new Maven module
- the plugin notification payload has been moved into
  `io.metersphere.system.support.plugin`
  so the Kafka message contract no longer sits in the generic `system.dto`
  package

## Decision Summary

- `adapter` is now structurally credible as an entrypoint layer
- workflow result payloads are the clearest future event-boundary candidate
- plugin notifications are the second-best candidate
- API execution notice payloads should be revisited later
- external SDK transport contracts should stay owned by `sdk`, not be copied
  into `service`
- the current refactor phase intentionally stops before extracting the
  higher-compatibility-surface API notice DTO set
- the correct action now is documentation and contract awareness, not module
  splitting
