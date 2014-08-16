(ns lain.experiments.basic-looping
  (:require [overtone.core :refer :all]
            [mecha.core :as mecha :refer [defmecha]]
            [lain.a300.events]
            [lain.utils :refer [load-note-samples]]
            [lain.play :refer [buf-player]]
            [lain.sequencers :refer [metro]]
            [lain.rp :refer [rp rp-mode]]))



(definst sinst [freq 440]
  (let [sig (sin-osc freq)
        env (env-gen (perc))
        snd (* env sig)
        snd (pan2 snd)]
    snd))


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
           n-beats (click (:beats m) 0.025)
           n-bars (click (:bars m) 0.05)
           t-rp (rp :sync-bus (:bars m))]
          {:rp t-rp}))


(def e (experiment 80 4 4))
(comment (sinst 220))
(comment (sinst 240))
(comment (sinst 280))
(comment (sinst 320))
(comment (rp-mode (:rp e) :rec))
(comment (rp-mode (:rp e) :play))
(comment (mecha/stop e))
