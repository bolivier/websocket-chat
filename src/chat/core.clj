(ns chat.core
  (:require [compojure.core :refer [defroutes GET POST]]
   [org.httpkit.server :refer [run-server]]
            [mount.core :refer [defstate]]))

(defn index-handler [req]
  {:status 200
   :body (str "uri: " (:uri req))})

(defroutes my-app
  (GET  "/"            req (index-handler req))
  (POST "/submit-form" req (index-handler req)))

(defn app [req]
  (index-handler req))

(defstate server
  :start (run-server #'app {:port 3000
                            :join? false})
  :stop (server))
