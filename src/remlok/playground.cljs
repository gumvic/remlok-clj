(enable-console-print!)

(ns remlok.playground
  (:require
    [reagent.ratom :as r :refer-macros [reaction]]
    [reagent.core :refer [render]]
    [remlok.loc :refer [sub]]
    [remlok.playground.wiki :refer [root]]))

(render
  [root]
  (js/document.getElementById "app"))

#_(let [a (r/atom 1)
      b (fn [r] (reaction r))
      c (reaction
          (println "run")
          (b @a))]
  (println @@c)
  (js/setTimeout #(do (swap! a inc) (println @@c)) 1000))

(let [a ()])