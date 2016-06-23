(ns clemence.benchmark
  (:require [clemence.core :as clemence]
            [criterium.core :as crit]))

(set! *warn-on-reflection* true)

(def dict (clojure.string/split (slurp "resources/words.txt") #"\s"))
(def trie (clemence/build-trie dict))

#_(def output
(with-out-str
(println "Goal: fuzzy search, naive --all-results")

(println "case: levenshtein")
(crit/quick-bench (run! identity (clemence/levenshtein trie "diff")))

(println "case: longest-common-subsequence")
(crit/quick-bench (run! identity (clemence/lcs trie "diff")))

(println "===================================================================")

(println "Goal: fuzzy search, naive -- 10 results")

(println "case: levenshtein")
(crit/quick-bench (run! identity (take 10 (clemence/levenshtein trie "diff"))))

(println "case: longest-common-subsequence")
(crit/quick-bench (run! identity (take 10 (clemence/lcs trie "diff"))))

(println "===================================================================")

(println "Goal: fuzzy search, constrained --all-results")

(println "case: levenshtein")
(crit/quick-bench (run! identity (clemence/levenshtein trie "diff" 2)))

(println "case: longest-common-subsequence")
(crit/quick-bench (run! identity (clemence/lcs trie "diff" 7)))

(println "===================================================================")

(println "Goal: fuzzy search, constrained -- 10 results")

(println "case: levenshtein")
(crit/quick-bench (run! identity (take 10 (clemence/levenshtein trie "diff" 2))))

(println "case: longest-common-subsequence")
(crit/quick-bench (run! identity (take 10 (clemence/lcs trie "diff" 7))))

))

#_(spit "resources/benchmark.txt" output)
