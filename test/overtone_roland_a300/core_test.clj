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

(facts
  "about :midi :bend events"

  (fact
    ":midi :bend events are triggered when the bender is bent left or right"
    (record-event [:midi :bend])

    (control-change 6 1 127)
    (control-change 6 1 64)
    (control-change 6 1 0)

    @records => [{:status nil,
      :note 1,
      :timestamp 613092694,
      :velocity 127,
      :data1 1,
      :bending 63,
      :channel 6,
      :command :control-change,
      :velocity-f 1,
      :data2-f 1,
      :bending-f 63/127,
      :data2 127}
     {:status nil,
      :note 1,
      :timestamp 613092694,
      :velocity 64,
      :data1 1,
      :bending 0,
      :channel 6,
      :command :control-change,
      :velocity-f 64/127,
      :data2-f 64/127,
      :bending-f 0,
      :data2 64}
     {:status nil,
      :note 1,
      :timestamp 613092694,
      :velocity 0,
      :data1 1,
      :bending -64,
      :channel 6,
      :command :control-change,
      :velocity-f 0,
      :data2-f 0,
      :bending-f -64/127,
      :data2 0}]))

(facts
  "about :midi :mod events"

  (fact
    ":midi :mod events are triggered when the bender is bent upwards"
    (record-event [:midi :mod])

    (control-change 7 1 0)
    (control-change 7 1 64)
    (control-change 7 1 127)

    @records => [{:status nil,
      :note 1,
      :timestamp 613092694,
      :velocity 0,
      :data1 1,
      :value-f 0,
      :channel 7,
      :command :control-change,
      :velocity-f 0,
      :data2-f 0,
      :data2 0,
      :value 0}
     {:status nil,
      :note 1,
      :timestamp 613092694,
      :velocity 64,
      :data1 1,
      :value-f 64/127,
      :channel 7,
      :command :control-change,
      :velocity-f 64/127,
      :data2-f 64/127,
      :data2 64,
      :value 64}
     {:status nil,
      :note 1,
      :timestamp 613092694,
      :velocity 127,
      :data1 1,
      :value-f 1,
      :channel 7,
      :command :control-change,
      :velocity-f 1,
      :data2-f 1,
      :data2 127,
      :value 127}]))
