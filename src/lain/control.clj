(ns lain.control
  (:require [overtone.sc.node :refer :all]
            [overtone.sc.bus :refer :all]
            [overtone.libs.event :refer :all]
            [lain.utils :refer [lin-interpolator]]
            [lain.play :refer [ctl-player]]
            [lain.mecha :refer [defmecha switch]]))


(defmecha controller [event-type
                      & [controller-fn (fn [_])
                         extent [0 1]
                         modifier identity]]

  (:start [interpolator (lin-interpolator [0 1] extent)
           event-key (concat [:controller] event-type)]

          (on-event
            event-type
            (fn [{value-f :value-f}]
              (let
                [value (interpolator value-f)
                 value (modifier value)]
                (controller-fn value)))
            event-key))

  (:stop (remove-event-handler event-key)))


(defmecha bus-controller [event-type
                          bus
                          & [extent [0 1]
                             modifier identity]]

  (:start [super
           (controller
             event-type
             :extent extent
             :modifier modifier
             :controller-fn
             (fn [value-f] (control-bus-set! bus value-f)))]))


(defmecha param-controller [event-type
                            node-id
                            param-key
                            & [extent [0 1]
                               modifier identity]]

  (:start [super
           (controller
             event-type
             :extent extent
             :modifier modifier
             :controller-fn
             (fn [value-f] (ctl node-id param-key value-f)))]))


(defmecha player-param-controller [event-type
                                   player-instnc
                                   param-key
                                   & [extent [0 1]
                                      modifier identity]]

  (:start [super
           (controller
             event-type
             :extent extent
             :modifier modifier
             :controller-fn
             (fn [value-f] (ctl-player player-instnc param-key value-f)))]))


(defmecha switch-controller [event-type
                             mechas
                             & [initial 0
                                extent [0 1]
                                modifier identity]]

  (:start [n (count mechas)
           switcher (switch mechas)

           super
           (controller
             event-type
             :extent extent
             :modifier modifier
             :controller-fn #(switcher (int (* % n))))]

          (if-not (nil? initial)
            (switcher initial))

          {:switcher switcher}))
