(ns lain.experiments.synths
  (:require [overtone.core :refer :all]
            [mecha.core :as mecha :refer [defmecha]]
            [lain.a300]
            [lain.play :refer [key-player]]
            [lain.control :refer [player-param-controller]]
            [lain.utils :refer [deflcgen]]))


(def !glock-buf (load-sample "samples/glock1/c1.wav"))


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

(deflcgen space-verb :ar
  [sig 0
   predelay-t 0.48
   combs-count 6
   combs-t-min 0.01
   combs-t-max 0.1
   combs-decay 5
   allpass-count 4
   allpass-t-min 0.01
   allpass-t-max 0.05
   allpass-decay 1]
  (let [predelay (delay-n sig predelay-t predelay-t)
        comb (comb-l predelay
                     combs-t-max
                     (ranged-rand combs-t-min combs-t-max)
                     combs-decay)
        sig (mix (repeat combs-count comb))
        sig (loop [n allpass-count
                   res sig]
              (if (<= n 0)
                res
                (recur (dec n)
                       (allpass-n res
                                  allpass-t-max
                                  [(ranged-rand allpass-t-min allpass-t-max)
                                   (ranged-rand allpass-t-min allpass-t-max)]
                                  allpass-decay))))]
    sig))

(definst evil-tower
  [freq 440
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

(definst drony-sin
  [freq 440
   a 0.01
   d 0.3
   s 1
   r 1
   gate 1
   phase-mod-freq 0.87
   phase-mod-depth 7.32
   amp-mod-freq 9.29
   amp-mod-depth 9.37]
  (let [phase-mod (* phase-mod-depth (sin-osc phase-mod-freq))
        amp-mod (* amp-mod-depth (lf-noise1 amp-mod-freq))
        sig (sin-osc freq phase-mod)
        sig (+ sig (chorus sig))
        sig (* amp-mod sig)
        env (env-gen (adsr a d s r) gate :action FREE)
        snd (* env sig 0.1)
        snd (pan2 snd)]
    snd))


(deflcgen detuned-sin :ar
  [freq 440
   offset 1]
  (let [sigs (sin-osc [(- freq offset) freq (+ freq offset)])
        sig (mix sigs)
        sig (pan2 sig)]
    sig))


(deflcgen kw :ar
  [freq 440
   offset 1]
  (let [freqs [(- freq offset) freq (+ freq offset)]
        freqs (concat (* 2 freqs) freqs)
        sigs (sin-osc freqs)
        sig (mix sigs)
        sig (pan2 sig)]
    sig))


(definst mecha
  [freq 440
   a 0.01
   d 0.3
   s 1
   r 2
   gate 1
   detune-offset 3.7
   fb-coef 0.9995
   leak-coef 0.995]
  (let [sig (detuned-saw freq detune-offset)
        sig (space-verb sig :predelay-t 0)
        fb (leak-dc (* fb-coef (local-in)) leak-coef)
        env (env-gen (adsr a d s r) gate :action FREE)
        snd (* env sig 0.1)
        snd (pan2 snd)]
    (local-out snd)
    snd))


(definst ant
  [freq 440
   a 0.01
   d 0.3
   s 1
   r 2
   gate 1
   detune-offset 3.7
   lpf-cutoff 440
   lpf-rq 1]
  (let [sig (detuned-square freq detune-offset)
        sig (rlpf sig lpf-cutoff lpf-rq)
        env (env-gen (adsr a d s r) gate :action FREE)
        snd (* env sig 0.1)
        snd (pan2 snd)]
    snd))


(defsynth feedback
  [bus 0
   fb-coef 0.995
   leak-coef 0.995]
  (let [snd (in 0)
        fb (leak-dc (* fb-coef (local-in)) leak-coef)
        snd (+ snd fb)]
    (local-out snd)
    (out 0 (pan2 snd))))


(definst granular-test
  [freq 440
   a 0.01
   d 0.3
   s 1
   r 1
   gate 1
   grain-dur 1.49
   grain-amp 15.0
   grain-center 0.1
   base-freq (midi->hz 60)]
  (let [sig (t-grains :rate (/ freq base-freq)
                      :bufnum !glock-buf
                      :trigger 1
                      :dur grain-dur
                      :amp grain-amp
                      :center-pos grain-center)
        env (env-gen (adsr a d s r) gate :action FREE)
        snd (* env sig 0.1)
        snd (pan2 snd)]
    snd))


(defmecha experiment
  (:start [p (key-player drony-sin :device-name "APRO [hw:2,0,1]")]))


(def e (experiment))
(comment (mecha/stop e))
