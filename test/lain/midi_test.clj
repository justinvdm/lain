(ns lain.midi-test
  (:require [speclj.core :refer :all]
            [overtone.sc.node :refer [ctl]]
            [overtone.libs.counters :refer [next-id
                                            reset-counter!]]
            [overtone.libs.event :refer [event
                                         remove-handler]]
            [overtone.music.pitch :refer [midi->hz]]
            [lain.midi :refer :all]))

(def ctls (atom []))
(def plays (atom []))
(def removed-handlers (atom []))

(defn play [& args]
  (swap! plays conj args)
  (next-id ::node))

(describe
  "midi"

  (after
    [(reset-counter! :play-midi-keys)])

  (describe
    "play-midi-keys"

    (around [it]
      [(reset! ctls [])
       (reset! plays [])
       (with-redefs [ctl #(swap! ctls conj %&)]
         (it))
       (remove-all-players)
       (reset-counter! ::node)])

    (describe
      "when a key is pressed"

      (it
        "should play the player function"
        (let [player-id (play-midi-keys play)]
          (event
            [:midi :key :down]
            {:note 60
             :velocity-f 0.5})

          (event
            [:midi :key :down]
            {:note 61
             :velocity-f 0.5})

          (Thread/sleep 50)

          (should= @plays [[:note 60
                            :freq (midi->hz 60)
                            :velocity-f 0.5]
                           [:note 61
                            :freq (midi->hz 61)
                            :velocity-f 0.5]]))))

    (describe
      "when a key is released"

      (it
        "should zeroize the gate of the player function for that note"
        (let [player-id (play-midi-keys play)]
          (event
            [:midi :key :down]
            {:note 60
             :velocity-f 0.5})
          (Thread/sleep 50)

          (event
            [:midi :key :down]
            {:note 61
             :velocity-f 0.5})
          (Thread/sleep 50)

          (event
            [:midi :key :up]
            {:note 61
             :velocity-f 0.5})
          (Thread/sleep 50)

          (should= @ctls [[1 :gate 0]]))))

    (describe
      "when the bender is bent"

      (it
        "should bend the notes currently being played"
        (let [player-id (play-midi-keys play)]
          (event
            [:midi :key :down]
            {:note 60
             :velocity-f 0.5})
          (Thread/sleep 50)

          (event
            [:midi :key :down]
            {:note 61
             :velocity-f 0.5})
          (Thread/sleep 50)

          (event
            [:midi :bend]
            {:bending-f 0.5})
          (Thread/sleep 50)

          (should= @ctls [[1
                           :note 63
                           :freq (midi->hz 63)]
                          [0
                           :note 62
                           :freq (midi->hz 62)]])))

      (it
        "should bend newly played notes"
        (let [player-id (play-midi-keys play)]
          (event
            [:midi :bend]
            {:bending-f 0.5})
          (Thread/sleep 50)

          (event
            [:midi :key :down]
            {:note 60
             :velocity-f 0.5})
          (Thread/sleep 50)

          (event
            [:midi :key :down]
            {:note 61
             :velocity-f 0.5})
          (Thread/sleep 50)

          (should= @plays [[:note 62
                            :freq (midi->hz 62)
                            :velocity-f 0.5]
                           [:note 63
                            :freq (midi->hz 63)
                            :velocity-f 0.5]])))))

  (describe
    "remove-player"

    (around [it]
      [(reset! removed-handlers [])
       (with-redefs [remove-handler #(swap! removed-handlers conj %&)]
         (it))
       (remove-all-players)])

    (it
      "should stop listening to key press events associated with the player"
      (remove-player (play-midi-keys play))
      (should-contain [[:play-midi-keys :midi :key :down]] @removed-handlers))

    (it
      "should stop listening to key release events associated with the player"
      (remove-player (play-midi-keys play))
      (should-contain [[:play-midi-keys :midi :key :up]] @removed-handlers))

    (it
      "should stop listening to bend events associated with the player"
      (remove-player (play-midi-keys play))
      (should-contain [[:play-midi-keys :midi :bend]] @removed-handlers)))

  (describe
    "remove-all-players"

    (around [it]
      [(remove-all-players)
       (reset! removed-handlers [])
       (with-redefs [remove-handler #(swap! removed-handlers conj %&)]
         (it))])

    (it
      "should stop listening to key press events for all players"
      (play-midi-keys play :up-event [:foo :down])
      (play-midi-keys play :up-event [:bar :down])
      (remove-all-players)
      (should-contain [[:play-midi-keys :foo :down]] @removed-handlers)
      (should-contain [[:play-midi-keys :bar :down]] @removed-handlers))

    (it
      "should stop listening to key release events for all players"
      (play-midi-keys play :down-event [:foo :down])
      (play-midi-keys play :down-event [:bar :down])
      (remove-all-players)
      (should-contain [[:play-midi-keys :foo :down]] @removed-handlers)
      (should-contain [[:play-midi-keys :bar :down]] @removed-handlers))

    (it
      "should stop listening to bend events for all players"
      (play-midi-keys play :bend-event [:foo :bend])
      (play-midi-keys play :bend-event [:bar :bend])
      (remove-all-players)
      (should-contain [[:play-midi-keys :foo :bend]] @removed-handlers)
      (should-contain [[:play-midi-keys :bar :bend]] @removed-handlers))))
