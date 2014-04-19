(ns lain.a300.control-test
  (:require [speclj.core :refer :all]
            [overtone.sc.node :refer [ctl]]
            [overtone.sc.bus :refer [control-bus-set!]]
            [overtone.libs.counters :refer [next-id
                                            reset-all-counters!]]
            [overtone.libs.event :refer [sync-event
                                         remove-event-handler]]
            [lain.a300.play :refer [ctl-player]]
            [lain.a300.control :refer :all]))

(describe "control"
  (with-stubs)

  (before
    [(remove-all-controllers)
     (reset-all-counters!)])

  (describe "controller"
    (describe "when the event is emitted"
      (it "invoke the controller function"
        (controller [:event-a]
                    :controller-fn (stub :controller-fn))

        (sync-event
          [:event-a]
          {:value-f 0.8})

        (should-have-invoked :controller-fn {:with [0.8]
                                             :times 1}))

      (it "should map the event's value to the given extent"
        (controller [:event-a]
                    :controller-fn (stub :controller-fn)
                    :extent [0 100])

        (sync-event
          [:event-a]
          {:value-f 0.8})

        (should-have-invoked :controller-fn {:with [80.0]
                                             :times 1}))

      (it "should pass the value through the given modifier function"
        (controller [:event-a]
                    :controller-fn (stub :controller-fn)
                    :modifier inc)

        (sync-event
          [:event-a]
          {:value-f 0.8})

        (should-have-invoked :controller-fn {:with [1.8]
                                             :times 1}))))

  (describe "bus-controller"
    (describe "when the event is emitted"
      (it "should change the value of the bus to the event's value"
        (bus-controller :bus-a [:event-a])

        (should-invoke
          control-bus-set!
          {:with [:bus-a 0.8]}
          (sync-event
            [:event-a]
            {:value-f 0.8})))))

  (describe "param-controller"
    (describe "when the event is emitted"
      (it "should control the node's param"
        (param-controller :synth-a :param-a [:event-a])

        (should-invoke
          ctl
          {:with [:synth-a :param-a 0.8]}
          (sync-event
            [:event-a]
            {:value-f 0.8})))))

  (describe "player-param-controller"
    (describe "when the event is emitted"
      (it "should control the node's param"
        (player-param-controller :player-a :param-a [:event-a])

        (should-invoke
          ctl-player
          {:with [:player-a :param-a 0.8]}
          (sync-event
            [:event-a]
            {:value-f 0.8})))))

  (describe "mode-controller"
    (describe "when the event is emitted"
      (it "should switch the mode"
        (let [record (atom [])
              modes {0 {:start #(swap! record conj "0:start")
                        :end   #(swap! record conj "0:end")}

                     1 {:start #(swap! record conj "1:start")
                        :end   #(swap! record conj "1:end")}}]

          (mode-controller [:event-a] modes)

          (sync-event
            [:event-a]
            {:value-f 0.1})

          (sync-event
            [:event-a]
            {:value-f 0.3})

          (sync-event
            [:event-a]
            {:value-f 0.4})

          (sync-event
            [:event-a]
            {:value-f 0.5})

          (sync-event
            [:event-a]
            {:value-f 0.8})

          (sync-event
            [:event-a]
            {:value-f 0.2})

          (should= ["0:start"
                    "0:end"
                    "1:start"
                    "1:end"
                    "0:start"] @record)))))

  (describe "remove-controller"
    (it "should stop listening to the event associated with the controller"
      (should-invoke
        remove-event-handler
        {:with [[:controller :event-a]]}
        (remove-controller (controller [:event-a])))))

  (describe "remove-all-controllers"
    (it "should stop listening to events for all controllers"
      (controller [:event-a])
      (controller [:event-b])

      (should-invoke
        remove-event-handler
        {:with [[:controller :event-a]] :times 1}
        {:with [[:controller :event-b]] :times 1}
        (remove-all-controllers)))))
