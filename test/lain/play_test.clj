(ns lain.play-test
  (:require [speclj.core :refer :all]
            [overtone.sc.node :refer :all]
            [overtone.libs.event :refer :all]
            [overtone.libs.counters :refer :all]
            [overtone.music.pitch :refer :all]
            [lain.test-init]
            [lain.play :refer :all]
            [lain.mecha :as mecha]))


(defn next-fake-node [& _]
  (next-id :fake-node))


(describe "play"
  (with-stubs)

  (before
    (reset-all-counters!))

  (describe "bend-midi-keys"
    (it "should bend the key player's active notes"
      (let [play (stub :play {:invoke next-fake-node})
            p (key-player play)]

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

          (bend-midi-keys p 2))

        (mecha/stop p)))

    (it "should bend new notes played by the key player"
      (let [p (key-player (stub :play))]
        (bend-midi-keys p 2)

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
                                    :times 1})

        (mecha/stop p))))


  (describe "ctl-player"
    (it "should control the player's active notes"
      (let [p (player :test-player
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
            (ctl-player p :foo 23)))

        (mecha/stop p)))

    (it "should use the given params for newly played notes"
      (let [p (player :test-player
                      :down (stub :down))]
        (ctl-player p :foo 23)

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
                                    :times 1})

        (mecha/stop p))))

(describe "player"
  (describe "when a key is pressed"
    (it "should handle the event"
      (let [p (player :test-player :down (stub :down))]

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
                                    :times 1})
        (mecha/stop p)))

    (it "should not handle the event if the key is already pressed"
      (let [p (player :test-player :down (stub :down))]

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (should-have-invoked :down {:times 1})

        (mecha/stop p)))

    (it "should not handle the event if the key belongs to another device"
      (let [p (player :test-player
                      :device "device-foo"
                      :down (stub :down))]

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

        (should-have-invoked :down {:times 1})

        (mecha/stop p)))

    (it "should not handle the event if the key belongs to another channel"
      (let [p (player :test-player
                      :channel 1
                      :down (stub :down))]

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

        (should-have-invoked :down {:times 1})

        (mecha/stop p))))

  (describe "when a key is released"
    (it "should handle the event"
      (let [p (player
                :test-player
                :down (stub :down {:return true})
                :up (stub :up))]

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
                                  :times 1})

        (mecha/stop p)))

    (it "should ignore the event if no corresponding down event has happened"
      (let [p (player :test-player :up (stub :up))]

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

        (should-have-invoked :up {:times 1})

        (mecha/stop p)))

    (it "should ignore the event if it belongs to another device"
      (let [p (player
                :test-player
                :device "device-foo"
                :up (stub :up))]

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

        (should-have-invoked :up {:times 1})

        (mecha/stop p)))

    (it "should ignore the event if it belongs to another channel"
      (let [p (player
                :test-player
                :channel 1
                :up (stub :up))]

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

        (should-have-invoked :up {:times 1})

        (mecha/stop p)))))

(describe "key-player"
  (describe "when a key is pressed"
    (it "should play the player function"
      (let [p (key-player (stub :play))]

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
                                    :times 1})

        (mecha/stop p))))

  (describe "when a key is released"
    (it "should zeroize the gate of the player function for that note"
      (let [p (key-player (stub :play {:invoke next-fake-node}))]

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
               :velocity-f 0.5}))


          (mecha/stop p)))))

  (it "should not zeroize the gate of the note is inactive"
    (let [p (key-player (stub :play {:invoke next-fake-node}))]

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
             :velocity-f 0.5}))

        (mecha/stop p)))))

(describe "buf-player"
  (describe "when the down event is emitted"
    (it "should play the player function"
      (let [p (buf-player (stub :play) {2 :buf-2
                                        3 :buf-3})]

        (sync-event
          [:midi :key :down]
          {:note 2
           :velocity-f 0.6})

        (sync-event
          [:midi :key :down]
          {:note 3
           :velocity-f 0.5})

        (should-have-invoked :play {:with [:buf :buf-2
                                           :velocity-f 0.6]
                                    :times 1})
        (should-have-invoked :play {:with [:buf :buf-3
                                           :velocity-f 0.5]
                                    :times 1})

        (mecha/stop p))))

  (describe "when the up event is emitted"
    (it "should zeroize the gate of the player function for that note"
      (let [p (buf-player (stub :play {:invoke next-fake-node}) {2 :buf-2
                                                                 3 :buf-3})]

        (with-redefs [node-active? (stub :node-active? {:return true})]
          (should-invoke
            ctl
            {:with [1 :gate 0]
             :times 1}
            (sync-event
              [:midi :key :down]
              {:note 2
               :velocity-f 0.6})

            (sync-event
              [:midi :key :down]
              {:note 3
               :velocity-f 0.5})

            (sync-event
              [:midi :key :up]
              {:note 3
               :velocity-f 0.5})))

        (mecha/stop p)))))

(describe "perc-player"
  (describe "when the down event is emitted"
    (it "should play the associated player function"
      (let [p (perc-player {2 (stub :play-2)
                            3 (stub :play-3)})]

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
                                      :times 1})

        (mecha/stop p)))))

(describe "mono-player"
  (describe "when a key is pressed"
    (it "should control the active node if the node is active"
      (let [p (mono-player (stub :play {:invoke next-fake-node}))]

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
               :velocity-f 0.5})))

        (mecha/stop p))))

  (describe "when a key is released"
    (it "should zeroize the gate if it the node is active"
      (let [p (mono-player (stub :play {:invoke next-fake-node}))]

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
               :velocity-f 0.5})))

        (mecha/stop p)))

    (it "should not zeroize the gate of the node is inactive"
      (let [p (mono-player (stub :play {:invoke next-fake-node}))]

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
               :velocity-f 0.5})))

        (mecha/stop p))))))
