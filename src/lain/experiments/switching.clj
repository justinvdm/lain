(ns lain.experiments.switching
  (:require [clojure.string :refer [split]]
            [overtone.core :refer :all]
            [lain.a300.events :refer [handle-a300-events]]
            [lain.a300.play :refer [players
                                    ctl-player
                                    buf-player
                                    remove-all-players]]
            [lain.a300.control :refer [mode-controller
                                       player-param-controller
                                       remove-all-controllers]]
            [lain.utils :refer [load-note-samples]]))


(def !saw-bufs (load-note-samples "samples/saw1/*.wav"))
(def !square-bufs (load-note-samples "samples/square1/*.wav"))


(defsynth ssynth
  [buf 0
   bus 0
   a 0.01
   d 0.3
   s 1
   r 2
   gate 1]
  (let [sig (scaled-play-buf 1 buf)
        env (env-gen (adsr a d s r) gate :action FREE)
        snd (* env sig)
        snd (pan2 snd)]
    (out bus snd)))


(defn mode [mode-name bufs]
  {:start
   (fn []
     (buf-player ssynth bufs :device-name "APRO [hw:2,0,1]")
     (println "==============")
     (println mode-name)
     (println "==============")
     (println ""))

   :end
   (fn []
     (remove-all-players))})


(def !modes {0 (mode "saw" !saw-bufs)
             1 (mode "square" !square-bufs)})


(defn start []
  (handle-a300-events)
  (mode-controller [:midi :r9] !modes))


(defn reset []
  (remove-all-players)
  (remove-all-controllers))


(comment
  (start)
  (reset))
