(ns lain.experiments.glock
  (:require [clojure.string :refer [split]]
            [overtone.core :refer :all]
            [mecha.core :as mecha :refer [defmecha]]
            [lain.a300.events]
            [lain.play :refer [buf-player]]
            [lain.control :refer [param-controller
                                  player-param-controller]]
            [lain.utils :refer [load-note-samples]]))


(def !glock-bufs (load-note-samples "samples/glock2/*.wav"))


(definst glock
  [buf 0
   a 0.01
   r 3]
  (let [sig (scaled-play-buf 1 buf)
        env (env-gen (perc))
        snd (* env sig 0.1)
        snd (pan2 snd)]
    snd))


(defsynth delays
  [bus 0
   delay-time 1
   delay-time-max 2
   atten 0.3]
  (let [del (delay-c (local-in) delay-time-max delay-time)
        del (* atten del)
        sig (in bus)
        sig (+ sig del)]
    (local-out sig)
    (out bus (pan2 sig))))


(defmecha experiment
  (:start [p (buf-player glock
                         !glock-bufs
                         :device-name "APRO [hw:2,0,1]")
           d (delays)])
  (:stop (kill d)))


(def e (experiment))
(comment (mecha/stop e))
