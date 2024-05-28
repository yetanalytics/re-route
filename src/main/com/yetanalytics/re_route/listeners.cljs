(ns com.yetanalytics.re-route.listeners
  (:require [goog.events :as gevents]
            [goog.Uri]
            [re-frame.core :as re-frame]
            [com.yetanalytics.re-route.navigation :as nav]
            [com.yetanalytics.re-route.path :as path]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Event and element
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- prevent-default! [event]
  (.preventDefault event))

(defn- set-event-return! [event v]
  ;; `event.returnValue = v` in JavaScript
  (set! (.. event -returnValue) v))

(defn- closest-by-tag
  ^{:see-also ["reitit.frontend.history/closest-by-tag"]}
  [element tag]
  ;; nodeName is upper case for HTML always,
  ;; for XML or XHTML it would be in the original case.
  (let [tag (.toUpperCase tag)]
    (loop [el element]
      (when el
        (if (= tag (.-nodeName el))
          el
          (recur (.-parentNode el)))))))

(defn event-target
  "Read event's target from composed path to get shadow dom working,
  fallback to target property if not available"
  ^{:see-also ["reitit.frontend.history/event-target"]}
  [event]
  (let [original-event (.getBrowserEvent event)]
    (if (exists? (.-composedPath original-event))
      (aget (.composedPath original-event) 0)
      (.-target event))))

(defn ignore-anchor-click?
  ^{:see-also ["reitit.frontend.history/ignore-anchor-click?"]}
  [event element uri]
  (let [?current-domain (when (exists? js/location)
                          (.getDomain (.parse goog.Uri js/location)))]
    (and (or (and (not (.hasScheme uri))
                  (not (.hasDomain uri)))
             (= ?current-domain (.getDomain uri)))
         (not (.-altKey event))
         (not (.-ctrlKey event))
         (not (.-metaKey event))
         (not (.-shiftKey event))
         (or (not (.hasAttribute element "target"))
             (contains? #{"" "_self"} (.getAttribute element "target")))
         (= 0 (.-button event))
         (not (.-isContentEditable element)))))

(defn- path-for-ignore-click
  [event]
  (when-let [element (closest-by-tag (event-target event) "a")]
    (let [uri (path/element-uri element)]
      (when (ignore-anchor-click? event element uri)
        (path/uri->path uri)))))

(re-frame/reg-event-fx
 ::unset-prevent-nav
 (fn [{:keys [db]} _]
   (let [listener-keys (:com.yetanalytics.re-route/listener-keys db)]
     {:db (assoc db :com.yetanalytics.re-route/prevent-nav nil)
      ::stop-beforeunload listener-keys})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Listener Keys
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 ::set-listener-key
 (fn [{:keys [db]} [_ k v]]
   {:db (assoc-in db [:com.yetanalytics.re-route/listener-keys k] v)}))

(re-frame/reg-event-db
 ::set-listener-keys
 (fn [db [_ keys-m]]
   (assoc db :com.yetanalytics.re-route/listener-keys keys-m)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Back Button Listeners
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-fx
 ::on-popstate-fx
 (fn [[prev-path current-path]]
   (re-frame/dispatch [::nav/navigate-back prev-path current-path])))

(re-frame/reg-event-fx
 ::on-popstate
 [(re-frame/inject-cofx :current-path)]
 (fn [{:keys [db current-path]} _]
   (let [prev-path (-> db :com.yetanalytics.re-route/routes :current :path)]
     {::on-popstate-fx [prev-path current-path]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Anchor Tag + Button Listeners
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-fx
 ::on-click-fx
 (fn [event]
   (when-let [path (path-for-ignore-click event)]
     (prevent-default! event)
     (re-frame/dispatch [::nav/navigate path]))))

(re-frame/reg-event-fx
 ::on-click
 (fn [_ [_ event]]
   {::on-click-fx event}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Browser URL Listeners
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-fx
 ::on-beforeunload-fx
 (fn [[text event]]
   (set-event-return! event text)
   ;; returnValue is deprecated so use preventDefault as a fallback
   (prevent-default! event)))

(re-frame/reg-event-fx
 ::on-beforeunload
 (fn [{:keys [db]} [_ event]]
   (if-some [{:keys [text] :as _prevent-nav}
             (:com.yetanalytics.re-route/prevent-nav db)]
     {::on-beforeunload-fx [text event]}
     {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Listener start and stop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- start-event-listeners!
  []
  (let [;; TODO: We ought to only listen to beforeunload when prevent-nav is
        ;; true, in order to avoid performance penalities (esp. on Firefox)
        ;; See: https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event#usage_notes
        beforeunload-key
        (gevents/listen
         js/window
         goog.events.EventType.BEFOREUNLOAD
         (fn [event]
           (re-frame/dispatch-sync [::on-beforeunload event])))
        popstate-key
        (gevents/listen
         js/window
         goog.events.EventType.POPSTATE
         (fn [event]
           (re-frame/dispatch-sync [::on-popstate event]))
         false)
        click-key
        (gevents/listen
         js/document
         goog.events.EventType.CLICK
         (fn [event]
           (re-frame/dispatch-sync [::on-click event])))]
    (re-frame/dispatch [::set-listener-keys {:beforeunload beforeunload-key
                                             :popstate     popstate-key
                                             :click        click-key}])))

(defn- stop-event-listeners!
  [{:keys [beforeunload popstate click]}]
  (gevents/unlistenByKey beforeunload)
  (gevents/unlistenByKey popstate)
  (gevents/unlistenByKey click)
  (re-frame/dispatch [::set-listener-keys {:beforeunload nil
                                           :popstate     nil
                                           :click        nil}]))

(re-frame/reg-fx
 ::start
 (fn [_] (start-event-listeners!)))

(re-frame/reg-fx
 ::stop
 (fn [listener-keys] (stop-event-listeners! listener-keys)))
