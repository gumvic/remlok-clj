(defproject gumvic/remlok "0.2.0"
  :description "Miniature Clojure(Script) UI framework with optimistic updates and all that."
  :url "https://github.com/gumvic/remlok"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [reagent "0.5.1"]]
  :profiles {:debug {:debug true}
             :dev   {:plugins [[lein-cljsbuild "1.1.2"]
                               [lein-figwheel "0.5.3-1"]]}}
  :source-paths ["src"])