(ns lain.utils-test
  (:require [speclj.core :refer :all]
            [overtone.sc.sample :refer [load-samples]]
            [lain.utils :refer :all]))

(describe "utils"
  (with-stubs)

  (describe "lin-interpolator"

      (it "should make a linear interpolator"
        (let
          [lerp (lin-interpolator [2 10] [12 16])]
          (should= (lerp 2) 12)
          (should= (lerp 10) 16)
          (should= (lerp 4) 13)
          (should= (lerp 6) 14))))

  (describe "mode-switcher"
    (def record (atom []))

    (defn make-switcher []
      (mode-switcher {0 {:start #(swap! record conj "0:start")
                         :end   #(swap! record conj "0:end")}

                      1 {:start #(swap! record conj "1:start")
                         :end   #(swap! record conj "1:end")}

                      2         #(swap! record conj "2:start")

                      3 {:start #(swap! record conj "3:start")
                         :end   #(swap! record conj "3:end")}}))

    (before
      [(reset! record [])])
    
    (it "should allow start and end hooks to be given for any mode"
      (let [switch (make-switcher)]
        (switch 0)
        (switch 3)
        (switch 1)
        (should= @record ["0:start"
                          "0:end"
                          "3:start"
                          "3:end"
                          "1:start"])))

    (it "should allow just a start hook to be given for any mode"
      (let [switch (make-switcher)]
        (switch 0)
        (switch 2)
        (switch 1)
        (should= @record ["0:start"
                          "0:end"
                          "2:start"
                          "1:start"])))
    
    (describe "the returned switching function"
      (it "should convert the given mode number to an int"
        (let [switch (make-switcher)]
          (switch 0.3)
          (switch 1.2)
          (should= @record ["0:start"
                            "0:end"
                            "1:start"])))

      (it "should clamp given mode number between 0 and the mode count"
        (let [switch (make-switcher)]
          (switch -1.3)
          (switch 8.3)
          (should= @record ["0:start"
                            "0:end"
                            "3:start"])))

      (describe "when a different mode from the current is requested"
        (it "stop the current mode, then start the new one"
          (let [switch (make-switcher)]
            (switch 0)
            (switch 1)
            (should= @record ["0:start"
                              "0:end"
                              "1:start"]))))

      (describe "when the same mode to the current is requested"
        (it "should not invoke any hooks"
          (let [switch (make-switcher)]
            (switch 0)
            (switch 0)
            (should= @record ["0:start"]))))))

  (describe "load-note-samples"
    (it "should load the samples keyed by midi notes"
      (with-redefs [load-samples
                    (stub :load-samples {:return [{:name "c1.wav"}
                                                  {:name "c2.wav"}]})]
        (should= (load-note-samples "c1.wav" "c2.wav")
          {24 {:name "c1.wav"}
           36 {:name "c2.wav"}}))

        (should-have-invoked :load-samples {:times 1
                                            :with ["c1.wav" "c2.wav"]})))

    (it "should support files named by midi notes"
      (with-redefs [load-samples
                    (stub :load-samples {:return [{:name "60.wav"}
                                                  {:name "61.wav"}]})]
        (should= (load-note-samples "c1.wav" "c2.wav")
          {60 {:name "60.wav"}
           61 {:name "61.wav"}}))))
