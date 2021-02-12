(ns chat.core
  (:require [compojure.core :refer [defroutes GET POST]]
            [org.httpkit.server :refer [run-server]]
            [reitit.ring :as ring]
            [ring.middleware.resource :as rs]
            [mount.core :refer [defstate]]
            [clojure.string :as str]))



(defonce messages (atom []))

(defn add-message [message]
  (swap! messages conj {:message message}))

(defn messages-handler [req]
  {:status 200
   :body (str @messages)})

(defstate routes
  :start [""
          ["/api"
           ["/messages" {:get messages-handler}]
           ["/submit-form" {:post (fn [req]
                                    {:status 200
                                     :body   "form submitted"})}]]])

(defstate my-app
  :start (ring/ring-handler (ring/router routes)
                            (ring/create-resource-handler
                             {:root ""
                              :path "/"})))

(defstate server
  :start (run-server #'my-app {:port  3000
                               :join? false})
  :stop (server))
