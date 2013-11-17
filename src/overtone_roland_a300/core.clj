(ns overtone-roland-a300.core
  (:require [clojure.string :refer [join]]
            [midje.sweet :refer :all]
            [overtone.libs.event :refer [event
                                         on-event
                                         remove-handler]]))

(def config
  {:channels {0 :key
              1 :b
              2 :l
              3 :r
              4 :s
              5 :pad
              6 :bend
              7 :mod}})

(defn- get-channel [n] (get (:channels config) n))

(defn- key-event [e]
  (let [is-down (= (:command e) :note-on)
        direction (if is-down :down :up)]
    (event [:midi :key direction] e)))

(defn- bend-event [e]
  (let [position (:velocity e)
        bending (- position 64)
        bending-f (/ bending 127)]
    (event
      [:midi :bend])
      (assoc e :bending bending
               :bending-f bending-f)))

(defn- mod-event [e]
  (event
    [:midi :mod]
    (assoc e :value (:velocity e)
             :value-f (:velocity-f e))))

(defn- pad-event [e]
  (let [pad-name (join "pad" (:note e))
        is-down (= (:note e) :note-on)
        direction (if is-down :down :up)]
    (event [:midi :pad direction] e)
    (event [:midi pad-name direction] e)))

(defn- control-event [e]
  (let [channel-num (:channel e)
        control-num (:note e)
        control-range (get-channel (:channel e))
        channel (join (name control-range) control-num)]
    (event
      [:midi (keyword channel)]
      (assoc e :value (:velocity e)
               :value-f (:velocity-f e)))))

(def ^:private channel-handlers
  {:key key-event
   :bend bend-event
   :mod mod-event
   :pad pad-event
   :b control-event
   :l control-event
   :r control-event
   :s control-event})

(defn- handle-event [e]
  (let [channel (get-channel (:channel e))
        handler (get channel-handlers channel)]
  (handler e)))

(defn handle-a300-events []
  (on-event [:midi :control-change] #(handle-event %) ::a300-control-change)
  (on-event [:midi :note-on] #(handle-event %) ::a300-note-on)
  (on-event [:midi :note-off] #(handle-event %) ::a300-note-off))

(defn remove-a300-event-handlers []
  (remove-handler ::a300-control-change)
  (remove-handler ::a300-note-on)
  (remove-handler ::a300-note-off))

(handle-a300-events)
