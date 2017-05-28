(def project 'fclj)
(def version "0.1.0-SNAPSHOT")

(set-env! :resource-paths #{"src"}
          :dependencies   '[[org.clojure/clojure "RELEASE"]
                            [nightlight "1.6.4" :scope "test"]
                            [enlive "1.1.6"]])

(require '[nightlight.boot :refer [nightlight]]
         '[crawler.core :as crawler])

(deftask crawl []
  (fn [next-task]
    (crawler/crawl-all-problems)
    (fn [fileset]
      (next-task fileset))))

(deftask dev []
  (comp (wait) (repl :server true) (nightlight :port 4000)))
