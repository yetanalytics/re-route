(ns com.yetanalytics.re-route.navigation
  (:require [re-frame.core :as re-frame]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rfc]
            [com.yetanalytics.re-route.spec :as spec]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; History stack manipulation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn replace-state
  [path]
  (.replaceState js/history nil "" path))

(defn push-state
  [path]
  (.pushState js/history nil "" path))

(re-frame/reg-fx
 ::replace-fx
 replace-state)

(re-frame/reg-fx
 ::push-fx
 push-state)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; on-navigate handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- apply-controllers
  [old-route-match new-route-match]
  (let [old-controllers (:controllers old-route-match)
        new-controllers (rfc/apply-controllers old-controllers new-route-match)]
    (assoc new-route-match :controllers new-controllers)))

(re-frame/reg-event-fx
 ::on-navigate
 [(spec/spec-interceptor ::on-navigate)]
 (fn [{:keys [db]} [_ path]]
   (let [{default-route-match :default
          old-route-match     :current}
         (:com.yetanalytics.re-route/routes db)
         router            (:com.yetanalytics.re-route/router db)
         new-route-match   (rf/match-by-path router path)
         new-route-match*  (or new-route-match default-route-match)
         new-route-match** (apply-controllers old-route-match new-route-match*)]
     {:db (assoc-in db
                    [:com.yetanalytics.re-route/routes :current]
                    new-route-match**)})))

(re-frame/reg-event-fx
 ::on-navigate-replace
 (fn [_ [_ path]]
   {::replace-fx path
    :fx [[:dispatch [::on-navigate path]]]}))

(re-frame/reg-event-fx
 ::on-navigate-push
 (fn [_ [_ path]]
   {::push-fx path
    :fx [[:dispatch [::on-navigate path]]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; unset-prevent-nav handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 ::unset-prevent-nav
 (fn [{:keys [db]} [_ on-success-dispatch]]
   {:db (assoc db :com.yetanalytics.re-route/prevent-nav nil)
    :fx [[:dispatch on-success-dispatch]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; navigate handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 ::navigate
 (fn [{:keys [db]} [_ path]]
   (let [{:keys [text] :as prevent-nav}
         (:com.yetanalytics.re-route/prevent-nav db)]
     (if (nil? prevent-nav)
       {:fx [[:dispatch [::on-navigate-push path]]]}
       (when (js/confirm text)
         {:fx [[:dispatch [::unset-prevent-nav
                           [::on-navigate-push path]]]]})))))

(re-frame/reg-event-fx
 ::navigate-replace
 (fn [{:keys [db]} [_ path]]
   (let [{:keys [text] :as prevent-nav}
         (:com.yetanalytics.re-route/prevent-nav db)]
     (if (nil? prevent-nav)
       {:fx [[:dispatch [::on-navigate-replace path]]]}
       (when (js/confirm text)
         {:fx [[:dispatch [::unset-prevent-nav
                           [::on-navigate-replace path]]]]})))))

(re-frame/reg-event-fx
 ::navigate-back
 [(re-frame/inject-cofx :current-path)]
 (fn [{:keys [db current-path]} _]
   (let [{{prev-path :path} :current}
         (:com.yetanalytics.re-route/routes db)
         {:keys [text back-button?] :as prevent-nav}
         (:com.yetanalytics.re-route/prevent-nav db)]
     (if (or (nil? prevent-nav)
             (not back-button?))
       {:fx [[:dispatch [::on-navigate current-path]]]}
       (if (js/confirm text)
         {:fx [[:dispatch [::unset-prevent-nav [::on-navigate current-path]]]]}
         ;; Restore where back button was before it was popped
         ;; Note that this causes strange behaviors if it was the forward button
         ;; that was pressed, or if the user went back multiple pages, but
         ;; not much we can do there ¯\_(ツ)_/¯
         {::push-fx prev-path})))))
