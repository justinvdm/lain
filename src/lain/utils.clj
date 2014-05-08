(ns lain.utils
  (:require [clojure.string :refer [join split]]
            [clj-figlet.core :refer [render-to-string load-flf]]
            [overtone.libs.counters :refer :all]
            [overtone.music.pitch :refer :all]
            [overtone.studio.inst :refer :all]
            [overtone.sc.synth :refer :all]
            [overtone.sc.defcgen :refer :all]
            [overtone.sc.sample :refer :all]))


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
