(ns lain.experiments.drums
  (:require [overtone.live :refer :all]
            [lain.a300.events :refer [handle-a300-events]]
            [lain.a300.play :refer [buf-player]]
            [lain.insts :refer [buf-inst]]))

(def !bufs {1 (load-sample "samples/drums1/kick.wav")
            4 (load-sample "samples/drums1/snare.wav")
            8 (load-sample "samples/drums1/hat.wav")})

(defn start []
  (handle-a300-events)
  (buf-player (buf-inst (perc)) !bufs))
