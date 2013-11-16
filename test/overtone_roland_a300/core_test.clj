(ns overtone-roland-a300.core-test
  (:require [midje.sweet :refer :all]
            [overtone.libs.event :refer [on-event]]
            [overtone-roland-a300.core :refer :all]
            [overtone-roland-a300.fake-midi :refer :all]))

(facts "about :midi :key events"
  (fact ":midi :key :down events are only triggered by the keyboard keys"
    0 => 1))
