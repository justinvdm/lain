(ns lain.utils
  (:require [overtone.studio.inst :refer [inst]]
            [overtone.sc.synth :refer [synth-form]]
            [overtone.sc.ugens :refer [FREE
                                       env-gen]]))

(defmacro temp-inst
  "Workaround to declare an inst with params without using definst
  https://github.com/overtone/overtone/issues/248"
  [inst-name & inst-form]
  {:arglists '([name doc-string? params ugen-form])}
  (let
    [[inst-name params ugen-form] (synth-form inst-name inst-form)
      inst-name (with-meta inst-name (merge (meta inst-name) {:type ::instrument}))]
    `(inst ~inst-name ~params ~ugen-form)))

(defn key-inst
  [wave
   env]
  (temp-inst
    _
    [freq 440
     velocity-f 1
     gate 1]
    (let
      [wave-val (wave :freq freq)
       env-val (env-gen env gate :action FREE)]
      (* velocity-f env-val wave-val))))

(defn key-note-inst
  [wave
   env]
  (temp-inst
    _
    [note 60
     velocity-f 1
     gate 1]
    (let
      [wave-val (wave :note note)
       env-val (env-gen env gate :action FREE)]
      (* velocity-f env-val wave-val))))
