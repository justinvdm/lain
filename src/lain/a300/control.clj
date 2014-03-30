(ns lain.a300.control
  (:require [overtone.sc.node :refer [ctl]]
            [overtone.sc.bus :refer [control-bus-set!]]
            [overtone.libs.counters :refer [next-id]]
            [overtone.libs.event :refer [on-event
                                         remove-event-handler]]
            [lain.utils :refer [lin-interpolator]]
            [lain.a300.play :refer [ctl-player]]))

(defonce controllers (atom {}))


(defn controller [event-type
                  & {:keys [controller-fn
                            extent
                            modifier]
                     :or {controller-fn (fn [_] ())
                          extent [0 1]
                          modifier identity}}]
  (let [interpolator (lin-interpolator [0 1] extent)
        event-key (concat [:controller] event-type)
        controller-id (next-id :controller)
        controller-data {:event-key event-key}]

    (on-event
      event-type
      (fn
        [{value-f :value-f}]
        (let
          [value (interpolator value-f)
           value (modifier value)]
          (controller-fn value)))
      event-key)

    (swap! controllers assoc controller-id controller-data)
    controller-id))


(defn bus-controller [bus
                      event-type
                      & {:keys [extent modifier]
                         :or {extent [0 1]
                              modifier identity}}]
  (controller event-type
              :extent extent
              :modifier modifier

              :controller-fn (fn [value-f] (control-bus-set! bus value-f))))


(defn param-controller [node-id
                        param-key
                        event-type
                        & {:keys [extent modifier]
                           :or {extent [0 1]
                                modifier identity}}]
  (controller event-type
              :extent extent
              :modifier modifier

              :controller-fn
              (fn [value-f] (ctl node-id param-key value-f))))


(defn player-param-controller [player-id
                               param-key
                               event-type
                               & {:keys [extent modifier]
                                  :or {extent [0 1]
                                       modifier identity}}]
  (controller event-type
              :extent extent
              :modifier modifier

              :controller-fn
              (fn [value-f]
                (ctl-player player-id param-key value-f))))


(defn remove-controller [controller-id]
  (let
    [{event-key :event-key} (get @controllers controller-id)]
    (remove-event-handler event-key)
    (swap! controllers dissoc controller-id)))


(defn remove-all-controllers []
  (doseq
    [[controller-id controller] @controllers] 
    (remove-controller controller-id)))
