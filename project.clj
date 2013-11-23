(defproject lain "0.0.0"
  :description "Serial experiments with overtone"
  :url "https://github.com/justinvdm/lain"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [overtone "0.8.1"]]
  :profiles {:dev {:dependencies [[speclj "2.7.5"]]}}
  :plugins [[speclj "2.7.5"]]
  :test-paths ["test"])
