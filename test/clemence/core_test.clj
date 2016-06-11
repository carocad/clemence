(ns clemence.core-test
  (:require [clojure.test :refer :all]
            [clemence.core :as clemence]
            [criterium.core :as crit]
            ;[clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))

(def min-words 2)
(def max-words 100)
(def word (gen/not-empty gen/string))
(def dictionary (gen/vector word min-words max-words))

; -------------------------------------------------------------------
; The levenshtein distance is simmetric, thus the order of the comparison
; doesn't matter for any two strings
; leven (P,Q) = leven (Q, P)
(defspec simmetry-property
  100; tries
  (prop/for-all [words dictionary]
    (let [trie (clemence/build-trie words)
          foo  (rand-nth words)
          bar  (rand-nth words)
          foo-bar-dist (first (filter (comp #(= bar (first %)))
                                  (clemence/levenshtein trie foo)))
          bar-foo-dist (first (filter (comp #(= foo (first %)))
                                  (clemence/levenshtein trie bar)))]
    (= (second foo-bar-dist) (second bar-foo-dist)))))
;(tc/quick-check 100 simmetry-property)

; -------------------------------------------------------------------
; The levenshtein distance is a true metric, thus the triangle-innequality
; holds for any 3 strings
; Ddf(P,Q) <= Ddf(P,R) + Ddf(R,Q)
(defspec triangle-innequality
  100; tries
  (prop/for-all [words dictionary]
    (let [trie (clemence/build-trie words)
          foo  (rand-nth words)
          bar  (rand-nth words)
          baz  (rand-nth words)
          foo-bar-dist (first (filter (comp #(= bar (first %)))
                                      (clemence/levenshtein trie foo)))
          foo-baz-dist (first (filter (comp #(= baz (first %)))
                                      (clemence/levenshtein trie foo)))
          bar-baz-dist (first (filter (comp #(= bar (first %)))
                                      (clemence/levenshtein trie baz)))]
      (<= (second foo-bar-dist) (+ (second foo-baz-dist) (second bar-baz-dist))))))

;(tc/quick-check 100 triangle-innequality)


(defspec result-type
  100; tries
  (prop/for-all [words dictionary]
    (let [trie (clemence/build-trie words)
          foo (rand-nth words)
          res (clemence/levenshtein trie foo)]
      (and (every? vector? res)
           (every? (comp string? first) res)
           (every? (comp integer? second) res)))))

;(tc/quick-check 100 result-type)

; -------------------------------------------------------------------
; The results are returned best first thus the distance of successive results
; is always increasing
; NOTE: not anymore since I introduced lazy seq. I should consider this
;       later on
#_(defspec sorted-property
  100; tries
  (prop/for-all [words dictionary]
    (let [trie (clemence/build-trie words)
          foo (rand-nth words)
          res (clemence/levenshtein trie foo)]
      (apply <= (map second res)))))

;(tc/quick-check 100 sorted-property)

; -------------------------------------------------------------------
; If the distance of two strings is 0, then the two strings are the same
; Ddf(P,Q) = 0 if P = Q
(defspec equality-property
  100; tries
  (prop/for-all [words dictionary]
    (let [trie (clemence/build-trie words)
          foo  (rand-nth words)
          foo-foo-dist (first (filter (comp #(= foo (first %)))
                                      (clemence/levenshtein trie foo)))]
    (= 0 (second foo-foo-dist)))))

;(tc/quick-check 100 equality-property)

; d(a, b) > 0 when a ≠ b, since this would require at least one operation at non-zero cost.
(defspec positive-distance
  100; tries
  (prop/for-all [foo   word
                 words dictionary]
    (let [trie     (clemence/build-trie words)
          foo-dist (clemence/levenshtein trie foo)]
      (if (get (set words) foo)
        (= 0 (second (first foo-dist)))
        (< 0 (apply min (map second foo-dist)))))))

;(tc/quick-check 100 positive-distance)

;; TODO: add random words and check that the number of words in the trie is
;;       equal to the number of input words

#_(def dict (clojure.string/split (slurp "resources/words.txt") #"\s"))
#_(def trie (clemence/build-trie dict))

;(crit/quick-bench (clemence/levenshtein trie "helli" 2))

;(crit/quick-bench (take 100 (clemence/lcs trie "hell" 3)))

#_(System/gc)
#_(time (sort-by (clemence/similarity "hemis") (clemence/starts-with trie "hemis" 1)))


#_(time (take 100 (clemence/levenshtein trie "her" 2)))

#_(time (reverse (sort-by (clemence/similarity "hemis") (clemence/lcs trie "hemis" 4))))
