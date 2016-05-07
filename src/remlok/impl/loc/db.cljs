(ns remlok.impl.loc.db
  (:require
    [reagent.ratom :as r]))

(def db
  (r/atom nil))