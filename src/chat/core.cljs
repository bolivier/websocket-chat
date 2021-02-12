(ns chat.core
  (:require [reagent.dom :as dom]
            [reagent.core :as r]
            [cljs.reader :refer [read-string]]
            [cljs.core.async :refer-macros [go go-loop] :refer [chan put! <! >!]]
            [clojure.string :as str]))

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

(defn make-async-socket! [url]
  (let [c (chan)]
    (make-websocket! "ws://localhost:3000/api/ws" #(put! c %))
    c))

(defn message-list []
  (let [messages (r/atom {})
        updates (make-async-socket! "/api/ws")]
    (go (reset! messages
                (read-string (<! (fetch "/api/messages")))))
    (go-loop []
      (let [msg (<! updates)]
        (swap! messages conj msg)))

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
        {:on-click #(do (send-transit-msg! (str [:chat/add-message @value]))
                        (reset! value ""))}
        "Send"]])))

(defn app []
  (let [input-chan (chan)
        ws-pub (pub input-chan first)
        output-chan (chan)]
   [:div
    [message-list {:updates (sub output-chan :)}]
    [message-sender]]))

(defn ^:dev/after-load init []
  (dom/render [app]
              (.getElementById js/document "app")))
