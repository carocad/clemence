(ns clemence.core
  (:require [fast-zip.core :as zip]))

(set! *warn-on-reflection* true)

(comment
  TODO, consider putting a :data field in the node records to allow linking back
            to a specific information that the user might want)

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

(defn- height
  "look-ahead and check if node's number of children is at least max-depth.
  Does so in a depth-first way. Returns the maximum depth reached or max-depth
  on success"
  ([node max-depth] (height node max-depth 0))
  ([node max-depth cdepth]
   (if (empty? (:children node)) 0
     (apply max (cons 0
                      (for [child (:children node)
                            :while (< cdepth max-depth)]
                        (inc (height child max-depth (inc cdepth)))))))))

(defn- deep-enough?
  [node target]
  (if-not (pos? target) true
    (= target (height node target))))

;; ======================== Trie functions

(defrecord node [letter children word?])

(defn- branch?  [anode] (instance? clemence.core.node anode))
(defn- make-node [anode children] (assoc anode :children children))
(defn- zip-trie [root] (zip/zipper branch? (comp seq :children) make-node root))

(defn- add-word
  "add a word to a trie using the loc zipper"
  [loc letters]
  (let [word? (= 1 (count letters))
        nloc  (some-> loc (zip/down) (loc-letter (first letters)))]
    (cond
      (empty? letters) (zip/root loc)
      (nil? nloc) (recur (zip/down (zip/insert-child loc (->node (first letters) [] word?)))
                         (rest letters))
      (true? word?) (recur (zip/edit nloc #(assoc % :word? true))
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

;; ======================== word retrieving functions

(defn- prefix
  "retrieve all the letters up to the position of loc (excluding)"
  [loc]
  (apply str (map :letter (zip/path loc))))

(defn- build-word
  "retrieve all the letters up to the position of loc"
  [loc]
  (str (prefix loc) (:letter (zip/node loc))))

(defn- suffixes
  "given a trie's loc returns all suffixes that can be build from that point
  onwards (breath first)"
  [loc]
  (for [siblings (take-while not-empty (iterate #(mapcat zip-children %) (zip-children loc)))
        child    siblings
    :when (:word? (zip/node child))]
    (build-word child)))

;; ======================== String metrics functions

;; LCS implementation inspired by: ICS 161: Design and Analysis of Algorithms
;; Lecture notes for February 29, 1996. https://www.ics.uci.edu/~eppstein/161/960229.html
(defn- lcs-next-row
  "computes the value of the next row of an lcs distance matrix based on the
  values from the previous row"
  [pre-row char1 str2]
  (let [init-val [0]
        row   (fn [crow [diagonal above char2]]
                       (let [length (if (= char2 char1) (inc diagonal)
                                      (max above (peek crow)))]
                         (conj crow length)))]
    (reduce row init-val (map vector pre-row (rest pre-row) str2))))

(defn- edit-next-row
  "computes the value of the next row of an edit (a.k.a levenshtein) distance
  matrix based on the values from the previous row"
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
  [word zp-lastrow next-type]
  (let [next-row (if (= next-type :edit) edit-next-row
                   lcs-next-row)
         nzp-row (for [[parent last-row] zp-lastrow]
                  (for [child (zip-children parent)
                        :let [nrow (next-row last-row (:letter (zip/node child)) word)]]
                    (vector child nrow)))]
    (apply concat nzp-row)))

(defn- start-distance
  "incrementally compute the levenshtein distance for all word-sections inside
  the trie (represented by zp-row tuple sequence '([zipper last-row])).
  Computation is performed on a depth-first search and it stops once the depth
  equals the size of the search word"
  [word max-dist cdepth zp-row]
  (let [nzp-row (next-depth word zp-row :edit)
        nzp-row (remove (comp #(> % max-dist) #(apply min %) second) nzp-row)]
    (if-not (or (empty? nzp-row) (>= cdepth (count word)))
      (recur word max-dist (inc cdepth) nzp-row)
      (for [[zp crow] zp-row
            suffix    (suffixes (zip-trie (zip/node zp)))
        :let [pre-word (prefix zp)]]
        [(str pre-word suffix) (apply min crow)]))))

(defn- strict-start
  "check letter by letter the words stored in the trie (loc) to see if
  any of them start with 'letters'. Returns a lazy seq of words starting
  with letters"
  [loc letters]
  (let [nloc (some-> loc (zip/down) (loc-letter (first letters)))]
    (cond
      (empty? letters) (for [suffix (suffixes (zip-trie (zip/node loc)))
                        :let [pre-word (prefix loc)]]
                         (str pre-word suffix))
      (nil? nloc) nil
      :else (recur nloc (rest letters)))))

(defn- edit-distance
  "incrementally compute the levenshtein distance for all words inside the trie
  represented by zp-row tuple sequence '([zipper last-row])"
  [word max-dist zp-row]
  (if (empty? zp-row) nil
    (let [nzp-row (next-depth word zp-row :edit)
          nzp-row (remove (comp #(> % max-dist) #(apply min %) second) nzp-row)
          words   (for [[zp crow] nzp-row
                    :when (and (:word? (zip/node zp)) (>= max-dist (peek crow)))]
                    [(build-word zp) (peek crow)])]
      (concat words (lazy-seq (edit-distance word max-dist nzp-row))))))

(defn- lcs-distance
  "incrementally compute the lcs length for all words inside the trie
  represented by zp-row tuple sequence '([zipper last-row])"
  [word min-length zp-row]
  (if (empty? zp-row) nil
    (let [nzp-row (next-depth word zp-row :lcs)
          nzp-row (filter #(deep-enough? (zip/node (first %))
                                         (- min-length (peek (second %))))
                          nzp-row)
          words   (for [[zp crow] nzp-row
                    :when (and (:word? (zip/node zp)) (>= (peek crow) min-length))]
                    [(build-word zp) (peek crow)])]
      (concat words (lazy-seq (lcs-distance word min-length nzp-row))))))

;; ======================== api functions

(defn starts-with
  "Compute the levenshtein distance between a word and all substrings of the
  same length present in a dictionary of strings represented as a trie. Returns
  a lazy sequence of [match distance] couples of all the words starting with
  'word' and having a substring distance less than max-dist.
  A maximum distance can be supplied to optimize the performance, it defaults
  to 'infinity' i.e. no optimization"
  ([trie word]
   (starts-with trie word Double/POSITIVE_INFINITY))
  ([trie word max-dist]
   (let [zp-root    (zip-trie trie)
         first-row  (range (inc (count word)))]
     (if (empty? word) (list [word 0])
       (start-distance word max-dist 0 (list [zp-root first-row]))))))

(defn levenshtein
  "Compute the levenshtein distance (a.k.a edit distance) between a word and
  a dictionary of strings represented as a trie. A maximum distance can be
  supplied to optimize the performance, it defaults to 'infinity' i.e.
  no optimization. Returns a lazy sequence of [match dist] of all the words
  that match the search term.

  Informally, the Levenshtein distance between two words is the minimum number
  of single-character edits (i.e. insertions, deletions or substitutions)
  required to change one word into the other"
  ([trie word]
   (levenshtein trie word Double/POSITIVE_INFINITY))
  ([trie word max-dist]
   (let [zp-root    (zip-trie trie)
         first-row  (range (inc (count word)))]
     (edit-distance word max-dist (list [zp-root first-row])))))

(defn lcs
  "Compute the longest common subsequence length between a word and a
  dictionary of strings represented as a trie. A minimum length can be
  supplied to restrict the returned matches, it defaults to 0 i.e. no
  restriction. Returns a lazy sequence of [match length].

  Informally, the longest common subsequence length between two words is the
  maximum number of consecutive single-character present in both words"
  ([trie word]
   (lcs trie word 0))
  ([trie word min-length]
   (let [zp-root    (zip-trie trie)
         first-row  (repeat (inc (count word)) 0)]
     (lcs-distance word min-length (list [zp-root first-row])))))

(defn similarity
  "takes a word and returns a function to use with sort-by for better results
  ordering.

  Example: (sort-by (similarity \"foo\") (starts-with trie \"foo\"))"
  [word]
  (juxt second #(count (first %)) #(compare word (first %))))

(defn autocomplete
  "takes an incomplete word (text) and search for words with a similar start
  substring. Returns a sequence of [word dist] where dist is the text-substring
  levenshtein distance"
  ([trie text] (autocomplete trie text 5))
  ([trie text max-words]
   (let [zp-root  (zip-trie trie)
         fresults (strict-start zp-root text)]
     (if-not (empty? fresults) (take max-words (map #(vector % 0) fresults))
       (loop [max-dist 1
              words   (take max-words (starts-with trie text max-dist))]
         (if (or (>= (count words) max-words) (>= max-dist (count text))) words
           (recur (inc max-dist)
              (take max-words
                    (concat words (starts-with zp-root text (inc max-dist)))))))))))
