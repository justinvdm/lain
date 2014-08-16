(ns lain.experiments.woo
  (:require [overtone.core :refer :all]
            [mecha.core :as mecha :refer [defmecha]]
            [lain.a300.events]
            [lain.play :refer [mono-player]]
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


(defmecha experiment
  (:start [i (key-inst woo (adsr))
           p (mono-player i :device-name "APRO [hw:2,0,1]")]))


(def e (experiment))
(comment (mecha/stop e))
