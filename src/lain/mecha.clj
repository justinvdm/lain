(ns lain.mecha
  (:require [overtone.helpers.ns :refer [immigrate]]
            [overtone.sc.node :refer :all]
            [overtone.sc.buffer :refer :all]
            [overtone.sc.bus :refer :all]
            [mecha.core :as mecha]))

(immigrate 'mecha.core)


(defmethod mecha/stop overtone.sc.node.SynthNode [n]
  (if (or (node-live? n)
          (node-loading? n))
    (kill n)))


(defmethod mecha/stop overtone.sc.buffer.Buffer [b]
  (if (buffer-live? b)
    (buffer-free b)))


(defmethod mecha/stop overtone.sc.bus.AudioBus [b]
  (free-bus b))


(defmethod mecha/stop overtone.sc.bus.ControlBus [b]
  (free-bus b))
