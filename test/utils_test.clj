(ns lain.utils-test
  (:require
    [speclj.core :refer :all]
    [lain.utils :refer :all]))

(describe "utils"

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
            (should= @record ["0:start"])))))))
