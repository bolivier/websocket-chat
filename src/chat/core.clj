(ns chat.core
  (:require [compojure.core :refer [defroutes GET POST]]
            [org.httpkit.server :refer [run-server with-channel send! on-close on-receive]]
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

(defonce channels (atom #{}))

(defn connect! [channel]
  (swap! channels conj channel))

(defn disconnect! [channel status]
  (swap! channels #(remove #{channel} %)))

(defn notify-clients [msg]
  (doseq [channel @channels]
    (send! channel msg)))

(defmulti ws-received first)

(defmethod ws-received :chat/add-message [[_ message]]
  (add-message message)
  (notify-clients (str {:message message})))

(defmethod ws-received :default [action]
  nil)

(defn ws-handler [req]
  (with-channel req channel
    (connect! channel)
    (on-close channel (partial disconnect! channel))
    (on-receive channel (fn [input]
                          (let [action (read-string input)]
                            (ws-received action))))))

(defstate routes
  :start [""
          ["/api"
           ["/ws" {:get ws-handler}]
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
