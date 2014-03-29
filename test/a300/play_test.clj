(ns lain.a300.play-test
  (:require [speclj.core :refer :all]
            [overtone.sc.node :refer [ctl
                                      node-active?]]
            [overtone.libs.counters :refer [next-id
                                            reset-all-counters!]]
            [overtone.libs.event :refer [sync-event
                                         remove-event-handler]]
            [overtone.music.pitch :refer [midi->hz]]
            [lain.a300.play :refer :all]))

(describe "play"
  (with-stubs)

  (before
    [(remove-all-midi-players)
     (reset-all-counters!)])

  (describe "midi-player"
    (it "should invoke its setup hook"
      (midi-player :test-player :setup (stub :setup {:invoke identity}))
      (should-have-invoked :setup {:times 1}))

    (describe "when a key is pressed"
      (it "should handle the event"
        (midi-player :test-player :down (stub :down))

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (sync-event
          [:midi :key :down]
          {:note 61
           :velocity-f 0.5})

        (should-have-invoked :down {:with [{:note 60
                                            :velocity-f 0.5}]
                                    :times 1})

        (should-have-invoked :down {:with [{:note 61
                                            :velocity-f 0.5}]
                                    :times 1}))

      (it "should not handle the event if the key is already pressed"
        (midi-player :test-player :down (stub :down))

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (should-have-invoked :down {:times 1}))

      (it "should not handle the event if the key belongs to another device"
        (midi-player :test-player
                     :device "device-foo"
                     :down (stub :down))

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5
           :device {:name "device-foo"}})

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5
           :device {:name "device-bar"}})

        (should-have-invoked :down {:times 1}))

      (it "should not handle the event if the key belongs to another channel"
        (midi-player :test-player
                     :channel 1
                     :down (stub :down))

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5
           :channel 1})

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5
           :channel 2})

        (should-have-invoked :down {:times 1})))

    (describe "when a key is released"
      (it "should handle the event"
        (midi-player
          :test-player
          :down (stub :down {:return true})
          :up (stub :up))

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (sync-event
          [:midi :key :down]
          {:note 61
           :velocity-f 0.5})

        (sync-event
          [:midi :key :up]
          {:note 61
           :velocity-f 0.5})

        (should-have-invoked :up {:with [{:note 61
                                          :velocity-f 0.5}
                                         true]
                                  :times 1}))

      (it "should ignore the event if no corresponding down event has happened"
        (midi-player :test-player :up (stub :up))

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (sync-event
          [:midi :key :up]
          {:note 60
           :velocity-f 0.5})

        (should-have-invoked :up {:times 1}))

      (it "should ignore the event if it belongs to another device"
        (midi-player
          :test-player
          :device "device-foo"
          :up (stub :up))

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5
           :device {:name "device-foo"}})

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5
           :device {:name "device-foo"}})

        (sync-event
          [:midi :key :up]
          {:note 60
           :velocity-f 0.5
           :device {:name "device-foo"}})

        (sync-event
          [:midi :key :up]
          {:note 60
           :velocity-f 0.5
           :device {:name "device-bar"}})

        (should-have-invoked :up {:times 1}))

      (it "should ignore the event if it belongs to another channel"
        (midi-player
          :test-player
          :channel 1
          :up (stub :up))

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5
           :channel 1})

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5
           :channel 2})

        (sync-event
          [:midi :key :up]
          {:note 60
           :velocity-f 0.5
           :channel 1})

        (sync-event
          [:midi :key :up]
          {:note 60
           :velocity-f 0.5
           :channel 2})

        (should-have-invoked :up {:times 1}))))

(describe "remove-midi-player"
  (it "should invoke the player's teardown hook"
    (remove-midi-player (midi-player :test-player :teardown (stub :teardown)))
    (should-have-invoked :teardown {:times 1}))

  (it "should stop listening to key press events associated with the player"
    (should-invoke
      remove-event-handler
      {:with [[:test-player :midi :key :down]]}
      (remove-midi-player (midi-player :test-player))))

  (it "should stop listening to key release events associated with the player"
    (should-invoke
      remove-event-handler
      {:with [[:test-player :midi :key :up]]}
      (remove-midi-player (midi-player :test-player)))))

(describe "remove-all-midi-players"
  (it "should remove all midi players"
    (midi-player :foo)
    (midi-player :bar)

    (should-invoke
      remove-midi-player
      {:with [0] :times 1}
      {:with [1] :times 1}
      (remove-all-midi-players))))

