(defproject shan "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "1.0.206"]
                 [borkdude/sci "0.2.6"]]
  :plugins [[io.taylorwood/lein-native-image "0.3.1"]]
  :main ^:skip-aot shan.core
  :repl-options {:init-ns shan.core}

  :native-image {:name "shan"                 ;; name of output image, optional
                 :graal-bin "$GRAALVM_HOME" ;; path to GraalVM home, optional
                 :opts ["--verbose"]}           ;; pass-thru args to GraalVM native-image, optional

  ;; optionally set profile-specific :native-image overrides
  :profiles {:test    ;; e.g. lein with-profile +test native-image
             {:native-image {:opts ["--report-unsupported-elements-at-runtime"
                                    "--initialize-at-build-time"
                                    "--verbose"]}}

             :uberjar ;; used by default
             {:aot :all
              :native-image {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}})
