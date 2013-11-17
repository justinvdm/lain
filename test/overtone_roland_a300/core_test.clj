(ns overtone-roland-a300.core-test
  (:require [midje.sweet :refer :all]
            [overtone.libs.event :refer [event
                                         on-event
                                         remove-handler]]
            [overtone-roland-a300.core :refer :all]
            [overtone-roland-a300.test-utils :refer :all]))

(def records (atom []))

(defn record-event
  [event-type]
  (on-event event-type #(swap! records conj %) ::test))

(defn clear-records
  []
  (reset! records []))

(background (after :facts [(clear-records)
                           (remove-handler ::test)]))

(facts
  "about :midi :key events"
  (fact
    ":midi :key :down events are triggered on key presses"
    (record-event [:midi :key :down])

    (note-on 0 1 127)
    (note-on 0 60 64)

    (Thread/sleep 100)

    @records => [{:status nil
                  :note 1
                  :timestamp 613092694
                  :velocity 127
                  :data1 1
                  :channel 0
                  :command :note-on
                  :velocity-f 1
                  :data2-f 1
                  :data2 127}
                 {:status nil
                  :note 60
                  :timestamp 613092694
                  :velocity 64
                  :data1 60
                  :channel 0
                  :command :note-on
                  :velocity-f 64/127
                  :data2-f 64/127
                  :data2 64}])

  (fact
    ":midi :key :up events are triggered on key releases"
    (record-event [:midi :key :up])

    (note-off 0 1)
    (note-off 0 60)

    (Thread/sleep 100)

    @records => [{:status nil
                  :note 1
                  :timestamp 613092694
                  :velocity 0
                  :data1 1
                  :channel 0
                  :command :note-off
                  :velocity-f 0
                  :data2-f 0 
                  :data2 0}
                 {:status nil
                  :note 60
                  :timestamp 613092694
                  :velocity 0
                  :data1 60
                  :channel 0
                  :command :note-off
                  :velocity-f 0
                  :data2-f 0
                  :data2 0}]))
