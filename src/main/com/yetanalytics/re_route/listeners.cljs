(ns com.yetanalytics.re-route.listeners
  (:require [goog.Uri]
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
  (if (exists? (.-composedPath event))
    (aget (.composedPath event) 0)
    (.-target event)))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Back Button Listeners
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- on-popstate
  [_event]
  (re-frame/dispatch-sync [::nav/navigate-back]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Anchor Tag + Button Listeners
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- on-click
  [event]
  (when-let [path (path-for-ignore-click event)]
    (prevent-default! event)
    (re-frame/dispatch [::nav/navigate path])))

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

(defn- on-beforeunload
  [event]
  (re-frame/dispatch-sync [::on-beforeunload event]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Listener start and stop
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def type-beforeunload "beforeunload")
(def type-popstate "popstate")
(def type-click "click")

(defn- start-event-listeners!
  []
  ;; TODO: We ought to only listen to beforeunload when prevent-nav is
  ;; true, in order to avoid performance penalities (esp. on Firefox)
  ;; See: https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event#usage_notes
  (.addEventListener js/window type-beforeunload on-beforeunload)
  (.addEventListener js/window type-popstate on-popstate)
  (.addEventListener js/document type-click on-click))

(defn- stop-event-listeners!
  []
  (.removeEventListener js/window type-beforeunload on-beforeunload)
  (.removeEventListener js/window type-popstate on-popstate)
  (.removeEventListener js/document type-click on-click))

(re-frame/reg-fx
 ::start
 (fn [_] (start-event-listeners!)))

(re-frame/reg-fx
 ::stop
 (fn [_] (stop-event-listeners!)))
