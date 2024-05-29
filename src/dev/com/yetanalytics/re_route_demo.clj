(ns com.yetanalytics.re-route-demo
  "Ring handler to serve paths other than `/`."
  (:require [ring.util.response :refer [file-response]]))

(defn handler [request]
  (if (= :get (:request-method request))
    (do (println (str "called handler on path: " (:uri request)))
        (file-response "resources/public/"))
    {:status  405
     :headers {"Content-Type" "text/plain"}
     :body    "Unsupported Operation"}))
