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
            [lain.utils :refer :all]))


(def c (control-bus))


(describe "utils"
  (with-stubs)

  (after (control-bus-set! c 0))

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
           61 {:name "61.wav"}}))))

  (describe "metro"
    (it "should send beat pulses"
      (let [m (metro :bpm (* 120 10)
                     :bpb 4
                     :res 4)
            n ((synth (replace-out:kr c (pulse-count:kr (in:kr (:beats m))))))]

        (should= (control-bus-get c) [0.0])

        (Thread/sleep 50)
        (should= (control-bus-get c) [1.0])

        (Thread/sleep 50)
        (should= (control-bus-get c) [2.0])

        (Thread/sleep 50)
        (should= (control-bus-get c) [3.0])

        (Thread/sleep 50)
        (should= (control-bus-get c) [4.0])

        (Thread/sleep 50)
        (should= (control-bus-get c) [5.0])

        (mecha/stop m)
        (kill n)))

    (it "should send bar pulses"
      (let [m (metro :bpm (* 120 20)
                     :bpb 4
                     :res 4)
            n ((synth (replace-out:kr c (pulse-count:kr (in:kr (:bars m))))))]

        (should= (control-bus-get c) [0.0])

        (Thread/sleep 100)
        (should= (control-bus-get c) [1.0])

        (Thread/sleep 100)
        (should= (control-bus-get c) [2.0])

        (Thread/sleep 100)
        (should= (control-bus-get c) [3.0])

        (Thread/sleep 100)
        (should= (control-bus-get c) [4.0])

        (Thread/sleep 100)
        (should= (control-bus-get c) [5.0])

        (mecha/stop m)))

    (describe "when the metronome is stopped"
      (it "should kill its synth node"
        (let [m (metro)]
          (should-invoke
            kill
            {:with [(:node m)]
             :times 1}
            (mecha/stop m)))))))
