(defproject overtone-roland-a300 "0.0.0"
  :description "Wrapper events for the strange way I use my Roland A-300 
               midi keyboard in overtone"
  :url "https://github.com/justinvdm/overtone-roland-a300"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [overtone "0.8.1"]]
  :profiles {:dev {:dependencies [[speclj "2.8.1"]]}}
  :plugins [[speclj "2.8.1"]]
  :test-paths ["test"])
