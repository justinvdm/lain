(ns lain.utils
  (:require [clojure.string :refer [join]]
            [overtone.libs.counters :refer [next-id]]
            [overtone.studio.inst :refer [inst]]
            [overtone.sc.synth :refer [synth-form]]
            [overtone.sc.defcgen :refer [defcgen]]
            [overtone.sc.sample :refer [load-sample]]))

(defn lin-interpolator
  [[x1 x2]
   [y1 y2]]
  (let [xd (- x2 x1)
        yd (- y2 y1)
        m (/ yd xd)]
    (fn [x] (+ (* m (- x x1)) y1))))

(defn format-modes [modes]
  (into
    {}
    (for
      [[mode-key mode] modes] 
      (if-not (map? mode)
        [mode-key {:start mode :end (fn [& _])}]
        [mode-key mode]))))

(defn mode-switcher [modes]
  (let
    [modes (format-modes modes)
     mode-key (atom nil)]
    (fn [new-mode-key]
      (when-not (= @mode-key new-mode-key)
        (when-let [old-mode-key @mode-key]
          ((:end (get modes old-mode-key))))
        (reset! mode-key new-mode-key)
        (when new-mode-key
          ((:start (get modes new-mode-key))))))))

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
