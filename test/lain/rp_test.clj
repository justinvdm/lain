(ns lain.rp-test
  (:require [speclj.core :refer :all]
            [mecha.core :as mecha]))


(describe "rp"
  (with-stubs)

  (describe "rp"
    (it "should support recording")
    (it "should support playing")
    (it "should support looped playing"))

  (describe "modal-rp"
    (describe "when the down key is pressed in recording mode"
      (it "should start recording"))

    (describe "when the mode key is released in recording mode"
      (it "should start playing"))

    (describe "when the up key is pressed in play mode"
      (it "should stop playing")))

    (describe "when the down key is pressed in play mode"
      (it "should start playing")))
