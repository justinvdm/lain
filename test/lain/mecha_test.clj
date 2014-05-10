(ns lain.mecha-test
  (:require [overtone.sc.machinery.allocator :refer :all]
            [overtone.sc.bus :refer :all]
            [overtone.sc.node :refer :all]
            [overtone.sc.buffer :refer :all]
            [overtone.sc.synth :refer :all]
            [overtone.studio.inst :refer :all]
            [speclj.core :refer :all]
            [lain.mecha :refer :all]))


(describe "mecha"
  (describe "stop"
    (it "should free audio busses"
      (let [b (audio-bus)]
        (should-invoke
          free-id
          {:with [:audio-bus (:id b) 1] :times 1}
          (stop b))))

    (it "should free control busses"
      (let [b (control-bus)]
        (should-invoke
          free-id
          {:with [:control-bus (:id b) 1] :times 1}
          (stop b))))

    (it "should free buffers"
      (let [b (buffer 1)]
        (should-be buffer-live? b)
        (stop b)
        (should-not-be buffer-live? b)))

    (it "should not try free dead buffers"
      (let [b (buffer 1)]
        (buffer-free b)
        (should-not-invoke buffer-free (stop b))))

    (it "should kill live nodes"
      (let [n ((inst 0))]
        (Thread/sleep 10)
        (should-be node-live? n)
        (stop n)
        (Thread/sleep 10)
        (should-not-be node-live? n)))

    (it "should kill loading nodes"
      (let [n ((inst 0))]
        (should-be node-loading? n)
        (stop n)
        (Thread/sleep 10)
        (should-not-be node-live? n)
        (should-not-be node-loading? n)))

    (it "should not try kill dead nodes"
      (let [n ((inst 0))]
        (kill n)
        (Thread/sleep 10)
        (should-not-invoke kill (stop n))))))
