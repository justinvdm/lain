(ns lain.sequencers-test
  (:require [speclj.core :refer :all]
            [mecha.core :refer [stop]]
            [overtone.sc.synth :refer [defsynth]]
            [overtone.sc.ugens :refer [in:kr
                                       replace-out:kr
                                       pulse-count:kr]]
            [overtone.sc.bus :refer [control-bus
                                     control-bus-set!]]
            [lain.utils :refer [deflcgen]]
            [lain.sequencers :refer :all]))


(deflcgen hit :kr [target 0
                   trig 0]
  (replace-out:kr target (pulse-count:kr (in:kr trig))))


(def bus-a (control-bus))
(defsynth syn-a [trig 0] (hit bus-a))


(def bus-b (control-bus))
(defsynth syn-b [trig 0] (hit bus-b))


(def bus-c (control-bus))
(defsynth syn-c [trig 0] (hit bus-c))


(defsq sq-foo 4 {syn-a [0 1 1 0]
                 syn-b [1 0 0 1]})


(describe "sequencers"
  (with-stubs)

  (after (do (control-bus-set! bus-a 0)
             (control-bus-set! bus-b 0)
             (control-bus-set! bus-c 0)))
  
  (describe "sq"
    (it "should normalize the sq"
      (let [s (sq 4 {syn-a [0 1 1 0]
                     syn-b [1 0 0 1]})]
        (with-redefs [normal-res 8]
          (should= {syn-a [0 0 1 0 1 0 0 0]
                    syn-b [1 0 0 0 0 0 1 0]} (s))))))

  (describe "sq+"
    (it "should construct a new sq that adds the given sq"
      (let [s1 (sq 4 {syn-a [0 1 0 1]
                      syn-b [1 0 0 1]})
            s2 (sq 4 {syn-a [0 1 1 0]
                      syn-c [1 0 1 1]})
            s3 (sq 8 {syn-b [1 0 0 1 1 0 0 1]
                      syn-a [1 0 1 0 1 0 1 0]})]
        (should=
          ((sq 8 {syn-a [0 0 1 0 0 0 1 0 0 0 1 0 1 0 0 0 1 0 1 0 1 0 1 0]
                  syn-b [1 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0 0 1 1 0 0 1]
                  syn-c [0 0 0 0 0 0 0 0 1 0 0 0 1 0 1 0 0 0 0 0 0 0 0 0]}))
          ((sq+ s1 s2 s3))))))

  (describe "defsq"
    (it "should define an sq"
      (should= (type sq-foo) :lain.sequencers/sq)))

  (describe "sequencer"
    (it "should send beat triggers for each track")
    (it "should allow a vector of sq to be given")

    (describe "when stopped"
      (it "should free its index bus")
      (it "should kill its index synth node")
      (it "should kill its track trigger nodes")
      (it "should free its track trigger busses")
      (it "should free its track buffers"))))
