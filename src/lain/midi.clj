(ns lain.midi
  (:require [overtone.sc.node :refer [ctl]]
            [overtone.libs.counters :refer [next-id]]
            [overtone.libs.event :refer [on-event
                                         remove-handler]]
            [overtone.music.pitch :refer [midi->hz]]))

(defonce midi-key-players (atom {}))

(defn bend-note [note offset]
  (let [bent-note (+ note offset)]
    [:note (int bent-note)
     :freq (midi->hz bent-note)]))

(defn play-midi-keys
  [player-fn & {:keys [down-event
                        up-event
                        bend-event
                        bend-extent
                        bend?]
                 :or {down-event [:midi :key :down]
                      up-event [:midi :key :up]
                      bend-event [:midi :bend]
                      bend-extent 4
                      bend? true}}]

  (let [down-event-key (concat [:play-midi-keys] down-event)
        up-event-key (concat [:play-midi-keys] up-event)
        bend-event-key (concat [:play-midi-keys] bend-event)
        notes (atom {})
        bend-offset (atom 0)]

    (on-event
      down-event
      (fn [{note :note
            velocity-f :velocity-f}]
        (let [play-args (concat (bend-note note @bend-offset)
                                [:velocity-f velocity-f])
              node-id (apply player-fn play-args)]
          (swap! notes assoc note node-id)))
      down-event-key)

    (on-event
      up-event
      (fn [{note :note}]
        (when-let [n (get @notes note)]
          (ctl n :gate 0)
          (swap! notes dissoc note)))
      up-event-key)

    (when bend?
      (on-event
        bend-event
        (fn [{bending-f :bending-f}]
          (reset! bend-offset (* bend-extent bending-f))

          (doseq
            [[note node-id] @notes] 
            (apply ctl (concat [node-id] (bend-note @bend-offset note)))))
        bend-event-key))

    (let [player-id (next-id :play-midi-keys)
          player {:player-id player-id
                  :down-event-key down-event-key
                  :up-event-key up-event-key
                  :bend-event-key (if bend-event bend-event-key nil)
                  :notes notes}]
      (swap! midi-key-players assoc player-id player)
      player-id)))

(defn remove-player [player-id]
  (let [{down-event-key :down-event-key
         up-event-key :up-event-key
         bend-event-key :bend-event-key} (get @midi-key-players player-id)]
    (remove-handler down-event-key)
    (remove-handler up-event-key)
    (when bend-event-key
      (remove-handler bend-event-key))
    (swap! midi-key-players dissoc player-id)))

(defn remove-all-players []
  (doseq
    [[player-id player] @midi-key-players] 
    (remove-player player-id)))
