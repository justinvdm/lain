(ns overtone-roland-a300.events-test
  (:require [speclj.core :refer :all]
            [overtone.libs.event :refer [event
                                         on-event
                                         remove-handler]]
            [overtone-roland-a300.events :refer :all]
            [overtone-roland-a300.test-utils :refer :all]))

(def records (atom []))

(defn record-event
  [event-type]
  (on-event event-type #(swap! records conj %) ::test))

(defn clear-records []
  (reset! records []))

(describe
  "a300 events"

  (before
     [(handle-a300-events)])

  (after
    [(clear-records)
     (remove-handler ::test)
     (remove-a300-event-handlers)])

  (describe
    "keyboard events"
    (it
      "should trigger :key :down events on key presses"
      (record-event [:midi :key :down])

      (note-on 0 1 127)
      (note-on 0 60 64)

      (should= @records [{:status nil
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
                    :data2 64}]))

    (it
      "should trigger midi :key :up events on key releases"
      (record-event [:midi :key :up])

      (note-off 0 1)
      (note-off 0 60)

      (should= @records [{:status nil
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
                    :data2 0}])))

  (describe
    "bender events"
    (it
      "should trigger midi :bend events when the bender is bent left or right"
      (record-event [:midi :bend])

      (control-change 6 1 127)
      (control-change 6 1 64)
      (control-change 6 1 0)

      (should= @records [{:status nil,
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

    (it
      "should trigger midi :mod events when the bender is bent upwards"
        (record-event [:midi :mod])

        (control-change 7 1 0)
        (control-change 7 1 64)
        (control-change 7 1 127)

        (should= @records [{:status nil,
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
                      :value 127}])))

  (describe
    "pad events"
    (it
      "should trigger midi :pad :down events when a pad is pressed"
      (record-event [:midi :pad :down])

      (note-on 5 1 32)
      (note-on 5 1 64)
      (note-on 5 1 127)

      (should= @records [{:status nil,
                    :note 1,
                    :timestamp 613092694,
                    :velocity 32,
                    :data1 1,
                    :channel 5,
                    :command :note-on,
                    :velocity-f 32/127,
                    :data2-f 32/127,
                    :data2 32}
                   {:status nil,
                    :note 1,
                    :timestamp 613092694,
                    :velocity 64,
                    :data1 1,
                    :channel 5,
                    :command :note-on,
                    :velocity-f 64/127,
                    :data2-f 64/127,
                    :data2 64}
                   {:status nil,
                    :note 1,
                    :timestamp 613092694,
                    :velocity 127,
                    :data1 1,
                    :channel 5,
                    :command :note-on,
                    :velocity-f 1,
                    :data2-f 1,
                    :data2 127}]))

    (it
      "should trigger :midi :pad<n> :down events when pad n is pressed"
      (record-event [:midi :pad1 :down])
      (note-on 5 1 127)

      (record-event [:midi :pad2 :down])
      (note-on 5 2 127)

      (record-event [:midi :pad3 :down])
      (note-on 5 3 127)

      (should= @records [{:status nil,
                    :note 1,
                    :timestamp 613092694,
                    :velocity 127,
                    :data1 1,
                    :channel 5,
                    :command :note-on,
                    :velocity-f 1,
                    :data2-f 1,
                    :data2 127}
                   {:status nil,
                    :note 2,
                    :timestamp 613092694,
                    :velocity 127,
                    :data1 2,
                    :channel 5,
                    :command :note-on,
                    :velocity-f 1,
                    :data2-f 1,
                    :data2 127}
                   {:status nil,
                    :note 3,
                    :timestamp 613092694,
                    :velocity 127,
                    :data1 3,
                    :channel 5,
                    :command :note-on,
                    :velocity-f 1,
                    :data2-f 1,
                    :data2 127}]))

    (it
      "should trigger :midi :pad :up events when a pad is released"
      (record-event [:midi :pad :up])

      (note-off 5 1)
      (note-off 5 2)
      (note-off 5 3)

      (should= @records [{:status nil,
                    :note 1,
                    :timestamp 613092694,
                    :velocity 0,
                    :data1 1,
                    :channel 5,
                    :command :note-off,
                    :velocity-f 0,
                    :data2-f 0,
                    :data2 0}
                   {:status nil,
                    :note 2,
                    :timestamp 613092694,
                    :velocity 0,
                    :data1 2,
                    :channel 5,
                    :command :note-off,
                    :velocity-f 0,
                    :data2-f 0,
                    :data2 0}
                   {:status nil,
                    :note 3,
                    :timestamp 613092694,
                    :velocity 0,
                    :data1 3,
                    :channel 5,
                    :command :note-off,
                    :velocity-f 0,
                    :data2-f 0,
                    :data2 0}]))

    (it
      "should trigger :midi :pad<n> :up events when pad n is released"
      (record-event [:midi :pad1 :up])
      (note-off 5 1)

      (record-event [:midi :pad2 :up])
      (note-off 5 2)

      (record-event [:midi :pad3 :up])
      (note-off 5 3)

      (should= @records [{:status nil,
                    :note 1,
                    :timestamp 613092694,
                    :velocity 0,
                    :data1 1,
                    :channel 5,
                    :command :note-off,
                    :velocity-f 0,
                    :data2-f 0,
                    :data2 0}
                   {:status nil,
                    :note 2,
                    :timestamp 613092694,
                    :velocity 0,
                    :data1 2,
                    :channel 5,
                    :command :note-off,
                    :velocity-f 0,
                    :data2-f 0,
                    :data2 0}
                   {:status nil,
                    :note 3,
                    :timestamp 613092694,
                    :velocity 0,
                    :data1 3,
                    :channel 5,
                    :command :note-off,
                    :velocity-f 0,
                    :data2-f 0,
                    :data2 0}])))

  (describe
    "events for buttons b1-b4"
    (it
      "should trigger :midi :b<n> :on events when button bn is enabled"
      (record-event [:midi :b1 :on])
      (control-change 1 1 127)

      (record-event [:midi :b2 :on])
      (control-change 1 2 127)

      (record-event [:midi :b3 :on])
      (control-change 1 3 127)

      (should= @records [{:status nil,
                    :note 1,
                    :timestamp 613092694,
                    :velocity 127,
                    :data1 1,
                    :channel 1,
                    :command :control-change,
                    :velocity-f 1,
                    :data2-f 1,
                    :data2 127}
                   {:status nil,
                    :note 2,
                    :timestamp 613092694,
                    :velocity 127,
                    :data1 2,
                    :channel 1,
                    :command :control-change,
                    :velocity-f 1,
                    :data2-f 1,
                    :data2 127}
                   {:status nil,
                    :note 3,
                    :timestamp 613092694,
                    :velocity 127,
                    :data1 3,
                    :channel 1,
                    :command :control-change,
                    :velocity-f 1,
                    :data2-f 1,
                    :data2 127}]))

    (it
      "should trigger :midi :b<n> :off events when button bn is disabled"
      (record-event [:midi :b1 :off])
      (control-change 1 1 0)

      (record-event [:midi :b2 :off])
      (control-change 1 2 0)

      (record-event [:midi :b3 :off])
      (control-change 1 3 0)

      (should= @records [{:status nil,
                    :note 1,
                    :timestamp 613092694,
                    :velocity 0,
                    :data1 1,
                    :channel 1,
                    :command :control-change,
                    :velocity-f 0,
                    :data2-f 0,
                    :data2 0}
                   {:status nil,
                    :note 2,
                    :timestamp 613092694,
                    :velocity 0,
                    :data1 2,
                    :channel 1,
                    :command :control-change,
                    :velocity-f 0,
                    :data2-f 0,
                    :data2 0}
                   {:status nil,
                    :note 3,
                    :timestamp 613092694,
                    :velocity 0,
                    :data1 3,
                    :channel 1,
                    :command :control-change,
                    :velocity-f 0,
                    :data2-f 0,
                    :data2 0}])))

  (describe
    "events for buttons l1-b9"
    (it
      "should trigger :midi :l<n> :on events when button bn is enabled"
      (record-event [:midi :l1 :on])
      (control-change 2 1 127)

      (record-event [:midi :l2 :on])
      (control-change 2 2 127)

      (record-event [:midi :l3 :on])
      (control-change 2 3 127)

      (should= @records [{:status nil,
                    :note 1,
                    :timestamp 613092694,
                    :velocity 127,
                    :data1 1,
                    :channel 2,
                    :command :control-change,
                    :velocity-f 1,
                    :data2-f 1,
                    :data2 127}
                   {:status nil,
                    :note 2,
                    :timestamp 613092694,
                    :velocity 127,
                    :data1 2,
                    :channel 2,
                    :command :control-change,
                    :velocity-f 1,
                    :data2-f 1,
                    :data2 127}
                   {:status nil,
                    :note 3,
                    :timestamp 613092694,
                    :velocity 127,
                    :data1 3,
                    :channel 2,
                    :command :control-change,
                    :velocity-f 1,
                    :data2-f 1,
                    :data2 127}]))

    (it
      "should trigger :midi :l<n> :off events when button bn is disabled"
      (record-event [:midi :l1 :off])
      (control-change 2 1 0)

      (record-event [:midi :l2 :off])
      (control-change 2 2 0)

      (record-event [:midi :l3 :off])
      (control-change 2 3 0)

      (should= @records [{:status nil,
                    :note 1,
                    :timestamp 613092694,
                    :velocity 0,
                    :data1 1,
                    :channel 2,
                    :command :control-change,
                    :velocity-f 0,
                    :data2-f 0,
                    :data2 0}
                   {:status nil,
                    :note 2,
                    :timestamp 613092694,
                    :velocity 0,
                    :data1 2,
                    :channel 2,
                    :command :control-change,
                    :velocity-f 0,
                    :data2-f 0,
                    :data2 0}
                   {:status nil,
                    :note 3,
                    :timestamp 613092694,
                    :velocity 0,
                    :data1 3,
                    :channel 2,
                    :command :control-change,
                    :velocity-f 0,
                    :data2-f 0,
                    :data2 0}])))

  (describe
    "events for buttons r1-r9"
    (it
      "that :midi :r<n> events are triggered when knob rn is turned"
      (record-event [:midi :r1])
      (control-change 3 1 32)

      (record-event [:midi :r2])
      (control-change 3 2 64)

      (record-event [:midi :r3])
      (control-change 3 3 127)

      (should= @records [{:status nil,
                    :note 1,
                    :timestamp 613092694,
                    :velocity 32,
                    :data1 1,
                    :value-f 32/127,
                    :channel 3,
                    :command :control-change,
                    :velocity-f 32/127,
                    :data2-f 32/127,
                    :data2 32,
                    :value 32}
                   {:status nil,
                    :note 2,
                    :timestamp 613092694,
                    :velocity 64,
                    :data1 2,
                    :value-f 64/127,
                    :channel 3,
                    :command :control-change,
                    :velocity-f 64/127,
                    :data2-f 64/127,
                    :data2 64,
                    :value 64}
                   {:status nil,
                    :note 3,
                    :timestamp 613092694,
                    :velocity 127,
                    :data1 3,
                    :value-f 1,
                    :channel 3,
                    :command :control-change,
                    :velocity-f 1,
                    :data2-f 1,
                    :data2 127,
                    :value 127}])))

  (describe
    "events for sliders s1-s9"
    (it
      "should trigger :midi :s<n> events when slider sn is slided"
      (record-event [:midi :s1])
      (control-change 4 1 32)

      (record-event [:midi :s2])
      (control-change 4 2 64)

      (record-event [:midi :s3])
      (control-change 4 3 127)

      (should= @records [{:status nil,
                    :note 1,
                    :timestamp 613092694,
                    :velocity 32,
                    :data1 1,
                    :value-f 32/127,
                    :channel 4,
                    :command :control-change,
                    :velocity-f 32/127,
                    :data2-f 32/127,
                    :data2 32,
                    :value 32}
                   {:status nil,
                    :note 2,
                    :timestamp 613092694,
                    :velocity 64,
                    :data1 2,
                    :value-f 64/127,
                    :channel 4,
                    :command :control-change,
                    :velocity-f 64/127,
                    :data2-f 64/127,
                    :data2 64,
                    :value 64}
                   {:status nil,
                    :note 3,
                    :timestamp 613092694,
                    :velocity 127,
                    :data1 3,
                    :value-f 1,
                    :channel 4,
                    :command :control-change,
                    :velocity-f 1,
                    :data2-f 1,
                    :data2 127,
                    :value 127}]))))
