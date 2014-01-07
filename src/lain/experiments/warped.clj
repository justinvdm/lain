(ns lain.experiments.warped
  (:require [overtone.live :refer :all]
            [a300.events :refer [handle-a300-events]]
            [a300.play :refer [midi-key-player
                               remove-key-player]]
            [lain.utils :refer [key-inst]]))

(defsynth note-warp
  [freq 2
   extent 1]
  (out
    -b-warp
    (range-lin (sin-osc freq) (- extent) extent)))

(comment
  (handle-a300-events)

  (def -b-warp (audio-bus))
  (def -b-waves-num (control-bus))
  (def -b-waves-diff (control-bus))

  (control-bus-set! -b-waves-num 3)
  (control-bus-set! -b-waves-diff 1)

  (def -s-note-warp (-s-note-warp))
  (def -i-ki (key-inst saw (adsr 0.0001 0.01 1 2.5)))
  (def -k (midi-key-player -i-ki))
())
