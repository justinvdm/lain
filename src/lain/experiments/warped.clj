(ns lain.experiments.warped
  (:require [overtone.live :refer :all]
            [a300.events :refer [handle-a300-events]]
            [a300.play :refer [midi-key-player
                               remove-key-player]]
            [lain.utils :refer [key-inst
                                deflcgen]]))

(def !warp-bus (audio-bus))

(definst warp
  [freq 0.5 
   extent 4]
  (out:ar
    !warp-bus
    (range-lin (sin-osc freq) (- extent) extent)))

(deflcgen wave :ar
  [freq 440]
  (let
    [offset (in:ar !warp-bus)
     freq (+ freq offset)]
    (square freq)))

(comment
  (handle-a300-events)
  (def !warp (warp))
  (def -i-ki (key-inst wave (adsr 0.0001 0.01 1 2.5)))
  (def -k (midi-key-player -i-ki))
())
