(ns clemence.simple
  (:require [clemence.core :as clemence])
  (:use perforate.core))

(def dict (clojure.string/split (slurp "resources/words.txt") #"\s"))
(def trie (clemence/build-trie dict))

(defgoal naive-search-eager "fuzzy search, naive --all-results")

(defcase naive-search-eager :levenshtein
  [] (run! identity (clemence/levenshtein trie "diff")))

(defcase naive-search-eager :longest-common-subsequence
  [] (run! identity (clemence/lcs trie "diff")))

;; ===================================================================

(defgoal naive-search-lazy "fuzzy search, naive -- 10 results")

(defcase naive-search-lazy :levenshtein
  [] (run! identity (take 10 (clemence/levenshtein trie "diff"))))

(defcase naive-search-lazy :longest-common-subsequence
  [] (run! identity (take 10 (clemence/lcs trie "diff"))))

;; ===================================================================

(defgoal constrained-search-eager "fuzzy search, constrained --all-results")

(defcase constrained-search-eager :levenshtein
  [] (run! identity (clemence/levenshtein trie "diff" 2)))

(defcase constrained-search-eager :longest-common-subsequence
  [] (run! identity (clemence/lcs trie "diff" 7)))

;; ===================================================================

(defgoal constrained-search-lazy "fuzzy search, constrained -- 10 results")

(defcase constrained-search-lazy :levenshtein
  [] (run! identity (take 10 (clemence/levenshtein trie "diff" 2))))

(defcase constrained-search-lazy :longest-common-subsequence
  [] (run! identity (take 10 (clemence/lcs trie "diff" 7))))
