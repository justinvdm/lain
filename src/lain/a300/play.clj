(ns lain.a300.play
  (:require [overtone.sc.node :refer [ctl
                                      node-active?]]
            [overtone.libs.counters :refer [next-id]]
            [overtone.libs.event :refer [on-event
                                         remove-event-handler]]
            [overtone.music.pitch :refer [midi->hz]]
            [overtone.sc.envelope :refer [perc]]))

(defonce midi-players (atom {}))

(defn bend-note [note offset]
  (let [bent-note (+ note offset)]
    [:note (int bent-note)
     :freq (midi->hz bent-note)]))


(defn midi-player [player-key & {:keys [down-event 
                                        up-event
                                        down
                                        up
                                        setup
                                        teardown
                                        device-name
                                        channel]
                                 :or {down-event [:midi :key :down]
                                      up-event [:midi :key :up]
                                      down (fn [& _] ())
                                      up (fn [& _] ())
                                      setup identity
                                      teardown (fn [& _] ())
                                      device-name nil
                                      channel nil}}]
  (let [down-event-key
        (concat [player-key] down-event)

        up-event-key
        (concat [player-key] up-event)

        notes
        (atom {})

        params
        (atom {})

        match
        (fn [{note :note
              event-channel :channel
              {event-device-name :name} :device}]
          (and (or (nil? channel)
                   (= channel event-channel))
               (or (nil? device-name)
                   (= device-name event-device-name))))]

    (on-event
      down-event
      (fn [e]
        (let [{note :note} e]
          (when (and (match e)
                     (not (contains? @notes note)))
            (swap! notes assoc note (down e (-> @params vec flatten))))))
      down-event-key)

    (on-event
      up-event
      (fn [e]
        (let [{note :note} e]
          (when (and (match e)
                     (contains? @notes note))
            (up e (get @notes note))
            (swap! notes dissoc note))))
      up-event-key)

    (let [player-id (next-id :midi-player)
          player {:player-id player-id
                  :setup setup
                  :teardown teardown
                  :down-event-key down-event-key
                  :up-event-key up-event-key
                  :notes notes
                  :params params}
          player (setup player)]
      (swap! midi-players assoc player-id player)
      player-id)))


(defn remove-midi-player [player-id]
  (let [player (get @midi-players player-id)
        {down-event-key :down-event-key
         up-event-key :up-event-key} player]
    ((:teardown player) player)
    (remove-event-handler down-event-key)
    (remove-event-handler up-event-key)
    (swap! midi-players dissoc player-id)))


(defn remove-all-midi-players []
  (doseq
    [[player-id player] @midi-players] 
    (remove-midi-player player-id)))


(defn midi-key-player [player-fn & {:keys [down-event
                                           up-event
                                           device-name]
                                    :or {down-event [:midi :key :down]
                                         up-event [:midi :key :up]
                                         device-name nil}}]
  (let [bend-offset (atom 0)]
    (midi-player
      :midi-key-player
      :device-name device-name
      :down-event down-event
      :up-event up-event

      :setup
      (fn [player]
        (assoc player :bend-offset bend-offset))

      :down
      (fn [{note :note
            velocity-f :velocity-f}
           params]
        (apply player-fn (concat (bend-note note @bend-offset)
                                 [:velocity-f velocity-f]
                                 params)))

      :up
      (fn [e node-id]
        (if (node-active? node-id)
          (ctl node-id :gate 0))))))


(defn ctl-midi-player [player-id & params]
  (let [{curr-params :params
         notes :notes} (get @midi-players player-id)]
    (swap! curr-params conj (apply hash-map params))
    (doseq
      [[note node-id] @notes] 
      (if (node-active? node-id)
        (apply ctl node-id params)))))


(defn bend-midi-keys
  [player-id
   bend-offset]
  (let [player (get @midi-players player-id)]
    (reset! (:bend-offset player) bend-offset)
    (doseq
      [[note node-id] @(:notes player)] 
      (apply ctl node-id (-> (bend-note bend-offset note) vec flatten)))))


(defn midi-buf-player [player-fn bufs & {:keys [down-event
                                                up-event
                                                device-name]
                                         :or {down-event [:midi :pad :down]
                                              up-event [:midi :pad :up]
                                              device-name nil}}]
  (midi-player
    :midi-buf-player
    :device-name device-name
    :down-event down-event
    :up-event up-event

    :down
    (fn [{note :note
          velocity-f :velocity-f}
         params]
      (when-let [buf (get bufs note)]
        (apply player-fn
               :buf buf
               :velocity-f velocity-f
               params)))))


(defn midi-perc-player [player-fns & {:keys [down-event
                                             up-event
                                             device-name]
                                      :or {down-event [:midi :pad :down]
                                           up-event [:midi :pad :up]
                                           device-name nil}}]
  (midi-player
    :midi-perc-player
    :device-name device-name
    :down-event down-event
    :up-event up-event

    :down
    (fn [{note :note
          velocity-f :velocity-f}
         params]
      (when-let [player-fn (get player-fns note)]
        (apply player-fn
               :velocity-f velocity-f
               params)))))


(defn midi-mono-player [player-fn & {:keys [down-event
                                            up-event
                                            device-name]
                                     :or {down-event [:midi :key :down]
                                          up-event [:midi :key :up]
                                          device-name nil}}]
  (let [node-id (atom nil)]
    (midi-player
      :midi-mono-player
      :device-name device-name
      :down-event down-event
      :up-event up-event

      :down
      (fn [{note :note
            velocity-f :velocity-f}
           params]
        (let [params (concat [:note note
                              :freq (midi->hz note)
                              :velocity-f velocity-f
                              :gate 1]
                             params)]
          (if (node-active? @node-id)
            (apply ctl @node-id params))
          (reset! node-id (apply player-fn params))))

      :up
      (fn [e _]
        (if (node-active? @node-id)
          (ctl @node-id :gate 0))))))
