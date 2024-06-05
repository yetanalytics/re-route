(ns com.yetanalytics.re-route.path
  (:require [re-frame.core :as re-frame]
            [reitit.frontend :as rf]
            [goog.Uri]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Current path
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-path 
  ^{:see-also ["reitit.frontend.history/-get-path"]}
  []
  (str (.. js/window -location -pathname)
       (.. js/window -location -search)
       (.. js/window -location -hash)))

(re-frame/reg-cofx
 :current-path
 (fn [cofx _]
   (assoc cofx :current-path (get-path))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; URI utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO: Move away from using goog.Uri, as the Google Closure Library
;; is no longer being maintained.

(defn location-uri
  []
  (when (exists? js/location)
    (.parse goog.Uri js/location)))

(defn element-uri
  [element]
  (.parse goog.Uri (.-href element)))

(defn has-scheme?
  [uri]
  (.hasScheme uri))

(defn has-domain?
  [uri]
  (.hasDomain uri))

(defn get-domain
  [uri]
  (.getDomain uri))

(defn uri->path
  [uri]
  (str (.getPath uri)
       (when (.hasQuery uri)
         (str "?" (.getQuery uri)))
       (when (.hasFragment uri)
         (str "#" (.getFragment uri)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Anchor tag paths
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn href
  ^{:see-also ["reitit.frontend.history/-href"]}
  [router route-name path-params query-params fragment]
  (let [match (rf/match-by-name! router route-name path-params)]
    (rf/match->path match query-params fragment)))
