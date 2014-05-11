(ns lain.rp
  (:require [overtone.libs.counters :refer :all]
            [overtone.sc.node :refer :all]
            [overtone.sc.synth :refer :all]
            [overtone.sc.envelope :refer :all]
            [overtone.sc.ugens :refer :all]
            [overtone.libs.event :refer :all]
            [overtone.sc.cgens.buf-io :refer :all]
            [overtone.sc.bus :refer :all]
            [overtone.sc.buffer :refer :all]
            [lain.utils :refer [deflcgen]]
            [lain.mecha :as mecha :refer [mecha
                                          switch
                                          defmecha]]))


(def default-tick-size 1000)
(def default-buffer-size (Math/pow 2 22))


(deflcgen syn-if :kr [v 1
                      then 1
                      else 0]
  (let [t (= v 1)
        f (= v 0)
        t (* t then)
        f (* f else)
        r (+ t f)]
    r))


(deflcgen syn-tick :kr [freq default-tick-size
                        gate 1]
  (let [p (and gate (impulse:kr freq))
        c (pulse-count:kr p)]
    (/ c freq)))


(defsynth syn-rec [in-bus 0
                   out-buf 0
                   timer-bus 1
                   sync-bus -1]
  (let [sync-trig (syn-if (> sync-bus 0) (in:kr sync-bus) 1)
        sync-trig (set-reset-ff:kr sync-trig)]
    (out:kr timer-bus (syn-tick :gate sync-trig))
    (record-buf:ar (in:ar in-bus) out-buf :run sync-trig :loop 0)))


(defmecha rp-rec [& [in-bus 0
                     sync-bus -1
                     buf-size default-buffer-size]]
  (:start [!out-buf (buffer buf-size)
           !timer-bus (control-bus)
           rec-node (syn-rec in-bus
                             !out-buf
                             !timer-bus
                             sync-bus)]
          {:node rec-node
           :out-buf !out-buf
           :timer-bus !timer-bus}))


(defsynth syn-play [in-buf 0
                    out-bus 0
                    timer-bus 1
                    sync-bus -1
                    looped 1
                    attack 0.01
                    sustain 1
                    release 1]
  (let [sync-trig (set-reset-ff:kr (in:kr sync-bus) (local-in:kr))
        sync-trig (syn-if (> sync-bus 0) sync-trig 1)

        rec-len (in:kr timer-bus)
        timer-pos (syn-tick :gate sync-trig)
        play-pos (mod timer-pos rec-len)
        play-done (= (round play-pos 1) 0)
        first-play (<= timer-pos rec-len)
        loop-trig (syn-if (or looped first-play) sync-trig 0)

        env (asr attack sustain release)
        env (env-gen:ar env sync-trig)

        sig (scaled-play-buf:ar 1 in-buf :trigger loop-trig :loop 0)
        sig (* env sig)]
    (local-out:kr play-done)
    (out:ar out-bus (pan2 sig))))


(defmecha rp-player [& [in-buf 0
                        out-bus 0
                        timer-bus 1
                        sync-bus -1
                        looped true]]
  (:start [play-node (syn-play in-buf
                               out-bus
                               timer-bus
                               sync-bus
                               (if looped 1 0))]
          {:node play-node}))


(defmecha rp [& [in-bus 0
                 out-bus 0
                 sync-bus -1
                 looped true
                 buf-size default-buffer-size
                 attack 0.01
                 sustain 1
                 release 1]]
  (:start [curr-rec (atom nil)
           rec-mode (mecha (:start [r (rp-rec :in-bus in-bus
                                              :sync-bus sync-bus
                                              :buf-size buf-size)]
                                   (reset! curr-rec r)
                                   r))
           play-mode (mecha (:start [p (if @curr-rec
                                         (rp-player :in-buf (:out-buf @curr-rec)
                                                    :out-bus out-bus
                                                    :timer-bus (:timer-bus @curr-rec)
                                                    :sync-bus sync-bus
                                                    :looped looped)
                                         nil)]
                                    p))
           modes (switch {:play play-mode
                          :rec rec-mode})]
          {:modes modes})
  (:stop (let [{out-buf :out-buf
                timer-bus :timer-bus} @curr-rec]
           (if (buffer-live? out-buf) (buffer-free out-buf))
           (if-not (nil? timer-bus) (free-bus timer-bus)))))


(defn rp-mode [t-rp mode-name]
  ((:modes t-rp) mode-name))


(defmecha a300-looper [down-event
                       up-event
                       & [in-bus 0
                          out-bus 0
                          mode-down-event [:midi :l9 :on]
                          mode-up-event [:midi :l9 :off]
                          sync-bus -1
                          looped true
                          buf-size default-buffer-size]]

  (:start [rp-key [::a300-looper (next-id :a300-looper)]
           up-key (concat rp-key up-event)
           down-key (concat rp-key down-event)
           mode-down-key (concat rp-key mode-down-event)
           mode-up-key (concat rp-key mode-up-event)
           recording (atom false)
           this-rp (rp :in-bus in-bus
                       :out-bus out-bus
                       :sync-bus sync-bus
                       :looped looped
                       :buf-size buf-size)]

          (on-event
            down-event
            (fn [e]
              (if @recording
                (rp-mode this-rp :rec)
                (rp-mode this-rp :play)))
            down-key)

          (on-event
            up-event
            (fn [e]
              (mecha/stop (:modes this-rp)))
            up-key)

          (on-event
            mode-down-event
            (fn [e]
              (reset! recording true))
            mode-down-key)

          (on-event
            mode-up-event
            (fn [e]
              (when @recording
                (rp-mode this-rp :play)
                (reset! recording false)))
            mode-up-key)

          {:rp this-rp})

  (:stop (remove-event-handler up-key)
         (remove-event-handler down-key)
         (remove-event-handler mode-down-key)
         (remove-event-handler mode-up-key)))
