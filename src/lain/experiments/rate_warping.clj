(ns lain.experiments.rate-warping
  (:require [clojure.string :refer [split]]
            [overtone.core :refer :all]
            [lain.a300.events :refer [handle-a300-events]]
            [lain.a300.play :refer [players
                                    buf-player
                                    remove-all-players]]
            [lain.a300.control :refer [param-controller
                                       player-param-controller
                                       remove-all-controllers]]
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


(do
  (handle-a300-events)
  (def p (buf-player sinst
                     !square-bufs
                     :device-name "VirMIDI [hw:0,0,0]"
                     :down-event [:midi :key :down]
                     :up-event [:midi :key :up]))
  (doseq [[param event-type extent]
          [[:warp-depth [:midi :r1] [0.01 0.1]]
           [:warp-freq [:midi :r2] [0 4]]]]
    (player-param-controller p param event-type :extent extent))
  ())

(comment
  (remove-all-players)
  (remove-all-controllers)
  ())
