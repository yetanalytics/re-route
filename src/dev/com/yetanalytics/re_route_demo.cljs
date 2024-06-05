(ns ^:figwheel-hooks com.yetanalytics.re-route-demo
  "In this example, we use the re-route API to navigate between pages."
  (:require [clojure.spec.alpha :as s]
            [reagent.dom :as rdom]
            [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [goog.dom :as gdom]
            [com.yetanalytics.re-route :as re-route]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Re-frame
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::uuid (s/nilable uuid?))
(s/def ::string string?)
(s/def ::edit-buffer string?)

(s/def ::page-one
  (s/keys :req-un [::uuid ::string ::edit-buffer]))

(s/def ::page-two
  (s/keys :req-un [::uuid ::string ::edit-buffer]))

(def db-spec
  (s/keys :req-un [::page-one ::page-two]))

;; Subscriptions

(re-frame/reg-sub
 :page-one-uuid
 (fn [db _]
   (str (get-in db [:page-one :uuid]))))

(re-frame/reg-sub
 :page-two-uuid
 (fn [db _]
   (str (get-in db [:page-two :uuid]))))

(re-frame/reg-sub
 :page-one-string
 (fn [db _]
   (get-in db [:page-one :string])))

(re-frame/reg-sub
 :page-two-string
 (fn [db _]
   (get-in db [:page-two :string])))

(re-frame/reg-sub
 :page-one-edit
 (fn [db _]
   (get-in db [:page-one :edit-buffer])))

(re-frame/reg-sub
 :page-two-edit
 (fn [db _]
   (get-in db [:page-two :edit-buffer])))

(re-frame/reg-sub
 :page-three-edit
 (fn [db [_ key]]
   (get-in db [:page-three :edit-buffer key])))

(re-frame/reg-sub
 :page-three-params/path
 :<- [::re-route/path-params :pages/three]
 (fn [{:keys [path-param]} _]
   (js/decodeURIComponent path-param)))

(re-frame/reg-sub
 :page-three-params/query
 :<- [::re-route/query-params :pages/three]
 (fn [{:keys [query-param]} _]
   (js/decodeURIComponent query-param)))

(re-frame/reg-sub
 :page-three-params/fragment
 :<- [::re-route/fragment :pages/three]
 (fn [fragment _]
   (js/decodeURIComponent fragment)))

;; Controllers: Using on-start/on-stop multimethods

(defmethod re-route/on-start :pages.one/view [{:keys [db]} _]
  {:db (assoc-in db [:page-one :uuid] (random-uuid))})

(defmethod re-route/on-stop :pages.one/view [{:keys [db]} _]
  {:db (assoc-in db [:page-one :uuid] nil)})

(defmethod re-route/on-start :pages.one/edit [{:keys [db]} _]
  {:db (assoc-in db [:page-one :edit-buffer] (get-in db [:page-one :string]))})

(defmethod re-route/on-stop :pages.one/edit [{:keys [db]} _]
  {:db (assoc-in db [:page-one :edit-buffer] "")})


;; Controllers: Direct dispatch

(re-frame/reg-event-fx
 :start.pages.two/view
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:page-two :uuid] (random-uuid))}))

(re-frame/reg-event-fx
 :stop.pages.two/view
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:page-two :uuid] nil)}))

(re-frame/reg-event-fx
 :start.pages.two/edit
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:page-two :edit-buffer] (get-in db [:page-two :string]))}))

(re-frame/reg-event-fx
 :stop.pages.two/edit
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:page-two :edit-buffer] "")}))

(re-frame/reg-event-fx
 :start.pages/three
 (fn [{:keys [db]} [_ {:keys [path-param query-param fragment]}]]
   {:db (assoc-in db [:page-three :edit-buffer]
                  {:path-param  (js/decodeURIComponent path-param)
                   :query-param (js/decodeURIComponent query-param)
                   :fragment    (js/decodeURIComponent fragment)})}))

(re-frame/reg-event-fx
 :stop.pages/three
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:page-three :edit-buffer] {})}))

;; Text fields

(re-frame/reg-event-fx
 :set-text.pages/one
 (fn [{:keys [db]} [_ text]]
   {:db (assoc-in db [:page-one :edit-buffer] text)}))

(re-frame/reg-event-fx
 :set-text.pages/two
 (fn [{:keys [db]} [_ text]]
   {:db (assoc-in db [:page-two :edit-buffer] text)}))

(re-frame/reg-event-fx
 :set-text.pages/three
 (fn [{:keys [db]} [_ key text]]
   {:db (assoc-in db [:page-three :edit-buffer key] text)}))

(re-frame/reg-event-fx
 :save-text.pages/one*
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:page-one :string] (get-in db [:page-one :edit-buffer]))}))

(re-frame/reg-event-fx
 :save-text.pages/one
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:page-one :string] (get-in db [:page-one :edit-buffer]))
    :fx [[:dispatch [::re-route/navigate-replace :pages.one/edit {} {:saved true}]]]}))

(re-frame/reg-event-fx
 :save-text.pages/two*
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:page-two :string] (get-in db [:page-two :edit-buffer]))}))

