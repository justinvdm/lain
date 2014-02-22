(ns lain.a300.play-test
  (:require [speclj.core :refer :all]
            [overtone.sc.node :refer [ctl]]
            [overtone.libs.counters :refer [next-id
                                            reset-counter!]]
            [overtone.libs.event :refer [event
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

      (it "should play the player function"
        (midi-key-player play)

        (event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})

        (event
          [:midi :key :down]
          {:note 61
           :velocity-f 0.5})

        (Thread/sleep 20)

        (should= @plays [[:note 60
                          :freq (midi->hz 60)
                          :velocity-f 0.5]
                         [:note 61
                          :freq (midi->hz 61)
                          :velocity-f 0.5]])))

    (describe "when a key is released"

      (it "should zeroize the gate of the player function for that note"
        (midi-key-player play)

        (event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})
        (Thread/sleep 20)

        (event
          [:midi :key :down]
          {:note 61
           :velocity-f 0.5})
        (Thread/sleep 20)

        (event
          [:midi :key :up]
          {:note 61
           :velocity-f 0.5})
        (Thread/sleep 20)

        (should= @ctls [[1 :gate 0]])))

  (describe "bend-midi-keys"

    (it
      "should bend the key player's active notes"
      (let [player-id (midi-key-player play)]
        (event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})
        (Thread/sleep 20)

        (event
          [:midi :key :down]
          {:note 61
           :velocity-f 0.5})
        (Thread/sleep 20)

        (bend-midi-keys player-id 2)
        (Thread/sleep 20)

        (should= @ctls [[1
                         :note 63
                         :freq (midi->hz 63)]
                        [0
                         :note 62
                         :freq (midi->hz 62)]])))

    (it "should bend new notes played by the key player"
      (let [player-id (midi-key-player play)]
        (bend-midi-keys player-id 2)
        (Thread/sleep 20)

        (event
          [:midi :key :down]
          {:note 60
           :velocity-f 0.5})
        (Thread/sleep 20)

        (event
          [:midi :key :down]
          {:note 61
           :velocity-f 0.5})
        (Thread/sleep 20)

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
      (should-contain [[:midi-key-player :bar :down]] @removed-handlers))))


  (describe "buf-player"

    (describe "when event is emitted"

      (it "should play the function"
        (buf-player play {3 "buf-3"})

        (event
          [:midi :pad :down]
          {:note 3
           :velocity-f 0.5})

        (Thread/sleep 20)

        (should= @plays [[:buf "buf-3"
                          :velocity-f 0.5]]))))

  (describe "remove-buf-player"

    (around [it]
      [(reset! removed-handlers [])
       (with-redefs [remove-event-handler #(swap! removed-handlers conj %&)]
         (it))
       (remove-all-buf-players)])

    (it "should stop listening to events associated with the player"
      (remove-buf-player (buf-player play {}))
      (should-contain [[:buf-player :midi :pad :down]] @removed-handlers))

  (describe "remove-all-buf-players"

    (around [it]
      [(remove-all-buf-players)
       (reset! removed-handlers [])
       (with-redefs [remove-event-handler #(swap! removed-handlers conj %&)]
         (it))])

    (it "should stop listening to events for all players"
      (buf-player play {} :event-type [:foo])
      (buf-player play {} :event-type [:bar])
      (remove-all-buf-players)
      (should-contain [[:buf-player :foo]] @removed-handlers)
      (should-contain [[:buf-player :bar]] @removed-handlers)))))
