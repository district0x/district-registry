(ns user
  (:require
   [clojure.java.io :as io]
   [figwheel-sidecar.repl-api]
   [ring.middleware.resource :as ring-resource]))

(defn start-server! []
  (figwheel-sidecar.repl-api/start-figwheel!
    (assoc-in (figwheel-sidecar.config/fetch-config)
              [:data :figwheel-options :server-port] 4178)
    "dev-server")
  (figwheel-sidecar.repl-api/cljs-repl "dev-server"))

(def ^:private ui-handler
  (ring-resource/wrap-resource
    (fn [& _]
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (-> "public/index.html" io/resource slurp)})
   "public"))

(defn start-ui! []
  (figwheel-sidecar.repl-api/start-figwheel!
    (assoc-in (figwheel-sidecar.config/fetch-config)
      [:data :figwheel-options :ring-handler]
      'user/ui-handler)
    "dev")
  (figwheel-sidecar.repl-api/cljs-repl "dev"))

(defn start-tests! []
  (figwheel-sidecar.repl-api/start-figwheel!
    (assoc-in (figwheel-sidecar.config/fetch-config)
              [:data :figwheel-options :server-port] 4179)
    "server-tests")
  (figwheel-sidecar.repl-api/cljs-repl "server-tests"))

(comment
  (start-ui!)
  (start-server!)
  (start-tests!))
