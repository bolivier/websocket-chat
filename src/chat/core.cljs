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
    (make-websocket! "ws://localhost:3000/api/ws" (fn [msg]
                                                    (swap! messages conj msg)
                                                    (js/console.log (str msg))))
    (fn []
      [:div
       [:h2 "Messages"]
       [:ul
        (map (fn [{:keys [message]}]
               [:li ^{:key message} message])
             @messages)]])))

(defn message-sender []
  (let [value (r/atom "")]
    (fn []
      [:div
       [:input {:placeholder "message"
                :value @value
                :on-change #(reset! value (.. % -target -value))}]
       [:button
        {:on-click #(do (send-transit-msg! @value)
                        (reset! value ""))}
        "Send"]])))

(defn app []
  [:div
   [message-list]
   [message-sender]])

(defn ^:dev/after-load init []
  (dom/render [app]
              (.getElementById js/document "app")))
