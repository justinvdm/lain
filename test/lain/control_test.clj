(ns lain.control-test
  (:require [speclj.core :refer :all]
            [overtone.sc.node :refer [ctl]]
            [overtone.sc.bus :refer [control-bus-set!]]
            [overtone.libs.event :refer [sync-event]]
            [mecha.core :refer [mecha switch stop]]
            [lain.test-init]
            [lain.play :refer [ctl-player]]
            [lain.control :refer :all]))

(describe "control"
  (with-stubs)

  (describe "controller"
    (describe "when the event is emitted"
      (it "invoke the controller function"
        (let [c (controller [:event-a]
                            :controller-fn (stub :controller-fn))]

          (sync-event
            [:event-a]
            {:value-f 0.8})

          (should-have-invoked :controller-fn {:with [0.8]
                                               :times 1})
          (stop c)))

      (it "should map the event's value to the given extent"
        (let [c (controller [:event-a]
                            :controller-fn (stub :controller-fn)
                            :extent [0 100])]

          (sync-event
            [:event-a]
            {:value-f 0.8})

          (should-have-invoked :controller-fn {:with [80.0]
                                               :times 1})
          (stop c)))

      (it "should pass the value through the given modifier function"
        (let [c (controller [:event-a]
                            :controller-fn (stub :controller-fn)
                            :modifier inc)]
          (sync-event
            [:event-a]
            {:value-f 0.8})

          (should-have-invoked :controller-fn {:with [1.8]
                                               :times 1})
          (stop c)))))

  (describe "bus-controller"
    (describe "when the event is emitted"
      (it "should change the value of the bus to the event's value"
        (let [c (bus-controller [:event-a] :bus-a)]

          (should-invoke
            control-bus-set!
            {:with [:bus-a 0.8]}
            (sync-event
              [:event-a]
              {:value-f 0.8}))

          (stop c)))))

  (describe "param-controller"
    (describe "when the event is emitted"
      (it "should control the node's param"
        (let [c (param-controller [:event-a] :synth-a :param-a)]

          (should-invoke
            ctl
            {:with [:synth-a :param-a 0.8]}
            (sync-event
              [:event-a]
              {:value-f 0.8}))
          
          (stop c)))))

  (describe "player-param-controller"
    (describe "when the event is emitted"
      (it "should control the node's param"
        (let [c (player-param-controller [:event-a] :player-a :param-a)]

          (should-invoke
            ctl-player
            {:with [:player-a :param-a 0.8]}
            (sync-event
              [:event-a]
              {:value-f 0.8}))

          (stop c)))))

  (describe "switch-controller"
    (describe "when the event is emitted"
      (it "should switch the mode"
        (let [switches (atom [])
              mechas {0 (mecha (:start (swap! switches conj [0 :start]))
                               (:stop (swap! switches conj [0 :stop])))
                      1 (mecha (:start (swap! switches conj [1 :start]))
                               (:stop (swap! switches conj [1 :stop])))}
              c (switch-controller [:event-a] mechas)]
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

          (should= @switches [[0 :start]
                              [0 :stop]
                              [1 :start]
                              [1 :stop]
                              [0 :start]])

          (stop c))))))
