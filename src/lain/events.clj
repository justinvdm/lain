(ns lain.events
  (:require [clojure.string :refer [join]]
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
      [:midi :bend]
      (assoc e :bending bending
               :bending-f bending-f))))

(defn- mod-event [e]
  (event
    [:midi :mod]
    (assoc e :value (:velocity e)
             :value-f (:velocity-f e))))

(defn- pad-event [e]
  (let [pad-num (:note e)
        pad-name (join ["pad" (str pad-num)])
        pad-name (keyword pad-name)
        is-down (= (:command e) :note-on)
        direction (if is-down :down :up)]
    (event [:midi :pad direction] e)
    (event [:midi pad-name direction] e)))

(defn- get-control-name [e]
  (let [channel-num (:channel e)
        control-num (:note e)
        control-range (get-channel (:channel e))]
    (-> [(name control-range)
         (str control-num)]
        join
        keyword)))

(defn- fuzzy-event [e]
  (event
    [:midi (get-control-name e)]
    (assoc e :value (:velocity e)
             :value-f (:velocity-f e))))

(defn- toggle-event [e]
  (let [control-name (get-control-name e)
        is-on (= 127 (:velocity e))
        status (if is-on :on :off)]
    (event [:midi control-name status] e)))

(def ^:private channel-handlers
  {:key key-event
   :bend bend-event
   :mod mod-event
   :pad pad-event
   :b toggle-event
   :l toggle-event
   :r fuzzy-event
   :s fuzzy-event})

(defn- handle-event [e]
  (let [channel (get-channel (:channel e))
        handler (get channel-handlers channel)]
  (handler e)))

(defn handle-a300-events []
  (on-event [:midi :control-change] #(handle-event %) [:a300 :control-change])
  (on-event [:midi :note-on] #(handle-event %) [:a300 :note-on])
  (on-event [:midi :note-off] #(handle-event %) [:a300 :note-off]))

(defn remove-a300-event-handlers []
  (remove-handler [:a300 :control-change])
  (remove-handler [:a300 :note-on])
  (remove-handler [:a300 :note-off]))
