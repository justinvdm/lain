(ns lain.experiments.synths
  (:require [overtone.core :refer :all]
            [lain.a300.events :refer [handle-a300-events]]
            [lain.a300.play :refer [key-player
                                    remove-all-players]]
            [lain.a300.control :refer [player-param-controller
                                       remove-all-controllers]]
            [lain.utils :refer [deflcgen]]))


(deflcgen detuned-saw :ar
  [freq 440
   offset 1]
  (let [sigs (saw [(- freq offset) freq (+ freq offset)])
        sig (mix sigs)
        sig (pan2 sig)]
    sig))


(deflcgen detuned-square :ar
  [freq 440
   offset 1]
  (let [sigs (square [(- freq offset) freq (+ freq offset)])
        sig (mix sigs)
        sig (pan2 sig)]
    sig))


(deflcgen vib :ar
  [freq 440
   rate 16
   depth 2]
  (let [offset (range-lin (sin-osc rate) (- depth) depth)]
    (+ freq offset)))


(definst k
  [freq 440
   velocity-f 1
   gate 1
   vib-rate 16
   vib-depth 16
   warp-rate 0.1
   warp-depth 4
   detune-offset 1
   lpf-cutoff 220
   release 1]
  (let
    [freq (vib freq vib-rate vib-depth)
     freq (vib freq warp-rate warp-depth)
     sig (detuned-saw freq detune-offset)defaultdefault
     sig (lpf sig lpf-cutoff)
     env (adsr :release release)
     env-sig (env-gen env gate :action FREE)]
    (* velocity-f env-sig sig)))


(do
  (handle-a300-events)
  (def p (key-player k :device-name "VirMIDI [default]"))
  (doseq [[param event-type extent]
          [[:vib-depth [:midi :r1] [2 16]]
           [:vib-rate [:midi :r2] [2 64]]
           [:detune-offset [:midi :r3] [0 5]]
           [:release [:midi :r4] [0 8]]
           [:warp-depth [:midi :r5] [0 10]]
           [:warp-rate [:midi :r6] [0.01 1]]
           [:lpf-cutoff [:midi :r7] [10 8000]]]]
    (player-param-controller p param event-type :extent extent))
  ())

(do
  (remove-all-players)
  (remove-all-controllers)
  ())
