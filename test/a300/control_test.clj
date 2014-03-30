(ns lain.a300.control-test
  (:require [speclj.core :refer :all]
            [overtone.sc.bus :refer [control-bus-set!]]
            [overtone.libs.counters :refer [next-id
                                            reset-counter!]]
            [overtone.libs.event :refer [event
                                         remove-event-handler]]
            [lain.a300.control :refer :all]))

(def removed-handlers (atom []))
(def bus-a (atom 0))
(def bus-b (atom 0))

(describe "control"

  (around [it]
    [(with-redefs
       [control-bus-set! #(reset! %1 %2)]
       (it))
     (remove-all-controllers)
     (reset-counter! :controller)
     (reset! bus-a 0)
     (reset! bus-b 0)])

  (describe "bus-controller"

    (describe "when the event is emitted"

      (it "should change the value of the bus to the event's value"
        (bus-controller bus-a [:event-a])

        (event
          [:event-a]
          {:value-f 0.8})
        (Thread/sleep 20)

        (should= @bus-a 0.8))

      (it "should map the event's value to the given extent"
        (bus-controller bus-a [:event-a] :extent [0 100])

        (event
          [:event-a]
          {:value-f 0.8})
        (Thread/sleep 20)

        (should= @bus-a 80.0))

      (it "should pass the value through the given modifier function"
        (bus-controller bus-a [:event-a] :modifier inc)

        (event
          [:event-a]
          {:value-f 0.8})
        (Thread/sleep 20)

        (should= @bus-a 1.8))))
    

  (describe "remove-controller"

    (around [it]
      [(reset! removed-handlers [])
       (with-redefs
         [remove-event-handler #(swap! removed-handlers conj %&)]
         (it))])

    (it "should stop listening to the event associated with the controller"
      (remove-controller (bus-controller bus-a [:event-a]))
      (should-contain [[:controller :event-a]] @removed-handlers)))

  (describe "remove-all-controllers"

    (around [it]
      [(reset! removed-handlers [])
       (with-redefs [remove-event-handler #(swap! removed-handlers conj %&)]
         (it))])

    (it "should stop listening to events for all controllers"
      (bus-controller bus-a [:event-a])
      (bus-controller bus-b [:event-b])
      (remove-all-controllers)
      (should-contain [[:controller :event-a]] @removed-handlers)
      (should-contain [[:controller :event-b]] @removed-handlers))))
