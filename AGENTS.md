# Working on pi-calculator

## App and source layout

This is a small static browser app written in Scala and compiled to JavaScript
with Scala.js. It estimates pi by sampling points in a square and counting those
inside its inscribed circle. There is no backend or frontend bundler.

- `build.mill`: the `pi` Scala.js module, compiler versions, and dependencies.
- `.mill-version`: pinned Mill version; `millw` is the checked-in launcher.
- `pi/src/pi/PiCalculator.scala`: browser initialization, canvas rendering,
  button handlers, and the `AutoIncrementer` timer.
- `pi/src/pi/PiState.scala`: immutable sample counts and pi estimation.
- `pi/src/pi/Point.scala`: point generation and distance calculations.
- `pi/src/pi/Color.scala`: color parsing and serialization.
- `public/index.html`: canvas, controls, count displays, and bundle loading.
- `public/css/main.css`: stylesheet (currently empty).
- `tests/browser-smoke.html` and `test.sh`: browser integration checks.
- `.github/workflows/scala.yml`: read-only CI build, browser smoke test,
  deployment regression tests, and `public` artifact upload.
- `.github/workflows/pages.yml`: trusted production/PR publishing and cleanup.
- `.github/scripts/Pages.scala`: current-build selection, artifact validation,
  and assembly of the complete Pages tree.
- `tests/PagesTests.scala`: deployment selection and production/preview isolation
  regression checks using Mill's Scala script support and uTest.

## Building

Run commands from the repository root. The pinned versions are Scala 3.10.0,
Scala.js 1.22.0, and Mill 1.3.0. The DOM dependency is `scalajs-dom` 2.8.1.
Use JDK 21 or newer as documented in the README. Mill can download its own JDK
and dependencies, so the first build needs network access.

```bash
./build.sh
```

This runs `./millw pi.fullLinkJS` and copies the optimized output from
`out/pi/fullLinkJS.dest/main.js` to `public/js/app.js`. The script fails immediately
if linking fails. `out/` is ignored build output. Treat `public/js/app.js` as
generated code: rebuild it instead of editing it.

For compile and linker checks without copying the public bundle:

```bash
./millw --no-server pi.compile pi.fastLinkJS pi.fullLinkJS
```

`fastLinkJS` produces development output; it does not update the bundle loaded
by `public/index.html`. Use `./build.sh` before browser testing. Mill normally
uses a local build server; `--no-server` is useful when that server is unavailable.
In a restricted execution environment, downloads or local sockets may require
the environment's normal permission escalation.

## Operating the app

After building, open `public/index.html` in a browser. No web server is required.
The deployed site is https://tylerprete.github.io/pi-calculator/.

- **Next** samples the selected number of points (1, 10, 100, or 1000).
- **Auto Increment** starts a timer running at 20 ms intervals.
- **Stop** clears that timer.
- **Reset** stops the timer, clears the counts, and restores the canvas.

The app needs an actual document containing its canvas and controls. Running
the Scala.js entry point in plain Node does not exercise the UI. A JSDOM
environment alone also does not supply the real canvas rendering used here.

## Testing and verification

For changes to Scala code, build configuration, HTML controls, or rendering:

```bash
./build.sh
bash ./test.sh
```

The smoke test launches headless Chrome with a temporary browser profile and
loads the app in an iframe. It checks all step sizes, count synchronization,
canvas changes, convergence toward pi after roughly 100,000 samples, automatic
incrementing, stopping, reset, and continued operation after reset. It exits
nonzero on failure and prints the captured page and Chrome diagnostics.

On macOS it uses `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`.
On Linux it uses `google-chrome`. Override the executable when needed:

```bash
CHROME_BIN=/path/to/chromium bash ./test.sh
```

`test.sh` does not build the app. Testing an old `public/js/app.js` can give a
misleading pass. There is currently no Scala unit-test module; the untracked
`package.json` observed during setup has a placeholder failing `npm test`
command and is not part of the build workflow.

For visual changes, also inspect the page in a browser. Check the canvas,
controls, and displayed estimates. Use `git diff --check` before finishing.
For shell changes, check syntax with `bash -n build.sh test.sh` or `sh -n millw`.

