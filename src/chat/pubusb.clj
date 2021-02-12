(ns chat.pubusb
  (:require [clojure.core.async :refer [>! <! go pub sub go-loop chan >!! <!!]]))

(def input-chan (chan))
(def our-pub (pub input-chan :msg-type))

(def output-chan (chan))
(sub our-pub :greeting output-chan)

(go-loop []
  (let [{:keys [text]} (<! output-chan)]
    (println text)
    (recur)))

(go (>! input-chan {:msg-type :greeting :text "Hello brandon"}))
