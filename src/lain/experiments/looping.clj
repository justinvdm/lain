(ns lain.experiments.looping
  (:require [clojure.string :refer [split]]
            [overtone.core :refer :all]
            [mecha.core :as mecha :refer [defmecha]]
            [lain.a300.events]
            [lain.play :refer [buf-player]]
            [lain.control :refer [switch-controller]]
            [lain.sequencers :refer [metro]]
            [lain.rp :refer [a300-looper rp-mode]]
            [lain.utils :refer [load-note-samples ascii]]))


(def !bufs {:saw (load-note-samples "samples/saw1/*.wav")
            :square (load-note-samples "samples/square1/*.wav")})


(definst sinst
  [buf 0
   bus 0
   a 0.01
   d 0.3
   s 1
   r 2
   gate 1]
  (let [sig (scaled-play-buf 1 buf)
        env (env-gen (adsr a d s r) gate :action FREE)
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


(defmecha saw-mode
  (:start [p (buf-player sinst (:saw !bufs) :device-name "APRO [hw:2,0,1]")]
          (ascii "saw")))


(defmecha square-mode
  (:start [p (buf-player sinst (:square !bufs) :device-name "APRO [hw:2,0,1]")]
          (ascii "square")))


(defmecha experiment
  (:start [m (metro :bpm 80)
           c-modes (switch-controller [:midi :r9] {0 saw-mode
                                                   1 square-mode})
           n-beats (click (:beats m) 0.025)
           n-bars (click (:bars m) 0.05)

          loopers (doall (for [e [[:midi :l1]
                                  [:midi :l2]
                                  [:midi :l3]
                                  [:midi :l4]
                                  [:midi :l5]
                                  [:midi :l6]
                                  [:midi :l7]
                                  [:midi :l8]]]
                           (a300-looper
                             (conj e :on)
                             (conj e :off)
                             :sync-bus (:bars m))))]))


(comment
  (def e (experiment))
  (mecha/stop e))
