(ns lain.sequencers
  (:require [clojure.set :refer [difference]]
            [mecha.core :refer [defmecha]]
            [overtone.sc.node :refer [kill]]
            [overtone.sc.synth :refer [defsynth]]
            [overtone.sc.ugens :refer [in:kr
                                       out:kr
                                       pulse-count:kr
                                       pulse-divider:kr
                                       buf-rd:kr
                                       round-up]]
            [overtone.sc.bus :refer [free-bus
                                     control-bus]]
            [overtone.sc.buffer :refer [buffer
                                        buffer-free
                                        buffer-write!]]
            [lain.utils :refer [metro]]))


(defonce default-res 4)
(defonce normal-res 128)


(defsynth trig-synth [buf 0
                      idx-bus 0
                      trig-bus 1]
  (out:kr trig-bus (buf-rd:kr 1 buf (in:kr idx-bus))))


(defsynth idx-synth [idx-bus 0
                     track-len 4
                     beat-bus 1
                     res 4]
  (let [div (/ normal-res res)
        p (pulse-divider:kr (in:kr beat-bus) div)
        idx (pulse-count:kr p)
        idx (mod idx track-len)]
    (out:kr idx-bus idx)))


(defn expand-steps [steps res]
  (let [fill-size (/ normal-res res)
        fill-size (- fill-size 1)
        steps (for [s steps] (cons s (repeat fill-size 0)))
        steps (flatten steps)]
    steps))


(defn normalize-sq [s res]
  (let [s (for [[syn steps] s] [syn (expand-steps steps res)])
        s (into {} s)]
    s))


(defmacro sq [res & form]
  `(let [result# (do ~@form)]
     (with-meta
       (fn [] (normalize-sq result# ~res))
       {:type ::sq
        :res ~res})))


(defmacro defsq [sq-name res & form]
  `(def ~sq-name (sq ~res ~@form)))


(defn sq-len [s]
  (-> s first second count))


(defn sq-add [a b]
  (let [keys-a (-> a keys set)
        keys-b (-> b keys set)
        keys-diff-ab (difference keys-a keys-b)
        keys-diff-ba (difference keys-b keys-a)
        fill-ab (for [k keys-diff-ab] [k (repeat (sq-len b) 0)])
        fill-ba (for [k keys-diff-ba] [k (repeat (sq-len a) 0)])
        a (merge a (into {} fill-ba))
        b (merge b (into {} fill-ab))
        a (merge-with concat a b)]
    a))


(defn sq+ [& sqs]
  (let [s (sq normal-res (let [sqs (for [s sqs] (s))]
                           (reduce sq-add sqs)))]
    s))


(defmecha track-sequencer [syn steps idx-bus]
  (:start [buf (-> steps count buffer)
           trig-bus (control-bus)
           _ (buffer-write! buf steps)
           trig-node (trig-synth buf idx-bus trig-bus)
           syn-node (syn :trig trig-bus)])
  (:stop (kill syn-node)
         (kill trig-node)
         (buffer-free buf)
         (free-bus trig-bus)))


(defmecha sequencer [s & [metronome nil]]
  (:start [s (if (vector? s) (apply sq+ s) s)
           s (s)
           metronome (or metronome (metro))
           idx-bus (control-bus)
           idx-node (idx-synth idx-bus
                               (sq-len s)
                               (:beats metronome)
                               (:res metronome))
           tracks (for [[syn steps] s] (track-sequencer syn steps idx-bus))
           tracks (doall tracks)])
  (:stop (kill idx-node)
         (free-bus idx-bus)))
