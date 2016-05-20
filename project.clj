(defproject clemence "0.1.0-SNAPSHOT"
  :description "Fast and incremental Levenshtein distance"
  :url "https://github.com/carocad/clemence"
  :license {:name "LGPLv3"
            :url "https://github.com/carocad/clemence/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :profiles {:dev {:dependencies [[criterium "0.4.4"]]}}) ; profiler
