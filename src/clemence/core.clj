(ns clemence.core
  (:require [clojure.zip :as zip]
            [clojure.string :as string]))

(defn- next-row
  "computes the value of the next row of the distance matrix based on the
  values from the previous row"
  [pre-row char1 str2]
  (let [init-val [(inc (first pre-row))]
        row   (fn [crow [diagonal above char2]]
                (let [dist (if (= char2 char1) diagonal
                             (inc (min diagonal above (peek crow))))]
                  (conj crow dist)))]
    (reduce row init-val (map vector pre-row (rest pre-row) str2))))


;;   Author: PLIQUE Guillaume (Yomguithereal)
;;   Source: https://gist.github.com/vishnuvyas/958488
(defn levenshtein
  "Compute the levenshtein distance (a.k.a edit distance) between two strings.
  Informally, the Levenshtein distance between two words is the minimum number
  of single-character edits (i.e. insertions, deletions or substitutions)
  required to change one word into the other"
  [str1 str2]
  (let [first-row  (range (inc (count str2)))]
    (peek (reduce #(next-row %1 %2 str2) first-row str1))))

(defrecord node [letter children word?])

(defn- branch?  [anode] (instance? clemence.core.node anode))
(defn- make-node [anode children] (assoc anode :children children))
(defn- zip-trie [root] (zip/zipper branch? :children make-node root))

(defn- loc-letter
  "search for letter among the siblings of loc. Returns a zipper
  at letter or nil on failure"
  [loc letter]
  (if (= (:letter (zip/node loc)) letter) loc
    (when-let [right (zip/right loc)]
      (recur right letter))))

(defn- add-word
  "add a word to a trie using the loc zipper"
  [loc letters]
  (let [word? (= 1 (count letters))
        nloc  (some-> loc (zip/down) (loc-letter (first letters)))]
    (cond
      (empty? letters) (zip/root loc)
      (nil? nloc) (recur (zip/down (zip/insert-child loc (->node (first letters) [] word?)))
                         (rest letters))
      :else (recur nloc (rest letters)))))

(defn- sufixes
  "retrieve all the word-sufixes that can be found from this point forward"
  [loc words]
  (if (zip/end? loc) words
    (if (:word? (zip/node loc))
      (let [sufx (string/join "" (map :letter (conj (zip/path loc) (zip/node loc))))]
        (recur (zip/next loc) (conj words sufx)))
      (recur (zip/next loc) words))))

(defn- prefix
  "retrieve all the letters up to the position of loc"
  [loc]
  (string/join "" (map :letter (zip/path loc))))

(let [root (->node "" [] false)
      t1   (add-word (zip-trie root) "cat")
      t2   (add-word (zip-trie t1) "cats")]
  (prefix (zip/next (zip/next (zip-trie t2)))))

