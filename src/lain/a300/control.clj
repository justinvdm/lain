(ns lain.a300.control
  (:require [overtone.sc.node :refer [ctl]]
            [overtone.sc.bus :refer [control-bus-set!]]
            [overtone.libs.event :refer [on-event
                                         remove-event-handler]]
            [mecha.core :refer [defmecha]]
            [lain.utils :refer [lin-interpolator
                                mode-switcher]]
            [lain.a300.play :refer [ctl-player]]))


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


(defmecha mode-controller [event-type
                           modes
                           & [first-mode 0
                              extent [0 1]
                              modifier identity]]

  (:start [n (count modes)
           switcher (mode-switcher modes)

           super
           (controller
             event-type
             :extent extent
             :modifier modifier
             :controller-fn #(switcher (* % n)))]

          (if-not (nil? first-mode)
            (switcher first-mode))))
