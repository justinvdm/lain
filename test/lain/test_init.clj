(ns lain.test-init
  (:require [overtone.core :refer :all]))


(if-not (or (server-connected?)
            (server-connecting?))
  (boot-external-server))
