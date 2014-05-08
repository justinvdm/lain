(ns lain.sequencers-test
  (:require [speclj.core :refer :all]
            [mecha.core :as mecha]
            [overtone.libs.event :refer [sync-event]]
            [overtone.sc.node :refer :all]
            [overtone.sc.synth :refer :all]
            [overtone.sc.ugens :refer :all]
            [overtone.sc.bus :refer :all]
            [lain.test-init]
            [lain.utils :refer [deflcgen]]
            [lain.sequencers :refer :all]))


(deflcgen hit :kr [target 0
                   trig 0]
  (replace-out:kr target (pulse-count:kr (in:kr trig))))


(def bus-a (control-bus))
(defsynth syn-a [trig 0] (hit bus-a trig))


(def bus-b (control-bus))
(defsynth syn-b [trig 0] (hit bus-b trig))


(def bus-c (control-bus))
(defsynth syn-c [trig 0] (hit bus-c trig))


(defsq sq-foo 4 {syn-a [0 1 1 0]
                 syn-b [1 0 0 1]})


(describe "sequencers"
  (with-stubs)

  (after (do (sync-event :reset)
             (control-bus-set! bus-a 0)
             (control-bus-set! bus-b 0)
             (control-bus-set! bus-c 0)))

  (describe "metro"
    (it "should send step pulses"
      (let [m (metro :bpm (/ (* 120 4) 32)
                     :bpb 4
                     :res 4)
            n ((synth (replace-out:kr bus-a (pulse-count:kr (in:kr (:steps m))))))]

        (should= [0.0] (control-bus-get bus-a))

        (Thread/sleep 125)
        (should= [1.0] (control-bus-get bus-a))

        (Thread/sleep 125)
        (should= [2.0] (control-bus-get bus-a))

        (Thread/sleep 125)
        (should= [3.0] (control-bus-get bus-a))

        (Thread/sleep 125)
        (should= [4.0] (control-bus-get bus-a))

        (Thread/sleep 125)
        (should= [5.0] (control-bus-get bus-a))

        (mecha/stop m)))

    (it "should keep track of the step index"
      (let [m (metro :bpm (/ (* 120 4) 32)
                     :bpb 4
                     :res 4)
            n ((synth (replace-out:kr bus-a (in:kr (:step-idx m)))))]

        (should= [0.0] (control-bus-get bus-a))

        (Thread/sleep 10)
        (should= [1.0] (control-bus-get bus-a))

        (Thread/sleep 125)
        (should= [2.0] (control-bus-get bus-a))

        (Thread/sleep 125)
        (should= [3.0] (control-bus-get bus-a))

        (Thread/sleep 125)
        (should= [4.0] (control-bus-get bus-a))

        (Thread/sleep 125)
        (should= [5.0] (control-bus-get bus-a))

        (Thread/sleep 125)
        (should= [6.0] (control-bus-get bus-a))

        (mecha/stop m)))

    (it "should send beat pulses"
      (let [m (metro :bpm (* 120 10)
                     :bpb 4
                     :res 4)
            n ((synth (replace-out:kr bus-a (pulse-count:kr (in:kr (:beats m))))))]

        (should= [0.0] (control-bus-get bus-a))

        (Thread/sleep 50)
        (should= [1.0] (control-bus-get bus-a))

        (Thread/sleep 50)
        (should= [2.0] (control-bus-get bus-a))

        (Thread/sleep 50)
        (should= [3.0] (control-bus-get bus-a))

        (Thread/sleep 50)
        (should= [4.0] (control-bus-get bus-a))

        (Thread/sleep 50)
        (should= [5.0] (control-bus-get bus-a))

        (mecha/stop m)))

    (it "should send bar pulses"
      (let [m (metro :bpm (* 120 20)
                     :bpb 4
                     :res 4)
            n ((synth (replace-out:kr bus-a (pulse-count:kr (in:kr (:bars m))))))]

        (should= [0.0] (control-bus-get bus-a))

        (Thread/sleep 100)
        (should= [1.0] (control-bus-get bus-a))

        (Thread/sleep 100)
        (should= [2.0] (control-bus-get bus-a))

        (Thread/sleep 100)
        (should= [3.0] (control-bus-get bus-a))

        (Thread/sleep 100)
        (should= [4.0] (control-bus-get bus-a))

        (Thread/sleep 100)
        (should= [5.0] (control-bus-get bus-a))

        (mecha/stop m)))

    (describe "when the metronome is stopped"
      (it "should kill its synth node"
        (let [m (metro)]
          (should-invoke
            kill
            {:with [(:node m)]
             :times 1}
            (mecha/stop m))))))
  
  (describe "sq"
    (it "should normalize the sq"
      (let [s (sq 4 {syn-a [0 1 1 0]
                     syn-b [1 0 0 1]})]
        (with-redefs [normal-res 8]
          (should= {syn-a [0 0 1 0 1 0 0 0]
                    syn-b [1 0 0 0 0 0 1 0]} (s)))))

    (it "should allow nested sqs"
      (should=
        ((sq {syn-a [0 1 1 0]
              syn-b [1 0 0 1]}))
        ((sq (sq {syn-a [0 1 1 0]
                  syn-b [1 0 0 1]})))))

    (it "should allow sequential collections of sqs"
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
          ((sq [s1 s2 s3]))))))

  (describe "defsq"
    (it "should define an sq"
      (should= (type sq-foo) :lain.sequencers/sq)))

  (describe "sequencer"
    (it "should send beat triggers for each track"
      (let [s (sq 4 {syn-a [0 1 1 0]
                     syn-b [1 0 0 1]})
            m (metro :bpm (* 120 2))
            sr (sequencer s :metronome m)]

        (should= [0.0] (control-bus-get bus-a))
        (should= [0.0] (control-bus-get bus-b))

        (Thread/sleep 250)
        (should= [1.0] (control-bus-get bus-a))
        (should= [0.0] (control-bus-get bus-b))

        (Thread/sleep 250)
        (should= [2.0] (control-bus-get bus-a))
        (should= [0.0] (control-bus-get bus-b))

        (Thread/sleep 250)
        (should= [2.0] (control-bus-get bus-a))
        (should= [1.0] (control-bus-get bus-b))

        (Thread/sleep 250)
        (should= [2.0] (control-bus-get bus-a))
        (should= [2.0] (control-bus-get bus-b))

        (Thread/sleep 250)
        (should= [3.0] (control-bus-get bus-a))
        (should= [2.0] (control-bus-get bus-b))

        (Thread/sleep 250)
        (should= [4.0] (control-bus-get bus-a))
        (should= [2.0] (control-bus-get bus-b))

        (Thread/sleep 250)
        (should= [4.0] (control-bus-get bus-a))
        (should= [3.0] (control-bus-get bus-b))

        (mecha/stop sr)))))
