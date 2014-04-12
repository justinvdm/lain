(ns lain.experiments.glock
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


(comment
  (handle-a300-events)
  (def p (buf-player glock
                     !glock-bufs
                     :device-name "VirMIDI [default]"
                     :down-event [:midi :key :down]
                     :up-event [:midi :key :up]))
  (def d (delays))
  (doseq [[param event-type extent]
          [[:delay-time [:midi :r1] [0 1]]
           [:atten [:midi :r2] [0 1]]]]
    (param-controller d param event-type :extent extent))
  ())

(comment
  (remove-all-players)
  (remove-all-controllers)
  ())
