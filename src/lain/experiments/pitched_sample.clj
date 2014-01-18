(ns lain.experiments.pitched-sample
  (:require [overtone.live :refer :all]
            [a300.events :refer [handle-a300-events]]
            [a300.play :refer [midi-key-player
                               remove-key-player]]
            [lain.utils :refer [key-inst
                                deflcgen
                                get-sample]]))

(def !glock-buf (get-sample "glock1/c1.wav"))
(def !base-freq (midi->hz 60))  ; c1

(deflcgen glock :ar
  [freq 440]
  (let
    [scale (/ freq !base-freq)]
    (warp1 
      :num-channels 1
      :bufnum !glock-buf
      :freq-scale scale
      :window-size 0.1
      :overlaps 8
      :window-rand-ratio 0.5
      :interp 4)))

(comment
  (handle-a300-events)
  (def -i-ki (key-inst
               glock
               (adsr
                 :attack 0.0001
                 :decay 0.01
                 :sustain 1
                 :release 2.5
                 :level 2)))
  (def -k (midi-key-player -i-ki))
())