For changes to Pages publishing or preview behavior, run:

```bash
./millw --no-server tests/PagesTests.scala
```

These checks cover production/preview isolation, stale asset removal, cleanup,
current-build selection, fork PR lookup, and rejection of unsafe artifacts.
Workflow-only changes do not require rebuilding the Scala app unless they also
change its build, controls, or rendering. Validate workflow YAML with
`actionlint` when available. Actionlint 1.7.12 does not recognize GitHub's
documented `concurrency.queue` property; verify `queue: max` against GitHub's
current documentation and lint a temporary copy without that key if needed.

## GitHub Actions and Pages

`Scala CI` in `.github/workflows/scala.yml` runs on pushes to `master` and pull
requests targeting `master`. It uses a read-only token, sets up Liberica JDK 21,
builds, runs the browser smoke test and Pages regression tests, and uploads
`public/` as the `public` artifact. An empty artifact upload fails the build.

`Publish Pages` in `.github/workflows/pages.yml` handles successful `Scala CI`
completions through `workflow_run`, and PR closure through `pull_request_target`.
It checks out trusted `master` code and the existing `gh-pages` tree, never PR
code. Its token has `contents: write`, `actions: read`, and `pull-requests: read`.
The existing `gh-pages` branch is required, and GitHub Pages must publish from
that branch's root.

- Production updates replace root assets while preserving `previews/`.
- PR builds replace only `previews/pr-N/`. The URL is
  `https://tylerprete.github.io/pi-calculator/previews/pr-N/`.
- Closing or merging a PR removes only its preview. Reopened PRs are not
  cleaned up by a queued closure event.
- Publishing skips outdated production commits, outdated PR heads, and closed
  PRs. Fork runs with missing PR metadata are matched against current open PRs.
- All publishing and cleanup share the `github-pages` concurrency group with
  `queue: max` and `cancel-in-progress: false`.
- Preview links appear in the build and publishing workflow summaries. The
  link becomes live after GitHub Pages finishes publishing. Fork CI may first
  need a maintainer's approval.

The publisher must exist on `master` before these triggers activate. Treat PR
artifacts as static data: never execute or import their contents in a workflow
with write permissions. Preserve validation against symlinks, hidden control
files, missing `index.html`, reserved `previews/`, and invalid PR destinations.
Keep asset references relative so the app works inside preview subdirectories.

Action releases currently used are `actions/checkout@v7.0.1`,
`actions/setup-java@v6.0.1`, `actions/upload-artifact@v7.0.2`,
`actions/download-artifact@v8.0.2`, and `peaceiris/actions-gh-pages@v4.1.0`.
When updating them, verify current stable releases and compatible inputs and
runtimes, then validate both workflows and run the deployment regression tests.

## Writing and changing the app

Follow the existing Scala style: two-space indentation, braces, small case
classes, and straightforward methods. Keep numerical state and geometry in
`PiState` and `Point`; keep DOM and canvas work in `PiCalculator`.

Preserve the coordinate model: random points span `[0, 2)` on both axes and
the circle is centered at `(1, 1)` with radius 1. The estimate is
`4 * pointsInCircle / totalPoints.toDouble`. Randomized checks should use a
reasonable tolerance rather than require an exact estimate.

Keep HTML IDs and radio values consistent with Scala event wiring and smoke
tests. Preserve automatic startup when the bundle loads. Avoid duplicate
timers; resetting must stop automatic sampling. If behavior intentionally
changes, update the corresponding smoke assertions and documentation.

When upgrading tooling, update the Scala and Scala.js versions in `build.mill`,
the Mill pin in `.mill-version`, and the fallback version in `millw`. Verify
both linker modes and run the production browser smoke test.

Inspect `git status` and existing diffs before editing or staging. At the time
this guide was written, there were pre-existing, uncommitted JSDOM dependency
and `jsEnv` additions in `build.mill`, plus untracked `package.json` and
`public/js/app.js`. These are local state, not requirements to add JSDOM or npm
to the project. Preserve unrelated user edits and stage only your own changes
unless asked otherwise.
