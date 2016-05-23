# clemence

A Clojure library for fast and incremental Levenshtein distance computation by means of a trie.

Clemence is particularly useful for autocomplete task where a large list of words must be search in order to account for typos and to show the closest match to the user.

You could also use clemence as a text search engine when searching amount a description or name database. Simply load all the descriptions/names, search for the current input and compare the obtained results with the words on the description itself. Since Clemence can take typos into account, a simple equality test is all that's needed in order to check for it.

## Usage

Add the following to your :dependencies

```
[clemence "0.1.0-SNAPSHOT"]
```

Using clemence is very simple:
```Clojure
(ns example.core
  (:require [clemence.core :as clemence]))

;; populate a dictionary with the words that you want to search against
;; dict is a list of strings
(def dict (string/split (slurp "resources/words.txt") #"\s"))

;; build up a trie which will be used to incrementally compute the
;; Levenshtein distance
(def trie (clemence/build-trie dict))

;; the word that you are looking for, the trie and a maximum edit distance
;; to avoid looking up every single word
(clemence/levenshtein "clemence" trie 3)
;;=> (["clemency" 1] ["clement" 2] ["credence" 2] ["commence" 2])
```
The levenshtein function returns a sorted sequence of vectors containing a string (word) and its distance.

### notes
- This is blazing fast !. Implementing the Levenshtein distance with a trie and a threshold reduces the search space and allows it to incrementally calculate it instead of allocating a matrix for any 2 string (naive strategy)
- The resources/words.txt file shown above contains around One hundred thousand words, and the levenshtein distance above with a max-dist of 2 takes mere 130 ms on my machine.
- You should be carefull when setting a threshold as its time-impact is not linear. I generally prefer a threshold of 2 but that is up to you
- For some reasons, downloading the file with `slurp` from github seems to give be extremelly slow on my machine. If it also happens to you, simply download the file manully and read it from disk.

## License

Copyright © 2016 Camilo Roca

Distributed under the LGPL v3 License.
