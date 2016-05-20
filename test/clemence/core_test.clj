(ns clemence.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [clemence.core :as clemence]
            [criterium.core :as crit]))

(deftest a-test
  (testing "Dummy test"
    (is (= 1 1))))

(def dict (string/split (slurp "resources/words.txt") #"\s"))
(def foo  (clemence/build-trie dict))

(crit/quick-bench (clemence/levenshtein "american" foo 2))
