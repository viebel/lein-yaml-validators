(ns leiningen.yaml-validators
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [audyx-toolbet.collections :refer [filter-branches]]
            [me.raynes.fs :as fs]
            [leiningen.cljsbuild]
            [io.aviso.ansi :refer [bold-red bold-green]]
            [robert.hooke]))

(defn check-unicity [yml k]
  (println "Checking unicty of" k)
  (as->
    (filter-branches yml k) $
    (map k $)
    (frequencies $)
    (filter #(> (val %) 1) $)))

(defn unicity-error [k not-unique]
  (println (bold-red (str "Unicty validation failed for key " k)))
  (doseq [mult not-unique]
    (println (bold-red (str (first mult) " has " (second mult) " occurances")))))

(defn unicity-validator [{:keys [fields]} yml]
  (loop [fields fields
          status "passed"]
    (if (empty? fields)
      status
      (let [field (first fields)
            not-unique (check-unicity yml field)]
        (if (empty? not-unique)
          (recur (rest fields) status)
          (do
            (unicity-error field not-unique)
            (recur (rest fields) "failed")))))))

(defn parse-yaml-file [file]
  (try 
    (->
      (slurp file)
      (yaml/parse-string))
    (catch Exception e (println (bold-red e)))))

(def validations-fns {
  :unicity unicity-validator
  })

(defn run-validator [validator yml]
  (let [result (validator yml)]
    (when (= result "passed")
      (println (bold-green result)))
    result))

(defn validate-file [file validations]
  (println "\nValidating" (fs/base-name file))
  (try
    (if-let [yml (parse-yaml-file file)]
      (mapv #(run-validator % yml) validations)
      "failed")))

(defn get-validator-fn [[validation args]]
  (if-let [f (validation validations-fns)]
    (partial f args)
    (println "no such validatior:" validation)))

(defn get-validators [validations]
  (map get-validator-fn validations))

(defn run-validations [files validations]
  (let [validators (get-validators validations)]
    (mapv #(validate-file % validators) files)))

(defn get-files [folder] 
  (if folder
    (loop [all-files (file-seq folder)
            yaml-files []]
      (if (empty? all-files)
        yaml-files
        (let [current (first all-files)]
          (if (= ".yaml" (fs/extension current))
            (recur (rest all-files) (conj yaml-files current))
            (recur (rest all-files) yaml-files)))))
    []))

(defn get-source-files [yaml-project]
  (->
    (:src-folder yaml-project)
    io/file
    get-files))

(defn yaml-validators
  [project & args]
  (let [yaml-project (:yaml-validators project)
        validations (:validations yaml-project)
        src-files (get-source-files yaml-project)]
    (as->
      (run-validations src-files validations) $
      (flatten $)
      (every? #(= "passed" %) $))))

(defn sleep [ms]
  (Thread/sleep ms))

(defn validate-on-change [validations folder prev-time]
  (when (fs/exists? folder)
    (let [new-time (fs/mod-time folder)]
      (when (< prev-time new-time)
        (println "folder" (fs/base-name folder) "has been updated; revalidating YAML files")
        (run-validations (get-files folder) validations))
      new-time)))

(defn validate-changed-files [files-and-times validations]
  (reduce (partial validate-on-change validations) files-and-times files-and-times))

(defn watch-for-change [project ms]
  (let [yaml-project (:yaml-validators project)
        validations (:validations yaml-project)
        src-folder (io/file (:src-folder yaml-project))]
    (loop [mod-time (fs/mod-time src-folder)]
      (sleep ms)
      (recur (validate-on-change validations src-folder mod-time)))))
      

(defn build-hook [task & args]
  (if-let [validated? (yaml-validators (first args))]
    (do
      (when-let [watch? (last args)]
        (future
          (watch-for-change (first args) 1000)))
      (apply task args))
    (println "please fix YAML files before continuing")))


(defn activate []
  (robert.hooke/add-hook #'leiningen.cljsbuild/run-compiler #'build-hook))
