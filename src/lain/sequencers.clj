(ns lain.sequencers
  (:require [clojure.set :refer [difference]]
            [mecha.core :refer [defmecha]]
            [overtone.sc.node :refer :all]
            [overtone.sc.synth :refer :all]
            [overtone.sc.ugens :refer :all]
            [overtone.sc.bus :refer :all]
            [overtone.sc.buffer :refer :all]))


(defonce default-res 4)
(defonce normal-res 128)


(defsynth metro-synth [bpm 120
                       bpb 4
                       res 4
                       step-bus 0
                       step-idx-bus 0
                       beat-bus 1
                       bar-bus 2]
  (let [bpqs (/ bpm 240)
        step-freq (* normal-res bpqs)
        beat-freq (* res bpqs)
        bar-freq (/ beat-freq bpb)
        steps (impulse:kr step-freq)]
    (out:kr step-bus steps)
    (out:kr step-idx-bus (pulse-count:kr steps))
    (out:kr beat-bus (impulse:kr beat-freq))
    (out:kr bar-bus (impulse:kr bar-freq))))


(defmecha metro [& [bpm 120
                    bpb 4
                    res 4]]
  (:start [busses {:steps (control-bus)
                   :step-idx (control-bus)
                   :beats (control-bus)
                   :bars (control-bus)}
           n (metro-synth :bpm bpm
                          :bpb bpb
                          :res res
                          :step-bus (:steps busses)
                          :step-idx-bus (:step-idx busses)
                          :beat-bus (:beats busses)
                          :bar-bus (:bars busses))]
          (merge busses {:node n
                         :bpm bpm
                         :bpb bpb
                         :res res}))
  (:stop (kill n)
         (doseq [[k b] busses] (free-bus b))))


(defn expand-steps [steps res]
  (let [fill-size (/ normal-res res)
        fill-size (- fill-size 1)
        steps (for [s steps] (cons s (repeat fill-size 0)))
        steps (flatten steps)]
    steps))


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


(defn eval-sq [s & {:keys [res]
                    :or {res normal-res}}]
  (cond
    (= (type s) ::sq)
    (eval-sq (s))

    (sequential? s)
    (eval-sq (reduce sq-add (map eval-sq s)))

    (map? s)
    (into {} (for [[syn steps] s] [syn (expand-steps steps res)]))))


(defmacro sq [& form]
  (let [[res form] (if (integer? (first form))
                     [(first form) (rest form)]
                     [normal-res form])]
    `(with-meta
       (fn [] (eval-sq (do ~@form) :res ~res))
       {:type ::sq})))


(defmacro defsq [sq-name & form]
  `(def ~sq-name (sq ~@form)))


(defsynth trig-synth [buf 0
                      idx-bus 0
                      trig-bus 1]
  (out:kr trig-bus (buf-rd:kr 1 buf (in:kr idx-bus))))


(defmecha track-sequencer [syn steps idx-bus]
  (:start [buf (-> steps count buffer)
           trig-bus (control-bus)
           trig-node (trig-synth buf idx-bus trig-bus)
           syn-node (syn trig-bus)]
           (buffer-write! buf steps)
          {:buf buf
           :trig trig-bus
           :syn-node syn-node
           :trig-node trig-node})
  (:stop (kill syn-node)
         (kill trig-node)
         (buffer-free buf)
         (free-bus trig-bus)))


(defmecha sequencer [s & [metronome nil]]
  (:start [s ((sq s))
           metronome (or metronome (metro))
           idx-bus (:step-idx metronome)
           tracks (for [[syn steps] s] (track-sequencer syn steps idx-bus))
           tracks (vec tracks)]
          {:tracks tracks}))
