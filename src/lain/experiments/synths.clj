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


(deflcgen organ-waves :ar
  [freq 440]
  (let [waves (sin-osc [(* 0.5 freq)
                        freq
                        (* (/ 3 2) freq)
                        (* 2 freq)
                        (* freq 2 (/ 3 2))
                        (* freq 2 2)
                        (* freq 2 2 (/ 5 4))
                        (* freq 2 2 (/ 3 2))
                        (* freq 2 2 2)])
        sig   (apply + waves)]
    sig))


(deflcgen fucked-organ-waves :ar
  [freq 440]
  (let [waves (sin-osc [(* 0.5 freq)
                        freq
                        (* 1.4142 freq)
                        (* 2 freq)
                        (* freq 2 1.4142)
                        (* freq 2 2)
                        (* freq 2 2 (/ 5 4))
                        (* freq 2 2 1.4142)
                        (* freq 2 2 2)])
        sig   (apply + waves)]
    sig))


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
     sig (detuned-saw freq detune-offset)
     sig (lpf sig lpf-cutoff)
     env (adsr :release release)
     env-sig (env-gen env gate :action FREE)]
    (* velocity-f env-sig sig)))


(deflcgen screamer :ar
  [sig 0
   wet 0.5
   hi-freq 720.484
   low-freq 723.431
   hi-freq2 1
   gain 4
   threshold 0.4]
  (let [f1 (* (hpf sig hi-freq) gain)
        f2 (lpf (clip2 f1 threshold) low-freq)
        f3 (hpf f2 hi-freq2)
        sig (+ (* sig (- 1 wet)) (* f3 wet))]
    sig))


(deflcgen echo :ar
  [sig 0
   max-delay 1.0
   delay-time 0.4
   decay-time 2.0]
  (let [result (comb-n sig max-delay delay-time decay-time)
        sig (+ sig result)]
    sig))


(deflcgen chorus :ar
  [sig 0
   rate 0.002
   depth 0.01
   att 0.7]
  (let [dbl-depth (* 2 depth)
        rates [rate (+ rate 0.001)]
        osc (+ dbl-depth (* dbl-depth (sin-osc:kr rates)))
        dly-a (delay-l sig 0.3 osc)
        sig (+ sig dly-a)
        sig (* (- 1 att) sig)]
    sig))


(deflcgen distortion :ar
  [sig 0
   amount 0.5]
  (let [k (/ (* 2 amount) (- 1 amount))
        sig (/ (* sig (+ 1 k)) (+ 1 (* k (abs sig))))]
    sig))


(definst evil-tower
  [bus 0
   freq 440
   a 1
   d 2
   s 2
   r 2
   gate 1
   reverb-mix 1
   reverb-room 1
   reverb-damp 1
   lpf-cutoff 440
   lpf-rq 1
   distortion-amount 3]
  (let [sig (fucked-organ-waves freq)
        sig (free-verb sig reverb-mix reverb-room reverb-damp)
        sig (free-verb sig reverb-mix reverb-room reverb-damp)
        sig (free-verb sig reverb-mix reverb-room reverb-damp)
        sig (rlpf sig lpf-cutoff lpf-rq)
        env (env-gen (adsr a d s r) gate :action FREE)
        snd (* env sig 0.1)
        snd (pan2 snd)]
    snd))


(do
  (handle-a300-events)
  (def p (key-player evil-tower :device-name "VirMIDI [default]"))
  (doseq [[param event-type extent]
          [[:lpf-cutoff [:midi :r1] [440 8000]]
           [:lpf-rq [:midi :r2] [0.0001 1]]
           [:distortion-amount [:midi :r3] [0.01 1]]]]
    (player-param-controller p param event-type :extent extent))
  ())

(do
  (remove-all-players)
  (remove-all-controllers)
  ())