(re-frame/reg-event-fx
 :save-text.pages/two
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:page-two :string] (get-in db [:page-two :edit-buffer]))
    :fx [[:dispatch [::re-route/navigate-replace :pages.two/edit {} {:saved true}]]]}))

(re-frame/reg-event-fx
 :save-text.pages/three
 (fn [{:keys [db]} _]
   (let [{:keys [path-param query-param fragment]}
         (get-in db [:page-three :edit-buffer])]
     {:fx [[:dispatch [::re-route/navigate-replace
                       :pages/three
                       {:path-param (js/encodeURIComponent path-param)}
                       {:query-param (js/encodeURIComponent query-param)}
                       (js/encodeURIComponent fragment)]]]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn home-page []
  [:div {:id "home-page"}
   [:h1 "This is the home page"]
   [:a {:href @(subscribe [::re-route/href :pages.one/view])}
    "Link to Page One"]
   [:a {:href @(subscribe [::re-route/href :pages.two/view])}
    "Link to Page Two"]
   [:a {:href @(subscribe [::re-route/href :pages/three {:path-param "default"} {:query-param "default"} "default"])}
    "Link to Page Three"]
   [:a {:href "https://www.yetanalytics.com/"}
    "Link to the Yet Analytics Website"]])

(defn not-found []
  [:div {:id "not-found"}
   [:h1 "Error 404: Not Found"]])

(defn view-one []
  [:div {:id "view-one"}
   [:h1 "View One"]
   [:p "This is a random UUID: " @(subscribe [:page-one-uuid])]
   [:p "Current string: " @(subscribe [:page-one-string])]
   [:a {:href @(subscribe [::re-route/href :pages.one/edit])}
    "Edit"]
   [:a {:href @(subscribe [::re-route/href :pages/home])}
    "Home"]])

(defn view-two []
  [:div {:id "view-two"}
   [:h1 "View Two"]
   [:p "This is a random UUID: " @(subscribe [:page-two-uuid])]
   [:p "Current string: " @(subscribe [:page-two-string])]
   [:a {:href @(subscribe [::re-route/href :pages.two/edit])}
    "Edit"]
   [:a {:href @(subscribe [::re-route/href :pages/home])}
    "Home"]])

(defn- edit-one []
  [:div {:id "edit-one"}
   [:h1 "Edit One"]
   (when @(subscribe [::re-route/prevent-nav])
     [:p "Unsaved changes have been made."])
   [:input
    {:name      (random-uuid)
     :type      "text"
     :value     @(subscribe [:page-one-edit])
     :on-change (fn [e]
                  (.preventDefault e)
                  (.stopPropagation e)
                  (let [v (.. e -target -value)]
                    (dispatch [::re-route/set-prevent-nav])
                    (dispatch [:set-text.pages/one v])))}]
   [:button
    {:on-click (fn [e]
                 (.preventDefault e)
                 (.stopPropagation e)
                 (dispatch [::re-route/unset-prevent-nav])
                 (dispatch [:save-text.pages/one]))}
    "Save"]
   [:a
    {:href @(subscribe [::re-route/href :pages.one/view])}
    "Return Link"]
   [:a
    {:class    "link-like"
     :on-click (fn [_]
                 ;; Deliberately not prevent default action, to see that
                 ;; the event gets intercepted by the on-click listener
                 (dispatch [::re-route/unset-prevent-nav])
                 (dispatch [:save-text.pages/one*])
                 (dispatch [::re-route/navigate :pages.one/view]))}
    "Save and Return Link"]
   [:a
    {:href "http://yetanalytics.com"}
    "Link to Yet Analytics Website"]])

(defn- edit-two []
  [:div {:id "edit-two"}
   [:h2 "Edit Two"]
   (when @(subscribe [::re-route/prevent-nav])
     [:p "Unsaved changes have been made."])
   [:input
    {:id        (random-uuid)
     :type      "text"
     :value     @(subscribe [:page-two-edit])
     :on-change (fn [e]
                  (.preventDefault e)
                  (.stopPropagation e)
                  (let [v (.. e -target -value)]
                    (dispatch [::re-route/set-prevent-nav])
                    (dispatch [:set-text.pages/two v])))}]
   [:button
    {:on-click (fn [e]
                 (.preventDefault e)
                 (.stopPropagation e)
                 (dispatch [::re-route/unset-prevent-nav])
                 (dispatch [:save-text.pages/two]))}
    "Save"]
   [:button
    {:on-click (fn [e]
                 (.preventDefault e)
                 (.stopPropagation e)
                 (dispatch [::re-route/navigate :pages.two/view]))}
    "Return Button"]
   [:button
    {:on-click (fn [e]
                 (.preventDefault e)
                 (.stopPropagation e)
                 (dispatch [::re-route/unset-prevent-nav])
                 (dispatch [:save-text.pages/two*])
                 (dispatch [::re-route/navigate :pages.two/view]))}
    "Save and Return Button"]])

(defn edit-three []
  [:div {:id "view-three"}
   [:h1 "View Three"]
   [:div
    [:h2 "Current Parameters:"]
    [:p "Path: " @(subscribe [:page-three-params/path])]
    [:p "Query: " @(subscribe [:page-three-params/query])]
    [:p "Fragment: " @(subscribe [:page-three-params/fragment])]]
   [:div
    [:h2 "New Parameters:"]
    (when @(subscribe [::re-route/prevent-nav])
      [:p "Unsaved changes have been made."])
    [:p "Path:"]
    [:input
     {:name (random-uuid)
      :type "text"
      :value @(subscribe [:page-three-edit :path-param])
      :on-change (fn [e]
                   (.preventDefault e)
                   (.stopPropagation e)
                   (let [v (.. e -target -value)]
                     (dispatch [::re-route/set-prevent-nav])
                     (dispatch [:set-text.pages/three :path-param v])))}]
    [:p "Query:"]
    [:input
     {:name (random-uuid)
      :type "text"
      :value @(subscribe [:page-three-edit :query-param])
      :on-change (fn [e]
                   (.preventDefault e)
                   (.stopPropagation e)
                   (let [v (.. e -target -value)]
                     (dispatch [::re-route/set-prevent-nav])
                     (dispatch [:set-text.pages/three :query-param v])))}]
    [:p "Fragment:"]
    [:input
     {:name (random-uuid)
      :type "text"
      :value @(subscribe [:page-three-edit :fragment])
      :on-change (fn [e]
                   (.preventDefault e)
                   (.stopPropagation e)
                   (let [v (.. e -target -value)]
                     (dispatch [::re-route/set-prevent-nav])
                     (dispatch [:set-text.pages/three :fragment v])))}]
    [:button
     {:on-click (fn [e]
                  (.preventDefault e)
                  (.stopPropagation e)
                  (dispatch [::re-route/unset-prevent-nav])
                  (dispatch [:save-text.pages/three]))}
     "Set Parameters"]
    [:a {:href @(subscribe [::re-route/href :pages/home])}
     "Home"]]])

(defn main-view []
  []
  (when-some [page @(subscribe [::re-route/route-view])]
    [page]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def routes
  [["/"
    {:name :pages/home
     :view home-page}]
   ["/one/view"
    {:name :pages.one/view
     :view view-one
     :controllers [{:identity (fn [_] nil)
                    :start    (fn [params]
                                (dispatch [::re-route/on-start :pages.one/view params]))
                    :stop     (fn [params]
                                (dispatch [::re-route/on-stop :pages.one/view params]))}]}]
   ["/one/edit"
    {:name :pages.one/edit
     :view edit-one
     :controllers [{:identity (fn [_] nil)
                    :start    (fn [params]
                                (dispatch [::re-route/on-start :pages.one/edit params]))
                    :stop     (fn [params]
                                (dispatch [::re-route/on-stop :pages.one/edit params]))}]}]
   ["/two/view"
    {:name :pages.two/view
     :view view-two
     :controllers [{:identity (fn [_] nil)
                    :start    (fn [params]
                                (dispatch [:start.pages.two/view params]))
                    :stop     (fn [params]
                                (dispatch [:stop.pages.two/view params]))}]}]
   ["/two/edit"
    {:name :pages.two/edit
     :view edit-two
     :controllers [{:identity (fn [_] nil)
                    :start    (fn [params]
                                (dispatch [:start.pages.two/edit params]))
                    :stop     (fn [params]
                                (dispatch [:stop.pages.two/edit params]))}]}]
   ["/three/:path-param/view"
    {:name        :pages/three
     :view        edit-three
     :parameters  {:path  [:path-param]
                   :query [:query-param]}
     :controllers [{:identity (fn [{{:keys [path-param]}  :path-params
                                    {:keys [query-param]} :query-params
                                    fragment              :fragment}]
                                {:path-param  path-param
                                 :query-param query-param
                                 :fragment    fragment})
                    :start    (fn [params]
                                (dispatch [:start.pages/three params]))
                    :stop     (fn [params]
                                (dispatch [:stop.pages/three params]))}]}]
   ["/not-found"
    {:name :not-found
     :view not-found}]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Load and Display SPA
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-fx
 ::init-db
 (fn [_ _]
   {:db {:page-one   {:uuid        nil
                      :string      "Yet Analytics"
                      :edit-buffer ""}
         :page-two   {:uuid        nil
                      :string      "Supercalifragilisticexpialidocious!"
                      :edit-buffer ""}
         :page-three {:edit-buffer {:path-param  ""
                                    :query-param ""
                                    :fragment    ""}}}
    :fx [[:dispatch [::re-route/init routes :not-found {:enabled?     true
                                                        :back-button? true}]]]}))

(defn mount [element]
  (rdom/render [main-view] element))

(defn mount-app-element []
  (when-let [el (gdom/getElement "app")]
    (mount el)))

(defn init! []
  (re-frame/dispatch-sync [::init-db])
  (mount-app-element))

(defonce init
  (init!))

;; specify reload hook with ^:after-load metadata
(defn ^:after-load on-reload []
  (println "figwheel reload!")
  (init!)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
