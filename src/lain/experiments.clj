(ns lain.experiments
  (:require [overtone.live :refer :all]
            [a300.events :refer [handle-a300-events]]
            [a300.play :refer [play-midi-keys
                               remove-player]]))

(definst rrr
  [velocity-f 1 freq 440 gate 1]
  (let [snd  (lf-tri freq)
        env  (env-gen (adsr 0.0001 0.01 1 2.5) gate :action FREE)]
    (* velocity-f env snd)))

(defsynth lcut
  [cutoff-f 0
   rq-f 0
   cutoff-lo 100
   cutoff-hi 800]
  (let [cutoff (lin-lin cutoff-f 0 1 cutoff-lo cutoff-hi)
        f (rlpf (in 0) cutoff rq-f)]
    (replace-out 0 (pan2 f))))

(defsynth hcut
  [cutoff-f 0
   cutoff-lo 400
   cutoff-hi 800]
  (let [cutoff (lin-lin cutoff-f 0 1 cutoff-lo cutoff-hi)
        f (hpf (in 0) cutoff)]
    (replace-out 0 (pan2 f))))

(defsynth del
  [t-f 0
   t-lo 0
   t-hi 2]
  (let [input (in 0 2)
        t (lin-lin:kr t-f 0 1 t-lo t-hi)
        result (delay-c input t-hi t)
        result (+ result input)]
    (out 0 result)))

(definst compliments
  [velocity-f 1 freq 440 gate 1]
  (let [snd  (saw freq)
        env  (env-gen (adsr 5 0.1 1 5) gate :action FREE)]
    (* velocity-f env snd)))

(defsynth pitch-shifter
  [window-cize 0.5 
   pitch-ratio-f 1.0
   pitch-dispersion-f 0.0
   time-dispersion-f 0.0
   pitch-ratio-lo 0
   pitch-ratio-hi 4
   pitch-dispersion-lo 0
   pitch-dispersion-hi 4
   time-dispersion-lo 0
   time-dispersion-hi 0.02]
  (let [pitch-ratio
        (lin-lin pitch-ratio-f 0 1 pitch-ratio-lo pitch-ratio-hi)

        pitch-dispersion
        (lin-lin pitch-dispersion-f 0 1 pitch-dispersion-lo pitch-dispersion-hi)

        time-dispersion
        (lin-lin time-dispersion-f 0 1 time-dispersion-lo time-dispersion-hi)
        
        snd 
        (pitch-shift (in 0 2) window-cize pitch-ratio pitch-dispersion time-dispersion)]

  (replace-out 0 snd)))


(defsynth -bpf
  [cutoff-f 0
   rq-f 0
   cutoff-lo 3000
   cutoff-hi 10000]
  (let [cutoff (lin-lin cutoff-f 0 1 cutoff-lo cutoff-hi)
        snd (bpf (in 0) cutoff rq-f)]
    (out 0 snd)))

(defsynth -brf
  [cutoff-f 0
   rq-f 0
   cutoff-lo 100
   cutoff-hi 10000]
  (let [cutoff (lin-lin cutoff-f 0 1 cutoff-lo cutoff-hi)
        snd (brf (in 0) cutoff rq-f)]
    (out 0 snd)))

(comment
  (handle-a300-events)

  (odoc resonz)
  (odoc delay-c)
  (odoc mouse-x)
  (odoc free-verb)
  (odoc local-in)
  (odoc bpf)
  (odoc fx-compressor)
  (odoc brf)
  (odoc pitch-shift)
  (odoc line)
  (odoc warp1)

  (def !keyboard (play-midi-keys rrr))
  (def !keyboard (play-midi-keys compliments))
  (remove-player !keyboard)
  (def !lcut (lcut))
  (def !hcut (hcut))
  (def !del (del))
  (def !bpf (-bpf))
  (def !pitch-shifter (pitch-shifter))

  (fx-compressor)

  (remove-player !keyboard)

  ((synth
     (out 0 (delay-n (in 0 2)))))

  ((synth (out 0 (free-verb (in 0 2)))))

  (on-event
    [:midi :r1]
    (fn [{value-f :value-f}] (ctl !lcut :cutoff-f value-f))
    [:lcut :cutoff-f])

  (on-event
    [:midi :r2]
    (fn [{value-f :value-f}] (ctl !lcut :rq-f value-f))
    [:lcut :rq-f])

  (on-event
    [:midi :r3]
    (fn [{value-f :value-f}] (ctl !hcut :cutoff-f value-f))
    [:hcut :cutoff-f])

  (on-event
    [:midi :mod]
    (fn [{value-f :value-f}] (ctl !del :t-f value-f))
    [:del :t-f])

  (on-event
    [:midi :r3]
    (fn [{value-f :value-f}] (ctl !del :t-f value-f))
    [:del :t-f])

  (on-event
    [:midi :r1]
    (fn [{value-f :value-f}]
      (println value-f)
      (ctl !pitch-shifter :window-cize-f value-f))
    [:pitch-shifter :window-size-f])

  (on-event
    [:midi :r2]
    (fn [{value-f :value-f}]
      (println value-f)
      (ctl !pitch-shifter :pitch-ratio-f value-f))
    [:pitch-shifter :pitch-ratio-f])

  (on-event
    [:midi :r3]
    (fn [{value-f :value-f}]
      (println value-f)
      (ctl !pitch-shifter :pitch-dispersion-f value-f))
    [:pitch-shifter :pitch-dispersion-f])

  (on-event
    [:midi :r4]
    (fn [{value-f :value-f}]
      (println value-f)
      (ctl !pitch-shifter :time-dispersion-f value-f))
    [:pitch-shifter :time-dispersion-f])

  (on-event
    [:midi :r1]
    (fn [{value-f :value-f}] (ctl !bpf :cutoff-f value-f))
    [:bpf :cutoff-f])

  (on-event
    [:midi :r2]
    (fn [{value-f :value-f}] (ctl !bpf :rq-f value-f))
    [:bpf :rq-f])

  (on-event
    [:midi :s8]
    (fn [{value-f :value-f}] (ctl !bpf :cutoff-f value-f))
    [:bpf :cutoff-f])

  (on-event
    [:midi :s9]
    (fn [{value-f :value-f}] (ctl !bpf :rq-f value-f))
    [:bpf :rq-f])

  (on-event
    [:midi :s6]
    (fn [{value-f :value-f}] (ctl !bpf :cutoff-f value-f))
    [:brf :cutoff-f])

  (on-event
    [:midi :s7]
    (fn [{value-f :value-f}] (ctl !bpf :rq-f value-f))
    [:brf :rq-f])

  (ctl !pitch-shifter :pitch-ratio-lo 0.5)
  (ctl !pitch-shifter :pitch-ratio-hi 1)
  (odoc pitch)
  (odoc warp1)
  (odoc wet)
())
