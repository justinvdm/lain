(ns lain.experiments.rate-warping
  (:require [clojure.string :refer [split]]
            [overtone.core :refer :all]
            [mecha.core :as mecha :refer [defmecha]]
            [lain.a300]
            [lain.play :refer [buf-player]]
            [lain.control :refer [param-controller
                                  player-param-controller]]
            [lain.utils :refer [load-note-samples]]))


(def !square-bufs (load-note-samples "samples/square1/*.wav"))


(definst sinst
  [buf 0
   a 0.01
   d 0.3
   s 1
   r 1
   gate 1
   warp-depth 0.035
   warp-freq 0.125]
  (let [warp-offset (sin-osc warp-freq)
        warp-offset (* warp-depth warp-offset)
        rate (- 1 warp-offset)
        sig (scaled-play-buf 1 buf rate)
        env (env-gen (adsr a d s r) gate :action FREE)
        snd (* env sig)
        snd (pan2 snd)]
    snd))


(defmecha experiment
  (:start
    [p (buf-player sinst
                   !square-bufs
                   :device-name "APRO [hw:2,0,1]")

     c-warp-depth (player-param-controller [:midi :r1]
                                           p
                                           :warp-depth
                                           :extent [0.01 0.1])

     c-warp-freq (player-param-controller [:midi :r2]
                                          p
                                          :warp-depth
                                          :extent [0 0.4])]))


(def e (experiment))
(comment (mecha/stop e))
