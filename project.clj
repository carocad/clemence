(defproject clemence "0.2.1"
  :description "Fast and incremental Levenshtein and Longest Common Subsequence computation"
  :url "https://github.com/carocad/clemence"
  :license {:name "LGPLv3"
            :url "https://github.com/carocad/clemence/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [fast-zip "0.7.0"]]

  :plugins [[perforate "0.3.4"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]
                                  [criterium "0.4.4"]]
                   :source-paths ["src/"] ;;perforate
                   }})
