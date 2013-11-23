(defproject lain "0.0.0"
  :description "Serial experiments with overtone"
  :url "https://github.com/justinvdm/lain"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [overtone "0.8.1" :exclusions [org.clojure/clojure]]]
  :profiles {:dev {:dependencies [[speclj "2.7.5"]]}}
  :plugins [[speclj "2.7.5"]]
  :test-paths ["test"])
