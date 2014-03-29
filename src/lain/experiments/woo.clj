(ns lain.experiments.woo
  (:require [overtone.core :refer :all]
            [lain.a300.events :refer [handle-a300-events]]
            [lain.a300.play :refer [midi-mono-player
                                    remove-all-midi-players]]
            [lain.a300.control :refer [value-controller
                                       remove-all-controllers]]
            [lain.insts :refer [key-inst]]
            [lain.utils :refer [deflcgen]]))

(deflcgen woo :ar
  [freq 440
   vib-rate 16
   vib-depth 5
   detune 1
   lag-time 0.2]
  (let
    [vib (range-lin (sin-osc vib-rate) (- vib-depth) vib-depth)
     freq (lag freq lag-time)
     freq (+ vib freq)
     freqs [(- freq detune) freq (+ freq detune)]
     freqs [(* 0.75 freqs) freqs (* 1.25 freqs)]
     sigs (map saw freqs)
     sig (mix sigs)
     sig (pan2 sig)]
    sig))

(comment
  (handle-a300-events)

  (midi-mono-player
    (key-inst woo (adsr :sustain 1
                        :decay 3
                        :release 8))
    :device-name "VirMIDI [default]")

  (value-controller !window-size-bus [:midi :r1])
  ())

(comment
  (remove-all-midi-players)
  ())
