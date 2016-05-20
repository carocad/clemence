(ns clemence.core
  (:require [clojure.zip :as zip]
            [clojure.string :as string]))

;; ======================== utility functions
(defn- zip-children
  "return a sequence of zippers with the children of loc"
  [loc]
  (when-let [first-child (zip/down loc)]
    (take-while (comp not nil?) (iterate zip/right first-child))))

(defn- loc-letter
  "search for letter among the siblings of loc. Returns a zipper
  at letter or nil on failure"
  [loc letter]
  (if (= (:letter (zip/node loc)) letter) loc
    (when-let [right (zip/right loc)]
      (recur right letter))))

;; ======================== core functions

(defrecord node [letter children word?])

(defn- branch?  [anode] (instance? clemence.core.node anode))
(defn- make-node [anode children] (assoc anode :children children))
(defn zip-trie [root] (zip/zipper branch? :children make-node root))

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

(defn build-trie
  "build up a trie using the sequence of words provided in dictionary. You can
  alternatively provide a trie to create a new one with the union of all the
  words"
  ([dictionary]
   (let [root (->node "" [] false)]
     (build-trie dictionary root)))
  ([dictionary root]
  (if (empty? dictionary) root
    (recur (rest dictionary) (add-word (zip-trie root) (first dictionary))))))

(defn- next-row
  "computes the value of the next row of the distance matrix based on the
  values from the previous row"
  [pre-row char1 str2]
  (let [init-val [(inc (first pre-row))]
        row      (fn [crow [diagonal above char2]]
                   (let [dist (if (= char2 char1) diagonal
                                (inc (min diagonal above (peek crow))))]
                     (conj crow dist)))]
    (reduce row init-val (map vector pre-row (rest pre-row) str2))))

(defn- next-depth
  "compute the next row of the levenshtein distance based on the word
  to compare against and on the [zipper last-row] sequence"
  [word zp-lastrow]
  (let [nzp-row (for [[parent last-row] zp-lastrow]
                  (for [child (zip-children parent)
                    :let [nrow (next-row last-row (:letter (zip/node child)) word)]]
                    (vector child nrow)))]
    (apply concat nzp-row)))

(defn- build-word
  "retrieve all the letters up to the position of loc"
  [loc]
  (string/join "" (map :letter (conj (zip/path loc) (zip/node loc)))))

(defn- distance
  "incrementally compute the levenshtein distance for all words inside the trie
  associated with the zp-row tuple sequence '([zipper last-row])"
  [word max-dist word-cost zp-row]
  (if (empty? zp-row) (sort-by second word-cost)
    (let [nzp-row (next-depth word zp-row)
          nzp-row (remove (comp #(> % max-dist) #(apply min %) second) nzp-row)
          n-words (for [[zp crow] nzp-row
                    :when (and (:word? (zip/node zp)) (> max-dist (peek crow)))]
                    [(build-word zp) (peek crow)])]
      (recur word max-dist (into word-cost n-words) nzp-row))))

(defn levenshtein
  "Compute the levenshtein distance (a.k.a edit distance) between a word and
  a dictionary of strings represented as a trie. A maximum distance can be
  supplied to optimize the performance, it defaults to 'infinity' i.e.
  no optimization.
  Informally, the Levenshtein distance between two words is the minimum number
  of single-character edits (i.e. insertions, deletions or substitutions)
  required to change one word into the other"
  ([word trie max-dist]
  (let [zp-root (zip-trie trie)
        first-row (range (inc (count word)))]
    (distance word max-dist [] (list [zp-root first-row]))))
  ([word trie]
   (levenshtein word trie Double/POSITIVE_INFINITY)))
