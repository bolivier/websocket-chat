(ns chat.core
  (:require [reagent.dom :as dom]
            [reagent.core :as r]
            [cljs.reader :refer [read-string]]
            [cljs.core.async :refer-macros [go] :refer [chan put! <! >!]]))

(defn fetch [url]
  (let [c (chan)]
   (-> url
       js/fetch
       (.then (fn [response] (.text response)))
       (.then (fn [data] (put! c data))))
   c))

(defn receive-transit-msg! [update-fn]
  (fn [msg]
    (update-fn (->> msg
                    .-data
                    read-string))))

(def ws-chan (atom nil))
(defn send-transit-msg! [msg]
  (if @ws-chan
    (.send @ws-chan (str msg))
    (throw (js/Error "Websocket not available"))))

(defn make-websocket! [url receive-handler]
  (println "attempting to connect websocket")
  (if-let [chan (js/WebSocket. url)]
    (do
      (set! (.-onmessage chan) (receive-transit-msg! receive-handler))
      (reset! ws-chan chan)
      (println "connection established"))
    (throw (js/Error. "connection failed!"))))

(defn message-list []
  (let [messages (r/atom {})]
    (go (reset! messages
                (read-string (<! (fetch "/api/messages")))))
    (make-websocket! "ws://localhost:3000/api/ws" (fn [msg] (js/console.log (str msg))))
    (fn []
      [:div
       [:h2 "Messages"]
       [:ul
        (map (fn [{:keys [message]}]
               [:li ^{:key message} message])
             @messages)]])))

(defn app []
  [message-list])

(defn ^:dev/after-load init []
  (dom/render [app]
              (.getElementById js/document "app")))
