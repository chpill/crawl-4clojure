(ns crawler.core
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as enlive]))

(def url "http://4clojure.com/problem/")

(def pb-range (range 1 200))

(def title-selector :#prob)
:#tags ;; this will need some formatting
:#prob-desc
:pre.test

(defn extract [document selector extract-fn]
  (->> (enlive/select document [selector])
       (map extract-fn)))

(defn get-table-content [data]
  (cond
    (string? data) data
    (seq? (:content data)) (apply str (map get-table-content
                                           (:content data)))))

(defn extract-meta [document]
  (let [tables (-> (enlive/select document [:#tags])
                   first
                   :content)]
    (keep (comp
           ;; get rid of empty topic line
           (fn [s] (when-not (= s "Topics: ") s))
           (fn [s] (str/replace s ":" ": "))
                get-table-content)
          tables)))

(defn get-description-content [content]
  (cond
    (string? content)     content
    (= :a (:tag content)) (-> content :attrs :href)
    (seq? (:content content)) (apply str (map get-description-content (:content content)))))

(defn extract-description [document]
  (let [full-description-block (first (extract document :#prob-desc :content))]
    (->> (take-while (fn [content]
                       (not (and (= :table (:tag content))
                                 (= "testcases" (-> content :attrs :class)))))
                     full-description-block)
         (keep get-description-content))))

(defn get-difficulty [s]
  (->> (re-find #"(Elementary|Easy|Medium|Hard)" s)
       first
       str/lower-case))

(defn extract-restrictions [document]
  (-> (extract document :#restrictions :content)
      first
      last
      :content
      (->> (map (comp first :content)))))

(defn spit-problem [problem-number]
  (try
    (let [document (enlive/html-resource (java.net.URL. (str url problem-number)))

          title (extract document :#prob-title (comp first :content))
          formatted-title (apply str (map #(str/upper-case (str ";; " %)) title))

          meta (extract-meta document)
          formatted-meta (str/join "\n"
                                   (map #(str ";; " %) meta))

          description  (extract-description document)

          restrictions (extract-restrictions document)
          formatted-restrictions (str/join "\n"
                                           (into [";; Special restrictions:"]
                                                 (map #(str ";; " %) restrictions)))

          formatted-description (->> (map (fn [text-block]
                                            (->> (str/split text-block #"\n")
                                                 (map #(str (when-not (str/blank? %) ";; ")
                                                            %))
                                                 (str/join "\n")))
                                          description)
                                     (apply str))

          test-cases  (extract document :pre.test (comp first :content))]

      (if (some empty? [title description test-cases])
        (println "nothing for" problem-number)

        (if-let [difficulty (get-difficulty (first meta))]
          (->> (str/join "\n"
                         (into [(str/join
                                 "\n\n"
                                 (keep identity
                                       [(str "(ns "
                                             difficulty
                                             ".p"
                                             problem-number
                                             ")")
                                        formatted-title
                                        formatted-meta
                                        (when-not (empty? restrictions)
                                          formatted-restrictions)
                                        formatted-description
                                        "(def __\n  ,,,\n  ;; Your code here!\n  ,,,)"
                                        ";; Test cases:"]))]
                               test-cases))
               (spit (str "tmp/"
                          difficulty
                          "/p"
                          (format "%03d" problem-number)
                          ".clj")))
          (println "unknown difficulty setting for" problem-number))))
    (catch Exception e
      (println "error processing problem !" problem-number)
      (println e))))

(defn crawl-all-problems []
  (doall (pmap spit-problem (range 1 300))))


