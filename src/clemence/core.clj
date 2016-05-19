(ns clemence.core
  (:require [clojure.zip :as zip]
            [clojure.string :as string])
  (:use [criterium.core]))

(defrecord node [letter children word?])

(defn- branch?  [anode] (instance? clemence.core.node anode))
(defn- make-node [anode children] (assoc anode :children children))
(defn zip-trie [root] (zip/zipper branch? :children make-node root))

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

;; (defn- sufixes
;;   "retrieve all the word-sufixes that can be found from this point forward"
;;   [loc words]
;;   (if (zip/end? loc) words
;;     (if (:word? (zip/node loc))
;;       (let [sufx (string/join "" (map :letter (conj (zip/path loc) (zip/node loc))))]
;;         (recur (zip/next loc) (conj words sufx)))
;;       (recur (zip/next loc) words))))

;; (defn- prefix
;;   "retrieve all the letters up to the position of loc"
;;   [loc]
;;   (string/join "" (map :letter (zip/path loc))))

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
  (let [init-val (vector-of :int (inc (first pre-row)))
        row      (fn [crow [diagonal above char2]]
                   (let [dist (if (= char2 char1) diagonal
                                (inc (min diagonal above (peek crow))))]
                     (conj crow dist)))]
    (reduce row init-val (map vector pre-row (rest pre-row) str2))))

(defn- build-word
  "retrieve all the letters up to the position of loc"
  [loc]
  (string/join "" (map :letter (conj (zip/path loc) (zip/node loc)))))

(defn- zip-children
  "return a sequence of zippers with the children of loc"
  [loc]
  (when-let [first-child (zip/down loc)]
    (take-while (comp not nil?) (iterate zip/right first-child))))

(defn- next-depth
  "compute the next row of the levenshtein distance based on the word
  to compare against and on the [zipper last-row] sequence"
  [word zp-lastrow]
  (let [nzp-row (for [[parent last-row] zp-lastrow]
                  ;:when (seq (zip/children parent))]
                  (for [child (zip-children parent)
                    :let [nrow (next-row last-row (:letter (zip/node child)) word)]]
                    (vector child nrow)))]
    (apply concat nzp-row)))

(defn- distance
  "incrementally compute the levenshtein distance for all words inside the trie
  associated with the zp-row tuple sequence '([zipper last-row])"
  [word word-cost zp-row]
  (if (empty? zp-row) word-cost
    (let [nzp-row (next-depth word zp-row)
          n-words (for [[zp crow] nzp-row :when (:word? (zip/node zp))]
                    [(build-word zp) (peek crow)])]
      (recur word (into word-cost n-words) nzp-row))))

(defn levenshtein
  "Compute the levenshtein distance (a.k.a edit distance) between two strings.
  Informally, the Levenshtein distance between two words is the minimum number
  of single-character edits (i.e. insertions, deletions or substitutions)
  required to change one word into the other"
  [word trie]
  (let [zp-root (zip-trie trie)
        first-row (apply vector-of :int (range (inc (count word))))]
    (distance word {} (list [zp-root first-row]))))

(def dict (string/split "Lorem Ipsum es simplemente el texto de relleno de las imprentas y archivos de texto. Lorem Ipsum ha
                        sido el texto de relleno estándar de las industrias desde el año 1500, cuando un impresor (N. del T.
                        persona que se dedica a la imprenta) desconocido usó una galería de textos y los mezcló de tal manera
                        que logró hacer un libro de textos especimen. No sólo sobrevivió 500 años, sino que tambien ingresó
                        como texto de relleno en documentos electrónicos, quedando esencialmente igual al original. Fue
                        popularizado en los 60s con la creación de las hojas Letraset, las cuales contenian pasajes de
                        Lorem Ipsum, y más recientemente con software de autoedición, como por ejemplo Aldus PageMaker,
                        el cual incluye versiones de Lorem Ipsum.

                        Es un hecho establecido hace demasiado tiempo que un lector se distraerá con el contenido del
                        texto de un sitio mientras que mira su diseño. El punto de usar Lorem Ipsum es que tiene una distribución
                        más o menos normal de las letras, al contrario de usar textos como por ejemplo Contenido aquí, contenido aquí.
                        Estos textos hacen parecerlo un español que se puede leer. Muchos paquetes de autoedición y editores de páginas
                        web usan el Lorem Ipsum como su texto por defecto, y al hacer una búsqueda de Lorem Ipsum va a dar por resultado
                        muchos sitios web que usan este texto si se encuentran en estado de desarrollo. Muchas versiones han evolucionado
                        a través de los años, algunas veces por accidente, otras veces a propósito (por ejemplo insertándole humor y cosas
                        por el estilo)."
                        #" "))
(def foo  (build-trie dict))

(with-progress-reporting
    (quick-bench (levenshtein "canibalisn" foo)
                 :verbose))
