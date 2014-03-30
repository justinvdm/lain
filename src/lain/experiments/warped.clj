(ns lain.experiments.warped
  (:require [overtone.core :refer :all]
            [lain.a300.events :refer [handle-a300-events]]
            [lain.a300.play :refer [key-player
                                    remove-all-players]]
            [lain.insts :refer [key-inst]]
            [lain.utils :refer [deflcgen]]))

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

(def warpy
  (key-inst wave (adsr 0.0001 0.01 1 2.5)))

(comment
  (handle-a300-events)
  (warp)
  (key-player warpy :device-name "VirMIDI [default]")
  ())

(comment
  (remove-all-players)
  ())
