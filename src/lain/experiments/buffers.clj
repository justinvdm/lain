(ns lain.experiments.buffers
  (:require [overtone.core :refer :all]
            [mecha.core :as mecha :refer [defmecha]]
            [lain.a300.events]
            [lain.play :refer [buf-player]]
            [lain.insts :refer [buf-inst]]))

(def !bufs {1 (load-sample "samples/drums1/kick.wav")
            3 (load-sample "samples/drums1/snare.wav")
            4 (load-sample "samples/drums1/snare.wav")
            8 (load-sample "samples/drums1/hat.wav")})


(defmecha experiment
  (:start [p (buf-player (buf-inst (perc))
                         !bufs
                         :device-name "APRO [hw:2,0,1]"
                         :down-event [:midi :pad :down]
                         :up-event [:midi :pad :up])]))


(def e (experiment))
(comment (mecha/stop e))
