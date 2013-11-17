(ns overtone-roland-a300.test-utils
  (:require [overtone.libs.event :refer [event]]))

(defn fake-event
  [event-type
   channel
   note
   velocity]
  (let [velocity-f (/ velocity 127)]
    (event
      [:midi event-type]
      {:status nil
       :note note,
       :timestamp 613092694
       :velocity velocity
       :data1 note
       :channel channel
       :command event-type
       :velocity-f velocity-f
       :data2-f velocity-f
       :data2 velocity})
    (Thread/sleep 50)))

(defn control-change [channel note velocity]
  (fake-event :control-change channel note velocity))

(defn note-on [channel note velocity]
  (fake-event :note-on channel note velocity))

(defn note-off [channel note]
  (fake-event :note-off channel note 0))
