(ns chat.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [mount.core :refer [defstate]]))

(defn app [_]
  {:status 200
   :body "hello world"})

(defstate server
  :start (run-jetty #'app {:port 3000
                           :join? false})
  :stop (.stop server))
