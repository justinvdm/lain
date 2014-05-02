(ns lain.utils
  (:require [clojure.string :refer [join split]]
            [clj-figlet.core :refer [render-to-string load-flf]]
            [mecha.core :refer [defmecha]]
            [overtone.libs.counters :refer [next-id]]
            [overtone.music.pitch :refer [note]]
            [overtone.studio.inst :refer [inst]]
            [overtone.sc.node :refer [kill]]
            [overtone.sc.bus :refer [control-bus free-bus]]
            [overtone.sc.ugens :refer [out:kr impulse:kr]]
            [overtone.sc.synth :refer [defsynth synth-form]]
            [overtone.sc.buffer :refer [buffer-info]]
            [overtone.sc.defcgen :refer [defcgen]]
            [overtone.sc.sample :refer [load-sample load-samples]]))


(def flf-doom (load-flf "resources/doom.flf"))


(defn lin-interpolator
  [[x1 x2]
   [y1 y2]]
  (let [xd (- x2 x1)
        yd (- y2 y1)
        m (/ yd xd)]
    (fn [x] (+ (* m (- x x1)) y1))))

(defmacro anon-inst
  "Workaround to declare an anonymous inst with params
  https://github.com/overtone/overtone/issues/248"
  [& inst-form]
  {:arglists '([name doc-string? params ugen-form])}
  (let
    [inst-name (symbol (join ["anon-inst" (next-id :anon-inst)]))
     [inst-name params ugen-form] (synth-form inst-name inst-form)
      inst-name (with-meta inst-name (merge (meta inst-name) {:type ::instrument}))]
    `(inst ~inst-name ~params ~ugen-form)))

(defmacro deflcgen
  "Define a lightweight cgen"
  [cgen-name rate params & body]
  (let
    [params (partition 2 params)
     params (for [[p-name, p-val] params] [p-name {:default p-val}])
     params (vec (flatten params))]
    `(defcgen ~cgen-name "" ~params (~rate ~@body))))


(defn load-note-samples [& glob]
  (let [bufs (apply load-samples glob)
        bufs (for [buf bufs]
               (let [note-name (:name buf)
                     note-name (first (split note-name #"\."))
                     note-name (if (re-matches #"^[0-9]+$" note-name)
                                 (read-string note-name)
                                 note-name)
                     note-name (note note-name)]
                 [note-name buf]))]
    (into {} bufs)))


(defn ascii [s]
  (println (render-to-string flf-doom s)))


(defsynth metro-synth [bpm 120
                       bpb 4
                       res 4
                       beat-bus 0
                       bar-bus 1]
  (let [quarters-ps (/ bpm 240)
        beat-freq (* res quarters-ps)
        bar-freq (/ beat-freq bpb)]
    (out:kr beat-bus (impulse:kr beat-freq))
    (out:kr bar-bus (impulse:kr bar-freq))))


(defmecha metro [& [bpm 120
                    bpb 4
                    res 4]]
  (:start [busses {:beats (control-bus)
                   :bars (control-bus)}
           n (metro-synth :bpm bpm
                          :bpb bpb
                          :res res
                          :beat-bus (:beats busses)
                          :bar-bus (:bars busses))]
          (merge busses {:node n
                         :bpm bpm
                         :bpb bpb
                         :res res}))
  (:stop (kill n)
         (doseq [[k b] busses] (free-bus b))))
