# clemence

A Clojure library for fast and incremental Levenshtein distance and Longest Common Subsequence computation by means of a trie.

Clemence is particularly useful for autocomplete task where a large list of words must be searched in order to account for typos and to show the closest match to the user.

You could also use clemence as a text search engine when searching among a description or name database. Simply load all the descriptions/names, search for the current input and compare the obtained results with the words on the description itself. Since Clemence can take typos into account, a simple equality test is all that's needed in order to check for it.

## Usage

[![Clojars Project](http://clojars.org/clemence/latest-version.svg)](http://clojars.org/clemence)

### as autocomplete tool:
```Clojure
(ns example.core
  (:require [clemence.core :as clemence]))

;; build up a trie which will be used to incrementally compute the
;; fuzzy matching. Simply pass a list of strings.
(def trie (clemence/build-trie (string/split (slurp "resources/words.txt") #"\s")))

(clemence/autocomplete trie "clem")
;; => (["clement" 0] ["clemency" 0] ["clematis" 0] ["clemency's" 0] ["clematises" 0])

```
### raw usage
```Clojure
;; the trie, the word that you are looking for and a maximum edit distance
;; to avoid looking up every single word
(clemence/levenshtein trie "clemence" 3)
;;=> (["cement" 3] ["element" 3] ["clement" 2] ["Clement" 3] ["Clemens" 3] ["credence" 2] ["commence" 2] ["clemency" 1])


;; you can alternatively use the LCS to perform a similar search
;; Nevertheless now you should specify the minimum similarity of the two words
(clemence/lcs trie "clemence" 7)
;; => (["clemency" 7] ["inclemency" 7] ["clemency's" 7] ["Clemenceau" 7] ["coalescence" 7] ["inclemency's" 7] ["complemented" 7] ["convalescence" 7]  ["coalescence's" 7] ["convalescences" 7] ["convalescence's" 7])
```
The Levenshtein distance and the Longest Common Subsequence are rather complementary. One tells you how much do two strings differ whereas the other tells you how much do they have in common.

### notes
- I have performed all benchmarks with 100000 words as a dictionary.
- If no typos occur the autocomplete function returns in a couple hundred *microseconds*. Otherwise it falls back to a fuzzy match, which returns in around 10 miliseconds.
- The Levenshtein computation is blazing fast !! Using a trie and a threshold reduces the search space and allows it to incrementally calculate it instead of allocating a matrix for any 2 string (naive strategy).
- Be carefull when using the LCS computation as it is not that fast. The LCS computation *must* traverse the whole tree because there is no way to know at which point will the words start matching. To overcome this, prefer a constrained LCS computation and don't realize the complete lazy sequence returned.
- You should be carefull when setting a threshold as its time-impact is not linear. I generally prefer a threshold of 2 but that is up to you
- You can the results of a benchmark [here](https://raw.githubusercontent.com/carocad/clemence/master/resources/benchmark.txt). The benchmark is performed by looking up the word 'diff' with both levenshtein and lcs.

## License

Copyright © 2016 Camilo Roca

Distributed under the LGPL v3 License.
