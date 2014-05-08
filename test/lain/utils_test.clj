(ns lain.utils-test
  (:require [speclj.core :refer :all]
            [mecha.core :as mecha]
            [overtone.sc.node :refer [kill]]
            [overtone.sc.synth :refer [synth]]
            [overtone.sc.sample :refer [load-samples]]
            [overtone.sc.ugens :refer [in:kr
                                       replace-out:kr
                                       pulse-count:kr]]
            [overtone.sc.bus :refer [free-bus
                                     control-bus
                                     control-bus-get
                                     control-bus-set!]]
            [lain.test-init]
            [lain.utils :refer :all]))


(describe "utils"
  (with-stubs)

  (describe "lin-interpolator"
      (it "should make a linear interpolator"
        (let
          [lerp (lin-interpolator [2 10] [12 16])]
          (should= (lerp 2) 12)
          (should= (lerp 10) 16)
          (should= (lerp 4) 13)
          (should= (lerp 6) 14))))

  (describe "load-note-samples"
    (it "should load the samples keyed by midi notes"
      (with-redefs [load-samples
                    (stub :load-samples {:return [{:name "c1.wav"}
                                                  {:name "c2.wav"}]})]
        (should= (load-note-samples "c1.wav" "c2.wav")
          {24 {:name "c1.wav"}
           36 {:name "c2.wav"}}))

        (should-have-invoked :load-samples {:times 1
                                            :with ["c1.wav" "c2.wav"]}))

    (it "should support files named by midi notes"
      (with-redefs [load-samples
                    (stub :load-samples {:return [{:name "60.wav"}
                                                  {:name "61.wav"}]})]
        (should= (load-note-samples "c1.wav" "c2.wav")
          {60 {:name "60.wav"}
           61 {:name "61.wav"}})))))
