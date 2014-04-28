(ns lain.play
  (:require [overtone.sc.node :refer [ctl
                                      node-active?]]
            [overtone.libs.event :refer [on-event
                                         remove-event-handler]]
            [overtone.music.pitch :refer [midi->hz]]
            [overtone.sc.envelope :refer [perc]]
            [mecha.core :refer [defmecha]]))


(defn bend-note [note offset]
  (let [bent-note (+ note offset)]
    [:note (int bent-note)
     :freq (midi->hz bent-note)]))


(defn bend-midi-keys [p bend-offset]
  (reset! (:bend-offset p) bend-offset)
  (doseq
    [[note node-id] @(:notes p)]
    (apply ctl node-id (-> (bend-note bend-offset note) vec flatten))))


(defn ctl-player [p & params]
  (let [{curr-params :params
         notes :notes} p]
    (swap! curr-params conj (apply hash-map params))
    (doseq
      [[note node-id] @notes] 
      (if (node-active? node-id)
        (apply ctl node-id params)))))


(defmecha player [player-key & [down-event [:midi :key :down]
                                up-event [:midi :key :up]
                                down (fn [& _])
                                up (fn [& _])
                                device-name nil
                                channel nil]]

  (:start [down-event-key (vec (concat [player-key] down-event))
           up-event-key (vec (concat [player-key] up-event))
           notes (atom {})
           params (atom {})

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

          {:notes notes
           :params params})

  (:stop (remove-event-handler down-event-key)
         (remove-event-handler up-event-key)))


(defmecha key-player [player-fn & [down-event [:midi :key :down]
                                   up-event [:midi :key :up]
                                   device-name nil]]

  (:start [bend-offset (atom 0)

           super
          (player
            :key-player
            :device-name device-name
            :down-event down-event
            :up-event up-event

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
                (ctl node-id :gate 0))))]
          (assoc super :bend-offset bend-offset)))


(defmecha buf-player [player-fn bufs & [down-event [:midi :key :down]
                                        up-event [:midi :key :up]
                                        device-name nil]]
  (:start [super
           (player
             :buf-player
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
                        params)))

             :up
             (fn [e node-id]
               (if (node-active? node-id)
                 (ctl node-id :gate 0))))]
          super))


(defmecha perc-player [player-fns & [down-event [:midi :pad :down]
                                     up-event [:midi :pad :up]
                                     device-name nil]]
  (:start [super
           (player
             :perc-player
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
                        params))))]
          super))


(defmecha mono-player [player-fn & [down-event [:midi :key :down]
                                    up-event [:midi :key :up]
                                    device-name nil]]
  (:start [node-id (atom nil)

           super
           (player
             :mono-player
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
                 (ctl @node-id :gate 0))))]
          super))
