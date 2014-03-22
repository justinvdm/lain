(ns lain.experiments.buffers
  (:require [overtone.core :refer :all]
            [lain.a300.events :refer [handle-a300-events]]
            [lain.a300.play :refer [midi-buf-player]]
            [lain.insts :refer [buf-inst]]))

(def !bufs {1 (load-sample "samples/drums1/kick.wav")
            3 (load-sample "samples/drums1/snare.wav")
            4 (load-sample "samples/drums1/snare.wav")
            8 (load-sample "samples/drums1/hat.wav")})

(comment
  (handle-a300-events)
  (midi-buf-player (buf-inst (perc)) !bufs :device-name "VirMIDI [default]")
  ())
