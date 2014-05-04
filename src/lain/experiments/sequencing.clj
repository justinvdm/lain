(ns lain.experiments.sequencing
  (:require [overtone.core :refer :all]
            [mecha.core :as mecha :refer [defmecha]]
            [lain.utils :refer [deflcgen]]
            [lain.sequencers :refer [defsq
                                     sequencer]]))


(def x 1)
(def _ 0)


(def !bufs {:kick (load-sample "samples/drums1/kick.wav")
            :snare (load-sample "samples/drums1/snare.wav")
            :hat (load-sample "samples/drums1/hat.wav")})


(deflcgen hit :kr [bus 0
                   buf 0]
  (let [trig (in:kr bus)
        env (env-gen (perc) :gate trig)
        sig (scaled-play-buf 2 buf :trigger trig)
        snd (* env sig)]
    snd))


(definst kick [bus 0]
  (hit bus (:kick !bufs)))


(definst snare [bus 0]
  (hit bus (:snare !bufs)))


(definst hat [bus 0]
  (hit bus (:hat !bufs)))


(defsq s1 4
  {kick  [x _ _ x x _ _ _]
   snare [_ _ x _ _ _ x _]
   hat   [x x x x x x x x]})


(defsq s2 8
  {kick  [x _ _ _ _ _ x _ x _ _ _ _ _ _ _]
   snare [_ _ _ _ x _ _ _ _ _ x _ x _ x x]
   hat   [x _ x _ x _ x _ x _ x _ x _ x _]})


(defmecha experiment
  (:start [s (sequencer [s1 s1 s1 s2])]))

(def e (experiment))
(comment (mecha/stop e))
