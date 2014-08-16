(ns lain.experiments.switching
  (:require [clojure.string :refer [split]]
            [overtone.core :refer :all]
            [mecha.core :as mecha :refer [defmecha]]
            [lain.a300.events]
            [lain.play :refer [ctl-player
                               buf-player]]
            [lain.control :refer [switch-controller
                                  player-param-controller]]
            [lain.utils :refer [load-note-samples ascii]]))


(def !bufs {:saw (load-note-samples "samples/saw1/*.wav")
            :square (load-note-samples "samples/square1/*.wav")})


(defsynth ssynth
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
    (out bus snd)))


(defmecha saw-mode
  (:start [p (buf-player ssynth (:saw !bufs) :device-name "APRO [hw:2,0,1]")]
          (ascii "saw")))


(defmecha square-mode
  (:start [p (buf-player ssynth (:square !bufs) :device-name "APRO [hw:2,0,1]")]
          (ascii "square")))


(defmecha experiment
  (:start [c-modes (switch-controller [:midi :r9] {0 saw-mode
                                                   1 square-mode})]))

(def e (experiment))
(comment (mecha/stop e))
