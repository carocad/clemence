(ns clemence.lcs-test
  (:require [clemence.core :as clemence]
            [criterium.core :as crit]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))

(def min-words 2)
(def max-words 100)
(def word (gen/not-empty gen/string))
(def dictionary (gen/vector word min-words max-words))

; -------------------------------------------------------------------
; The LCS distance is simmetric, thus the order of the comparison
; doesn't matter for any two strings
; leven (P,Q) = leven (Q, P)
(defspec lcs-simmetry
  100; tries
  (prop/for-all [words dictionary]
    (let [trie (clemence/build-trie words)
          foo  (rand-nth words)
          bar  (rand-nth words)
          foo-bar-dist (first (filter #(= bar (first %))
                                      (clemence/lcs trie foo)))
          bar-foo-dist (first (filter #(= foo (first %))
                                      (clemence/lcs trie bar)))]
    (= (second foo-bar-dist) (second bar-foo-dist)))))

;(tc/quick-check 100 lcs-simmetry)

; -------------------------------------------------------------------
; The *** distance is a true metric, thus the triangle-innequality
; holds for any 3 strings
; Ddf(P,Q) <= Ddf(P,R) + Ddf(R,Q)
#_(def lcs-triangle-innequality
  ;100; tries
  (prop/for-all [words dictionary]
    (let [trie (clemence/build-trie words)
          foo  (rand-nth words)
          bar  (rand-nth words)
          baz  (rand-nth words)
          foo-bar-dist (first (filter #(= bar (first %))
                                      (clemence/lcs trie foo)))
          foo-baz-dist (first (filter #(= baz (first %))
                                      (clemence/lcs trie foo)))
          bar-baz-dist (first (filter #(= bar (first %))
                                      (clemence/lcs trie baz)))]
      (<= (second foo-bar-dist) (+ (second foo-baz-dist) (second bar-baz-dist))))))
; NOTE: the LCS is not a distance metric, thus I'm not sure how to formulate the triangle innequality
#_(tc/quick-check 100 lcs-triangle-innequality)


(defspec lcs-result-type
  100; tries
  (prop/for-all [words dictionary]
    (let [trie (clemence/build-trie words)
          foo (rand-nth words)
          res (clemence/lcs trie foo)]
      (and (every? vector? res)
           (every? (comp string? first) res)
           (every? (comp integer? second) res)))))

(tc/quick-check 100 lcs-result-type)

; -------------------------------------------------------------------
; If the lcs length of two strings is equal to its number of letters, then the
; two strings are the same
(defspec lcs-equality
  100; tries
  (prop/for-all [words dictionary]
    (let [trie (clemence/build-trie words)
          foo  (rand-nth words)
          foo-foo-dist (first (filter #(= foo (first %))
                                      (clemence/lcs trie foo)))]
    (= (count foo) (second foo-foo-dist)))))

(tc/quick-check 100 lcs-equality)

; d(a, b) > 0 when a ≠ b, since this would require at least one operation at non-zero cost.
(defspec lcs-positive-length
  100; tries
  (prop/for-all [foo   word
                 words dictionary]
    (let [trie     (clemence/build-trie words)
          foo-dist (clemence/lcs trie foo)]
      (<= 0 (apply min (map second foo-dist))))))

(tc/quick-check 100 lcs-positive-length)

;; TODO: add random words and check that the number of words in the trie is
;;       equal to the number of input words
