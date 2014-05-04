(ns lain.sequencers-test
  (:require [speclj.core :refer :all]
            [mecha.core :refer [stop]]
            [lain.sequencers :refer :all]))


(describe "sequencers"
  (with-stubs)
  
  (describe "sq"
    (it "should should normalize its steps when called")
    (it "should allow the resultion to be optional"))

  (describe "sq+"
    (it "should construct a new sq that adds the given sq when called"))

  (describe "defsq"
    (it "should define an sq"))

  (describe "sequencer"
    (it "should allow a metronome to be optional")
    (it "should a vector of sq to be given")
    (it "should an sq to be given")
    (it "should send beat triggers for each track")

    (describe "when stopped"
      (it "should free its index bus")
      (it "should kill its index synth node")
      (it "should kill its track trigger nodes")
      (it "should free its track trigger busses")
      (it "should free its track buffers"))))
