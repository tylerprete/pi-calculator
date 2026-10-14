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

CI in `.github/workflows/scala.yml` runs on pushes and pull requests targeting
`master`. It sets up JDK 21, builds, runs the browser smoke test, uploads
`public/`, and has a GitHub Pages deployment step publishing that directory.

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
