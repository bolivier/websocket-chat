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

(defn message-list []
  (let [messages (r/atom {})]
    (go (reset! messages
                (read-string (<! (fetch "/api/messages")))))
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
