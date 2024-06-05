(ns com.yetanalytics.re-route.spec
  (:require [clojure.spec.alpha :as s]
            [re-frame.core      :as re-frame]
            [reitit.spec        :as rs]))

;; route + router specs

(s/def :route.data/name any?)
(s/def :route.data/view fn?)
(s/def :route/data (s/keys :req-un [:route.data/name]
                           :opt-un [:route.data/view]))

(s/def :route/path-params map?)
(s/def :route/query-params map?)
(s/def :route/fragment (s/nilable string?))

(s/def :routes/route (s/keys :req-un [:route/data]
                             :opt-un [:route/path-params
                                      :route/query-params
                                      :route/fragment]))

;; nil on immediate init, non-nil after on-navigate
(s/def :routes/current (s/nilable :routes/route))

(s/def :routes/default :routes/route)

(s/def :com.yetanalytics.re-route/routes
  (s/keys :req-un [:routes/current
                   :routes/default]))

(s/def :com.yetanalytics.re-route/router
  ::rs/router)

;; prevent-nav specs

(s/def :prevent-nav/enabled? boolean?)
(s/def :prevent-nav/back-button? boolean?)

(s/def :prevent-nav/text string?)
(s/def :prevent-nav/default-text string?)

(s/def :com.yetanalytics.re-route/prevent-nav
  (s/nilable (s/keys :req-un [:prevent-nav/text
                              :prevent-nav/back-button?])))

(s/def :com.yetanalytics.re-route/prevent-nav-opts
  (s/keys :req-un [:prevent-nav/enabled?
                   :prevent-nav/back-button?
                   :prevent-nav/default-text]))

;; Combined specs

(def db-spec
  (s/keys :req [:com.yetanalytics.re-route/router
                :com.yetanalytics.re-route/routes
                :com.yetanalytics.re-route/prevent-nav
                :com.yetanalytics.re-route/prevent-nav-opts]))

;; Interceptors

(defn validate-db
  [handler-name db-spec db]
  (when-not (s/valid? db-spec db)
    (let [msg* (str "Spec check failed in " handler-name ":")
          msg  (str msg* "\n" (s/explain-str db-spec db))]
      (throw (ex-info msg {})))))

(defn spec-interceptor [handler-name]
  (re-frame/after (partial validate-db handler-name db-spec)))
