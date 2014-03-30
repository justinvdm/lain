(ns lain.experiments.pitched-sample
  (:require [overtone.core :refer :all]
            [lain.a300.events :refer [handle-a300-events]]
            [lain.a300.play :refer [midi-key-player
                                    remove-all-midi-players]]
            [lain.a300.control :refer [bus-controller
                                       remove-all-controllers]]
            [lain.insts :refer [key-inst]]
            [lain.utils :refer [deflcgen]]))

(def !window-size-bus (control-bus))
(def !window-rand-ratio-bus (control-bus))
(def !overlaps-bus (control-bus))

(def !glock-buf (load-sample "samples/glock1/c1.wav"))
(def !base-freq (midi->hz 60))  ; c1

(deflcgen glock-wave :ar
  [freq 440]
  (let
    [scale (/ freq !base-freq)]
    (warp1 :num-channels 1
           :bufnum !glock-buf
           :freq-scale scale
           :window-size (in:kr !window-size-bus)
           :overlaps (in:kr !overlaps-bus)
           :window-rand-ratio (in:kr !window-rand-ratio-bus)
           :interp 4)))

(def glock
  (key-inst
    glock-wave
    (adsr :attack 0.0001
          :decay 0.01
          :sustain 1
          :release 2.5 :level 2)))

(comment
  (handle-a300-events)

  (bus-controller !window-size-bus [:midi :r1])
  (bus-controller !window-rand-ratio-bus [:midi :r2])
  (bus-controller !overlaps-bus [:midi :r3] :extent [0 8])

  (midi-key-player glock :device-name "VirMIDI [default]")
  ())

(comment
  (remove-all-controllers)
  (remove-all-midi-players)
  ())
