# re-route

[![Clojars Project](https://img.shields.io/clojars/v/com.yetanalytics/re-route.svg)](https://clojars.org/com.yetanalytics/re-route)

Routing wrapper library for SPAs based on [re-frame](https://github.com/day8/re-frame) and [reitit](https://github.com/metosin/reitit).

In addition to standard navigation functionality, this library comes with built-in functionality for navigation prevention, e.g. to warn the user of unsaved input changes.

## Installation

To use re-route as a dependency in your ClojureScript project, add the following to your `deps.edn` file:

```clojure
{ com.yetanalytics/re-route {:mvn/version "0.1.0"} }
```

For information on the latest version, as well as the changelog for previous versions, see the [Releases](https://github.com/yetanalytics/re-route/releases) page.

**NOTE:** re-route assumes that your browser supports the [History API](https://developer.mozilla.org/en-US/docs/Web/API/History_API).

## API

All API functions are located in the `com.yetanalytics.re-route` namespace, which we will alias to `re-route`.

### Effect Handlers

#### `[::re-route/init routes default-route prevent-nav-opts]`

Starts a reitit router and initializes re-route's values in the re-frame app-db.

`routes` is a reitit-compatible vector of route maps; a standard route map looks something like:
```clojure
{:name        :route-name
 :view        (fn [args] ...)
 :parameters  { ... }
 :controllers [{:start (fn [params] ...)
                :stop  (fn [params] ...)}]}
```
where `:controllers` contains the `:start` and `:stop` functions that are called when entering and leaving the route, respectively. `:view` is a property unique to re-route that specifies a Reagent render function.

`default-route` is the `:name` of the fallback route to be served instead of a route that is not found in the router. This is usually for "Not Found" pages.

`prevent-nav-opts` is an options map for navigation prevention:
- `:enabled?` determines if nav prevention is enabled or not. If `false`, then the `::re-route/set-prevent-nav` effect handler has no effect. Default `true`.
- `:back-button?` determines whether nav prevention is enabled for browser back button clicks (or other changes to the browser history stack). Default `false`, as enabling this causes weird effects with the browser history, due to limitations of the History API.
- `:default-text` sets the default text for confirm dialogs. By default this is "You have unsaved changes, are you sure?"

#### `[::re-route/navigate route-name path-params query-params fragment]`

Navigate to a new route with these parameters, and calls [`.pushState`](https://developer.mozilla.org/en-US/docs/Web/API/History/pushState) on the history stack.

#### `[::re-route/navigate-replace route-name path-params query-params fragment]`

Same as `::re-route/navigate`, but calls [`.replaceState`](https://developer.mozilla.org/en-US/docs/Web/API/History/replaceState) on the history stack instead. This will, unlike `.pushState`, _not_ push the current page onto the history stack.

#### `[::re-route/set-prevent-nav ?text]`

Sets navigation prevention, which will cause navigation away from the current page to trigger a confirm dialog. `?text` sets the confirm dialog text (for non-[`beforeunload`](https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event) dialogs).

#### `[::re-route/unset-prevent-nav]`

Unsets navigation prevention, allowing navigation to proceed as normal again.

#### `[::re-route/on-start]` and `[::re-route/on-stop]`

Calls the `on-start` and `on-stop` multimethods, respectively. These multimethods are an alternative to defining the handlers directly; in other words, instead of writing this:

```clojure
{:start (fn [params] (dispatch [:start-my-page params]))}
```

one can write this:

```clojure
{:start (fn [params] (dispatch [::re-route/on-start :my-page params]))}

(defmethod re-route/on-start :my-page [& params] ...)
```

The `add-controllers` function takes a `[path route-data]` pair and automatically applies a `:controllers` map with a pair of `::re-route/on-start` and `::re-route/on-stop` dispatch functions.

### Subscriptions

#### `[::re-route/href route-name path-params query-params fragment]`

Creates a link from the parameters to be used in anchor tags. Clicking on the link is the same as navigation via `::re-route/navigate` for the same route and parameters.

#### `[::re-route/prevent-nav]`

Returns the current `prevent-nav` map.

#### `[::re-route/route]`

Returns the current route map.

#### `[::re-route/route-name]`

Returns the name of the current route.

#### `[::re-route/route-view]`

Returns the `:view` render function of the current route.

#### `[::re-route/path-params]`, `[::re-route/query-params]`, `[::re-route/fragment]`

Returns the appropriate parameters of the current route.

## Run Demo

This library comes with a demo that demonstrates the functionality of re-route. This can be run via Figwheel or can be compiled and run on an nginx webserver.

To run via Figwheel, run `make dev`.

To compile the demo webpage, run `make demo-bundle`, which will create a `target/demo_bundle` directory. Then to run the webpage on an nginx Docker image, `cd` to the `target/demo_bundle` directory, then run:
```
docker build -t re-route-nginx-image .
```
followed by
```
docker run --name re-route-nginx-container -p 9500:80 re-route-nginx-image
```
which will serve the webpage on `localhost:9500`.

### Screenshots

Opening the webpage at `localhost:9500` will display the home page:
![home page](doc/images/home.png)

Click on "Link to Page One" to view Page One:
![view page](doc/images/view.png)

Click on "Edit" to edit the contents of Page One:
![edit page](doc/images/edit_1.png)

Editing the text, then clicking on "Return Link" will display a navigation prevention popup:
![prevent navigation](doc/images/edit_2.png)

Going to an external link (e.g. "Link to Yet Analytics Website") will display a different dialog (corresponding to a [`beforeunload`](https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event) event):
![prevent link navigation](doc/images/edit_3.png)

## Credits

- [re-frame](https://github.com/day8/re-frame) and [reitit](https://github.com/metosin/reitit) serve as the foundations that re-route was built on.
- [kee-frame](https://github.com/ingesolvoll/kee-frame) is another re-route and reitit-based navigation library that was developed independently (we were not even aware of its existence when developing re-route).
