(ns com.yetanalytics.re-route
  "API for preventable navigation using reitit routes."
  (:require [re-frame.core   :as re-frame]
            [reitit.frontend :as rf]
            [com.yetanalytics.re-route.listeners  :as listen]
            [com.yetanalytics.re-route.navigation :as nav]
            [com.yetanalytics.re-route.path       :as path]
            [com.yetanalytics.re-route.spec       :as spec]))

;; TODO: Use :as-alias to compress `:com.yetanalytics.re-route` into
;; `::re-route` in the other namespaces, once we update to Clojure 1.11+.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Route Controller Multimethods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti on-start
  "Multimethod that is called when the `::on-start` effect is dispatched.
   Dispatches on `route-name` to perform re-frame effects when entering that
   specific route. The method must be compatible with `reg-event-fx` and
   return a map of `:db` and/or `:fx`. Will error on unregistered `route-name`."
  (fn [_ctx [_ route-name _path-params _query-params]] route-name))

(defmulti on-stop
  "Multimethod that is called when the `::on-stop` effect is dispatched.
   Dispatches on `route-name` to perform re-frame effects when entering that
   specific route. The method must be compatible with `reg-event-fx` and
   return a map of `:db` and/or `:fx`. Will error on unregistered `route-name`."
  (fn [_ctx [_ route-name _path-params _query-params]] route-name))

;; We don't add unused controllers as a micro-optimization, to avoid
;; dispatching on unregistered values.

(defn- start-fn
  [route-name]
  (when (get-method on-start route-name)
    (fn [{path-params :path query-params :query}]
      (re-frame/dispatch [::on-start route-name path-params query-params]))))

(defn- stop-fn
  [route-name]
  (when (get-method on-stop route-name)
    (fn [{path-params :path query-params :query}]
      (re-frame/dispatch [::on-stop route-name path-params query-params]))))

(defn- parameter-keys
  "`parameters` is a map of `{:path path-params, :query query-params, ...}`.
   If (for example) `path-params` is...
   - A vector of keys: return as-is
   - A map (e.g. for spec coercion): return vector of map keys
   - Else: skip adding `path-params` entirely"
  [parameters]
  (reduce-kv ; TODO: update-vals from Clojure 1.11
   (fn [m param-type parameters*]
     (cond
       (vector? parameters*) ; regular
       (assoc m param-type parameters*)
       (map? parameters*) ; param -> spec coercion map
       (assoc m param-type (vec (keys parameters*)))
       :else ; unknown; skip parameter set
       m))
   {}
   parameters))

(defn add-controllers
  "Given a `[path route-data]` pair, add controllers if `:controllers`
   does not exist. Returns the pair with `:controllers` assoc'd to the
   `route-data` map, consisting of a single pair of start and stop controllers
   that dispatch `::on-start` and `::on-stop`, respectively.
   
   NOTE: Skips assoc'ing controllers if `on-start`/`on-stop` multimethods were
   not registered for the route `name`, and skips adding `:controllers`
   entirely if both are not registered."
  [[path {:keys [name parameters controllers]
          :as   route-data}]]
  (if controllers
    ;; custom controllers = no-op
    [path route-data]
    ;; create controllers map
    (let [?start-fn (start-fn name)
          ?stop-fn  (stop-fn name)
          ?params   (not-empty (parameter-keys parameters))
          control-m (cond-> {}
                      ?start-fn (assoc :start ?start-fn)
                      ?stop-fn  (assoc :stop ?stop-fn)
                      ?params   (assoc :parameters ?params))]
      (if (not-empty control-m)
        [path (assoc route-data :controllers [control-m])]
        [path route-data]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init Handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-prevent-nav-text
  "You have unsaved changes, are you sure?")

(def default-prevent-nav-opts
  {:enabled?     true
   :back-button? false
   :default-text default-prevent-nav-text})

(re-frame/reg-event-fx
 ::init
 [(re-frame/inject-cofx :current-path)
  (spec/spec-interceptor :init)]
 (fn [{:keys [db current-path]} [_ routes default-route prevent-nav-opts]]
   (let [router        (rf/router routes)
         default-match (rf/match-by-name router default-route)
         {:keys [enabled? back-button? default-text]}
         (merge default-prevent-nav-opts prevent-nav-opts)]
     {:db (merge db {::routes           {:current nil
                                         :default default-match}
                     ::router           router
                     ::prevent-nav      nil
                     ::prevent-nav-opts {:enabled?     enabled?
                                         :back-button? back-button?
                                         :default-text default-text}})
      :fx [[:dispatch [::nav/on-navigate current-path]]]
      ::listen/start prevent-nav-opts})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Runtime Handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-sub
 ::href
 (fn [db [_ route-name path-params query-params fragment]]
   (path/href (::router db) route-name path-params query-params fragment)))

;; Controllers

(re-frame/reg-event-fx
 ::on-start
 on-start)

(re-frame/reg-event-fx
 ::on-stop
 on-stop)

;; Navigation

;; The `::navigate` effect is the programatic version of clicking on a link or
;; typing in a URL in the browser

(re-frame/reg-event-fx
 ::navigate
 (fn [{:keys [db]} [_ route-name path-params query-params fragment]]
   (let [router (::router db)
         path   (path/href router route-name path-params query-params fragment)]
     {:fx [[:dispatch [::nav/navigate path]]]})))

(re-frame/reg-event-fx
 ::navigate-replace
 (fn [{:keys [db]} [_ route-name path-params query-params fragment]]
   (let [router (::router db)
         path   (path/href router route-name path-params query-params fragment)]
     {:fx [[:dispatch [::nav/navigate-replace path]]]})))

;; Nav Prevention

(re-frame/reg-event-fx
 ::set-prevent-nav
 [(spec/spec-interceptor ::set-prevent-nav)]
 (fn [{:keys [db]} [_ text]]
   (let [{:keys [enabled? back-button? default-text]} (::prevent-nav-opts db)]
     (if enabled?
       (let [dialog-text (or (not-empty text)
                             default-text)
             prevent-nav {:text         dialog-text
                          :back-button? back-button?}]
         {:db (assoc db ::prevent-nav prevent-nav)})
       {}))))

(re-frame/reg-event-fx
 ::unset-prevent-nav
 [(spec/spec-interceptor ::unset-prevent-nav)]
 (fn [{:keys [db]} _]
   {:db (assoc db ::prevent-nav nil)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Route Subscriptions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-sub
 ::prevent-nav
 (fn [db _]
   (::prevent-nav db)))

(re-frame/reg-sub
 ::route
 (fn [db _]
   (get-in db [::routes :current])))

(re-frame/reg-sub
 ::route-name
 :<- [::route]
 (fn [route-match _]
   (get-in route-match [:data :name])))

(re-frame/reg-sub
 ::route-view
 :<- [::route]
 (fn [route-match _]
   (get-in route-match [:data :view])))

(re-frame/reg-sub
 ::path-params
 :<- [::route]
 (fn [route-match _]
   (get route-match :path-params)))

(re-frame/reg-sub
 ::query-params
 :<- [::route]
 (fn [route-match _]
   (get route-match :query-params)))

(re-frame/reg-sub
 ::fragment
 :<- [::route]
 (fn [route-match _]
   (get route-match :fragment)))
