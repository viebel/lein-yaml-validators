(defproject audyx-s3/lein-yaml-validators "0.0.1"
  :description "YAML validaton functions for clojure projects"
  :url "https://github.com/Audyx/lein-yaml-validators"
  :repositories [["audyx-s3" {:url "s3p://clojurescript/releases" :creds :gpg}]]
  :license {:name "Audyx License"
            :url "http://www.audyx.com/mentions-legales/"}
  :plugins [[s3-wagon-private "1.1.2"]]
  :dependencies [[clj-yaml "0.4.0"]
                 [viebel/audyx-toolbet "0.0.17"]
                 [me.raynes/fs "1.4.6"]
                 [io.aviso/pretty "0.1.17"]]
  :eval-in-leiningen true)
