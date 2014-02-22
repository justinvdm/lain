(ns lain.a300.play
  (:require [overtone.sc.node :refer [ctl]]
            [overtone.libs.counters :refer [next-id]]
            [overtone.libs.event :refer [on-event
                                         remove-event-handler]]
            [overtone.music.pitch :refer [midi->hz]]))

(defonce midi-key-players (atom {}))

(defn bend-note [note offset]
  (let [bent-note (+ note offset)]
    [:note (int bent-note)
     :freq (midi->hz bent-note)]))

(defn midi-key-player
  [player-fn & {:keys [down-event
                       up-event]
                 :or {down-event [:midi :key :down]
                      up-event [:midi :key :up]}}]

  (let [down-event-key (concat [:midi-key-player] down-event)
        up-event-key (concat [:midi-key-player] up-event)
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

    (let [player-id (next-id :midi-key-player)
          player {:player-id player-id
                  :down-event-key down-event-key
                  :up-event-key up-event-key
                  :notes notes
                  :bend-offset bend-offset}]
      (swap! midi-key-players assoc player-id player)
      player-id)))

(defn remove-key-player [player-id]
  (let [{down-event-key :down-event-key
         up-event-key :up-event-key} (get @midi-key-players player-id)]
    (remove-event-handler down-event-key)
    (remove-event-handler up-event-key)
    (swap! midi-key-players dissoc player-id)))

(defn remove-all-key-players []
  (doseq
    [[player-id player] @midi-key-players] 
    (remove-key-player player-id)))

(defn bend-midi-keys
  [player-id
   bend-offset]
  (let [player (get @midi-key-players player-id)]
    (reset! (:bend-offset player) bend-offset)
    (doseq
      [[note node-id] @(:notes player)] 
      (apply ctl (concat [node-id] (bend-note bend-offset note))))))
