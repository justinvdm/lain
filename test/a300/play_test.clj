(ns lain.a300.play-test
  (:require [speclj.core :refer :all]
            [overtone.sc.node :refer [ctl]]
            [overtone.libs.counters :refer [next-id
                                            reset-counter!]]
            [overtone.libs.event :refer [sync-event
                                         remove-event-handler]]
            [overtone.music.pitch :refer [midi->hz]]
            [lain.a300.play :refer :all]))

(def ctls (atom []))
(def plays (atom []))
(def removed-handlers (atom []))

(defn play [& args]
  (swap! plays conj args)
  (next-id ::node))

(describe "play"

  (around [it]
    [(reset! ctls [])
     (reset! plays [])
     (with-redefs [ctl #(swap! ctls conj %&)]
       (it))
     (remove-all-key-players)
     (remove-all-buf-players)
     (reset-counter! ::node)
     (reset-counter! :play-midi-keys)])

  (describe "midi-key-player"

    (describe "when a key is pressed"
      (it "should play the player function if the key is not already pressed"
        (midi-key-player play)

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (sync-event
          [:midi :key :down]
          {:note 61
           :velocity-f 0.5})

        (should= @plays [[:note 60
                          :freq (midi->hz 60)
                          :velocity-f 0.5]
                         [:note 61
                          :freq (midi->hz 61)
                          :velocity-f 0.5]]))

      (it "should not play the player function if the key is already pressed"
        (midi-key-player play)

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (should= 1 (count @plays)))

      (it "should not play the player function if the key belongs to another device"
        (midi-key-player play :device "device-foo")

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

        (should= 1 (count @plays)))))

    (describe "when a key is released"

      (it "should zeroize the gate of the player function for that note"
        (midi-key-player play)

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

        (should= @ctls [[1 :gate 0]]))

      (it "should ignore the key if it belongs to another device"
        (midi-key-player play :device "device-foo")

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
           :device {:name "device-bar"}})

        (should= 1 (count @ctls))))

  (describe "bend-midi-keys"

    (it
      "should bend the key player's active notes"
      (let [player-id (midi-key-player play)]
        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (sync-event
          [:midi :key :down]
          {:note 61
           :velocity-f 0.5})

        (bend-midi-keys player-id 2)

        (should= @ctls [[1
                         :note 63
                         :freq (midi->hz 63)]
                        [0
                         :note 62
                         :freq (midi->hz 62)]])))

    (it "should bend new notes played by the key player"
      (let [player-id (midi-key-player play)]
        (bend-midi-keys player-id 2)

        (sync-event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (sync-event
          [:midi :key :down]
          {:note 61
           :velocity-f 0.5})

        (should= @plays [[:note 62
                          :freq 293.6647679174076
                          :velocity-f 0.5]
                         [:note 63
                          :freq 311.1269837220809
                          :velocity-f 0.5]]))))

  (describe "remove-key-player"

    (around [it]
      [(reset! removed-handlers [])
       (with-redefs [remove-event-handler #(swap! removed-handlers conj %&)]
         (it))
       (remove-all-key-players)])

    (it "should stop listening to key press events associated with the player"
      (remove-key-player (midi-key-player play))
      (should-contain [[:midi-key-player :midi :key :down]] @removed-handlers))

    (it "should stop listening to key release events associated with the player"
      (remove-key-player (midi-key-player play))
      (should-contain [[:midi-key-player :midi :key :up]] @removed-handlers)))

  (describe "remove-all-key-players"

    (around [it]
      [(remove-all-key-players)
       (reset! removed-handlers [])
       (with-redefs [remove-event-handler #(swap! removed-handlers conj %&)]
         (it))])

    (it "should stop listening to key press events for all players"
      (midi-key-player play :up-event [:foo :down])
      (midi-key-player play :up-event [:bar :down])
      (remove-all-key-players)
      (should-contain [[:midi-key-player :foo :down]] @removed-handlers)
      (should-contain [[:midi-key-player :bar :down]] @removed-handlers))

    (it "should stop listening to key release events for all players"
      (midi-key-player play :down-event [:foo :down])
      (midi-key-player play :down-event [:bar :down])
      (remove-all-key-players)
      (should-contain [[:midi-key-player :foo :down]] @removed-handlers)
      (should-contain [[:midi-key-player :bar :down]] @removed-handlers)))


  (describe "buf-player"

    (describe "when the down event is emitted"

      (it "should only play the function once until the up event is emitted"
        (buf-player play {2 "buf-2"
                          3 "buf-3"})

        (sync-event
          [:midi :pad :down]
          {:note 2
           :velocity-f 0.6})

        (sync-event
          [:midi :pad :down]
          {:note 3
           :velocity-f 0.5})

        (sync-event
          [:midi :pad :down]
          {:note 2
           :velocity-f 0.3})

        (sync-event
          [:midi :pad :down]
          {:note 3
           :velocity-f 0.2})

        (sync-event
          [:midi :pad :up]
          {:note 2})

        (sync-event
          [:midi :pad :up]
          {:note 3})

        (sync-event
          [:midi :pad :down]
          {:note 2
           :velocity-f 0.8})

        (sync-event
          [:midi :pad :down]
          {:note 3
           :velocity-f 0.9})

        (should= @plays [[:buf "buf-2"
                          :velocity-f 0.6]
                         [:buf "buf-3"
                          :velocity-f 0.5]
                         [:buf "buf-2"
                          :velocity-f 0.8]
                         [:buf "buf-3"
                          :velocity-f 0.9]]))))

  (describe "remove-buf-player"

    (around [it]
      [(reset! removed-handlers [])
       (with-redefs [remove-event-handler #(swap! removed-handlers conj %&)]
         (it))
       (remove-all-buf-players)])

    (it "should stop listening to down events associated with the player"
      (remove-buf-player (buf-player play {}))
      (should-contain [[:buf-player :midi :pad :down]] @removed-handlers))

    (it "should stop listening to up events associated with the player"
      (remove-buf-player (buf-player play {}))
      (should-contain [[:buf-player :midi :pad :up]] @removed-handlers))

  (describe "remove-all-buf-players"

    (around [it]
      [(remove-all-buf-players)
       (reset! removed-handlers [])
       (with-redefs [remove-event-handler #(swap! removed-handlers conj %&)]
         (it))])

    (it "should stop listening to down events for all players"
      (buf-player play {} :down-event [:foo :down])
      (buf-player play {} :down-event [:bar :down])
      (remove-all-buf-players)
      (should-contain [[:buf-player :foo :down]] @removed-handlers)
      (should-contain [[:buf-player :bar :down]] @removed-handlers))

    (it "should stop listening to up events for all players"
      (buf-player play {} :up-event [:foo :up])
      (buf-player play {} :up-event [:bar :up])
      (remove-all-buf-players)
      (should-contain [[:buf-player :foo :up]] @removed-handlers)
      (should-contain [[:buf-player :bar :up]] @removed-handlers)))))