(describe "midi-key-player"
  (describe "when a key is pressed"
    (it "should play the player function"
      (midi-key-player (stub :play))

      (sync-event
        [:midi :key :down]
        {:note 60
         :velocity-f 0.5})

      (sync-event
        [:midi :key :down]
        {:note 61
         :velocity-f 0.5})

      (should-have-invoked :play {:with [:note 60
                                         :freq (midi->hz 60)
                                         :velocity-f 0.5]
                                  :times 1})

      (should-have-invoked :play {:with [:note 61
                                         :freq (midi->hz 61)
                                         :velocity-f 0.5]
                                  :times 1})))

  (describe "when a key is released"
    (it "should zeroize the gate of the player function for that note"
      (midi-key-player
        (stub :play {:invoke (fn [_ _ _ _ _ _] (next-id :fake-node))}))

      (with-redefs [node-active? (stub :node-active? {:return true})]
        (should-invoke
          ctl
          {:with [1 :gate 0]
           :times 1}
          (sync-event
            [:midi :key :down]
            {:note 60
             :velocity-f 0.5})

          (sync-event
            [:midi :key :down]
            {:note 61
             :velocity-f 0.5})

          (sync-event
            [:midi :key :up]
            {:note 61
             :velocity-f 0.5}))))

    (it "should not zeroize the gate of the note is inactive"
      (midi-key-player
        (stub :play {:invoke (fn [_ _ _ _ _ _] (next-id :fake-node))}))

      (with-redefs [node-active? (stub :node-active? {:return false})]
        (should-not-invoke
          ctl
          (sync-event
            [:midi :key :down]
            {:note 60
             :velocity-f 0.5})

          (sync-event
            [:midi :key :down]
            {:note 61
             :velocity-f 0.5})

          (sync-event
            [:midi :key :up]
            {:note 61
             :velocity-f 0.5})))))

  (describe "bend-midi-keys"
    (it "should bend the key player's active notes"
      (let [play (stub :play {:invoke (fn [_ _ _ _ _ _] (next-id :fake-node))})
            player-id (midi-key-player play)]

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})
        (sync-event
          [:midi :key :down]
          {:note 61
           :velocity-f 0.5})

        (should-invoke
          ctl
          {:with [0
                  :note 62
                  :freq (midi->hz 62)]
           :times 1}
          {:with [1
                  :note 63
                  :freq (midi->hz 63)]
           :times 1}
          (bend-midi-keys player-id 2))))

    (it "should bend new notes played by the key player"
      (let [player-id (midi-key-player (stub :play))]
        (bend-midi-keys player-id 2)

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (sync-event
          [:midi :key :down]
          {:note 61
           :velocity-f 0.5})


        (should-have-invoked :play {:with [:note 62
                                           :freq 293.6647679174076
                                           :velocity-f 0.5]
                                    :times 1})
        (should-have-invoked :play {:with [:note 63
                                           :freq 311.1269837220809
                                           :velocity-f 0.5]
                                    :times 1}))))

  (describe "midi-buf-player"
    (describe "when the down event is emitted"
      (it "should play the player function"
        (midi-buf-player (stub :play) {2 :buf-2
                                       3 :buf-3})

        (sync-event
          [:midi :pad :down]
          {:note 2
           :velocity-f 0.6})

        (sync-event
          [:midi :pad :down]
          {:note 3
           :velocity-f 0.5})

        (should-have-invoked :play {:with [:buf :buf-2
                                           :velocity-f 0.6]
                                    :times 1})
        (should-have-invoked :play {:with [:buf :buf-3
                                           :velocity-f 0.5]
                                    :times 1})))))

  (describe "midi-perc-player"
    (describe "when the down event is emitted"
      (it "should play the associated player function"
        (midi-perc-player {2 (stub :play-2)
                           3 (stub :play-3)})

        (sync-event
          [:midi :pad :down]
          {:note 2
           :velocity-f 0.6})

        (sync-event
          [:midi :pad :down]
          {:note 3
           :velocity-f 0.5})

        (should-have-invoked :play-2 {:with [:velocity-f 0.6]
                                      :times 1})
        (should-have-invoked :play-3 {:with [:velocity-f 0.5]
                                      :times 1}))))

  (describe "midi-mono-player"
    (describe "when a key is pressed"
      (it "should control the active node if the node is active"
        (midi-mono-player
          (stub :play {:invoke (fn [_ _ _ _ _ _ _ _] (next-id :fake-node))}))

        (sync-event
              [:midi :key :down]
              {:note 60
               :velocity-f 0.5})

        (with-redefs [node-active? (stub :node-active? {:return true})]
          (should-invoke
            ctl
            {:with [0
                    :note 61
                    :freq (midi->hz 61)
                    :velocity-f 0.5
                    :gate 1]
             :times 1}
            (sync-event
              [:midi :key :down]
              {:note 61
               :velocity-f 0.5})))))

    (describe "when a key is released"
      (it "should zeroize the gate if it the node is active"
        (midi-mono-player
          (stub :play {:invoke (fn [_ _ _ _ _ _ _ _] (next-id :fake-node))}))

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (with-redefs [node-active? (stub :node-active? {:return true})]
          (should-invoke
            ctl
            {:with [0 :gate 0]
             :times 1}
            (sync-event
              [:midi :key :up]
              {:note 60
               :velocity-f 0.5}))))

      (it "should not zeroize the gate of the node is inactive"
        (midi-mono-player
          (stub :play {:invoke (fn [_ _ _ _ _ _ _ _] (next-id :fake-node))}))

        (with-redefs [node-active? (stub :node-active? {:return false})]
          (sync-event
            [:midi :key :down]
            {:note 60
             :velocity-f 0.5})

          (should-not-invoke
            ctl
            (sync-event
              [:midi :key :up]
              {:note 60
               :velocity-f 0.5})))))))
