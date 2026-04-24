# Layer Restructure Merge Notes

## Merge Direction

Current branch:

- `refactor/layer-restructure`

Comparison baseline:

- `origin/master`

Current divergence at the time of writing:

- `origin/master` ahead: `0`
- `refactor/layer-restructure` ahead: `89`

This means the current branch is a clean one-way lead over `origin/master` and
does not currently contain master-side drift that must be merged first.

## Merge Recommendation

Recommended merge approach:

1. use a normal PR or merge request from
   `refactor/layer-restructure` into `master`
2. keep the existing commit history visible
3. do not squash if you want to preserve the phased cleanup trail

Why:

- this branch represents a long-running structural refactor
- the commit history is grouped by concern and useful for later archaeology
- the delivery summary and boundary-review documents already explain the phase
  outcome and stopping point

## Main Change Areas

High-level change groups included in this branch:

- removed the failed/unstable extra module experiment and restored truthful
  dependency direction
- reduced `adapter` to listener-style entrypoints
- regrouped `system.runtime`, `system.security`, `system.notice`,
  `workflow.support`, and `api.support`
- reorganized `functional.support.importexport`
- moved the highest-value internal transport contracts into clearer support
  packages
- added package-level boundary guidance and delivery documentation

## Key Contract Moves

Notable transport/contract cleanup completed in this branch:

- `io.metersphere.workflow.support.callback.WorkflowRunResultCallbackRequest`
- `io.metersphere.system.support.plugin.PluginNotifiedDTO`

Not intentionally extracted in this phase:

- API execution / report notification DTOs

Reason:

- they still serve mapper result models
- they are still used by notice-template reflection
- they still participate in downstream DTO inheritance

## Validation Completed

Validation already completed on this branch:

```bash
./mvnw -q -DskipTests clean compile
```

## Reviewer Guidance

The most useful files for reviewers to read first are:

- [docs/layer-restructure-delivery-summary.md](/Users/jan/IdeaProjects/spotter-metersphere/docs/layer-restructure-delivery-summary.md)
- [docs/layer-restructure-roadmap.md](/Users/jan/IdeaProjects/spotter-metersphere/docs/layer-restructure-roadmap.md)
- [docs/event-adapter-boundary-review.md](/Users/jan/IdeaProjects/spotter-metersphere/docs/event-adapter-boundary-review.md)
- [docs/api-support-boundary-review.md](/Users/jan/IdeaProjects/spotter-metersphere/docs/api-support-boundary-review.md)

## Merge Risk Judgment

Current risk profile:

- structural refactor risk: medium
- business-logic change risk: low to medium
- merge conflict risk against current `origin/master`: currently low, because
  `master` is not ahead

## After Merge

After merging to `master`, the next work should not reopen broad package
migration immediately.

Better next-step categories:

- targeted functional responsibility cleanup
- feature-level architectural hotspots
- future watchlist review only when growth pressure appears
