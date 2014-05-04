(ns lain.experiments.metronome
  (:require [overtone.core :refer :all]
            [mecha.core :as mecha :refer [defmecha]]
            [lain.sequencers :refer [metro]]))


(definst click [bus 0
                amp 0.1]
  (let [trig (in:kr bus)
        env (env-gen (perc) :gate trig)
        sig (brown-noise)
        sig (* amp env sig)]
    sig))


(defmecha experiment [bpm bpb res]
  (:start [m (metro :bpm bpm
                    :bpb bpb
                    :res res)
           n-beats (click (:beats m))
           n-bars (click (:bars m) 0.8)])
  (:stop (kill n-beats)
         (kill n-bars)))


(def e (experiment 120 4 4))
(comment (mecha/stop e))
