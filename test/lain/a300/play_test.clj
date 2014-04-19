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


(defn next-fake-node [& _]
  (next-id :fake-node))


(describe "play"
  (with-stubs)

  (before
    [(remove-all-players)
     (reset-all-counters!)])

  (describe "player"
    (it "should invoke its setup hook"
      (player :test-player :setup (stub :setup {:invoke identity}))
      (should-have-invoked :setup {:times 1}))

    (describe "when a key is pressed"
      (it "should handle the event"
        (player :test-player :down (stub :down))

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (sync-event
          [:midi :key :down]
          {:note 61
           :velocity-f 0.5})

        (should-have-invoked :down {:with [{:note 60
                                            :velocity-f 0.5}
                                           []]
                                    :times 1})

        (should-have-invoked :down {:with [{:note 61
                                            :velocity-f 0.5}
                                           []]
                                    :times 1}))

      (it "should not handle the event if the key is already pressed"
        (player :test-player :down (stub :down))

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
        (player :test-player
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
        (player :test-player
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
        (player
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
        (player :test-player :up (stub :up))

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
        (player
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
        (player
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

(describe "ctl-player"
  (it "should control the player's active notes"
    (let [player-id (player :test-player
                            :down (stub :down {:invoke next-fake-node}))]
      (with-redefs [node-active?
                    (stub :node-active? {:invoke boolean})]

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
          {:with [0 :foo 23]
           :times 1}
          {:with [1 :foo 23]
           :times 0}
          (ctl-player player-id :foo 23)))))

  (it "should use the given params for newly played notes"
    (let [player-id (player :test-player
                            :down (stub :down))]
      (ctl-player player-id :foo 23)

      (sync-event
        [:midi :key :down]
        {:note 60
         :velocity-f 0.5})

      (sync-event
        [:midi :key :down]
        {:note 61
         :velocity-f 0.5})

      (should-have-invoked :down {:with [{:note 60
                                          :velocity-f 0.5}
                                         [:foo 23]]
                                  :times 1})

      (should-have-invoked :down {:with [{:note 61
                                          :velocity-f 0.5}
                                         [:foo 23]]
                                  :times 1}))))

(describe "remove-player"
  (it "should invoke the player's teardown hook"
    (remove-player (player :test-player :teardown (stub :teardown)))
    (should-have-invoked :teardown {:times 1}))

  (it "should stop listening to key press events associated with the player"
    (should-invoke
      remove-event-handler
      {:with [[:test-player :midi :key :down]]}
      (remove-player (player :test-player))))

  (it "should stop listening to key release events associated with the player"
    (should-invoke
      remove-event-handler
      {:with [[:test-player :midi :key :up]]}
      (remove-player (player :test-player)))))

(describe "remove-all-players"
  (it "should remove all midi players"
    (player :foo)
    (player :bar)

    (should-invoke
      remove-player
      {:with [0] :times 1}
      {:with [1] :times 1}
      (remove-all-players))))

(describe "key-player"
  (describe "when a key is pressed"
    (it "should play the player function"
      (key-player (stub :play))

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
      (key-player
        (stub :play {:invoke next-fake-node}))

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
      (key-player
        (stub :play {:invoke next-fake-node}))

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
             :velocity-f 0.5}))))))

  (describe "bend-midi-keys"
    (it "should bend the key player's active notes"
      (let [play (stub :play {:invoke next-fake-node})
            player-id (key-player play)]

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
      (let [player-id (key-player (stub :play))]
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

(describe "buf-player"
  (describe "when the down event is emitted"
    (it "should play the player function"
      (buf-player (stub :play) {2 :buf-2
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
                                  :times 1})))

  (describe "when the up event is emitted"
    (it "should zeroize the gate of the player function for that note"
      (buf-player
        (stub :play {:invoke next-fake-node}) {2 :buf-2
                                               3 :buf-3})

      (with-redefs [node-active? (stub :node-active? {:return true})]
        (should-invoke
          ctl
          {:with [1 :gate 0]
           :times 1}
          (sync-event
            [:midi :pad :down]
            {:note 2
             :velocity-f 0.6})

          (sync-event
            [:midi :pad :down]
            {:note 3
             :velocity-f 0.5})

          (sync-event
            [:midi :pad :up]
            {:note 3
             :velocity-f 0.5}))))))

(describe "perc-player"
  (describe "when the down event is emitted"
    (it "should play the associated player function"
      (perc-player {2 (stub :play-2)
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

(describe "mono-player"
  (describe "when a key is pressed"
    (it "should control the active node if the node is active"
      (mono-player
        (stub :play {:invoke next-fake-node}))

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
      (mono-player
        (stub :play {:invoke next-fake-node}))

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
      (mono-player
        (stub :play {:invoke next-fake-node}))

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
