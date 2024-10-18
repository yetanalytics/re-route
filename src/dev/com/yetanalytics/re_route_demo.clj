(ns com.yetanalytics.re-route-demo
  "Ring handler to serve paths other than `/`."
  (:require [ring.util.response :refer [file-response]]))

(defn handler [request]
  (if (= :get (:request-method request))
    (file-response "dev-resources/public/")
    {:status  405
     :headers {"Content-Type" "text/plain"}
     :body    "Unsupported Operation"}))
