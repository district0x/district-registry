(defproject district0x/district-registry "1.0.0"
  :description "A community-curated registry of marketplaces on the district0x Network."
  :url "https://github.com/district0x/district-registry"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[camel-snake-kebab "0.4.0"]
                 [cljs-web3 "0.19.0-0-10"]
                 [cljs-web3-next "0.1.3"]
                 [cljsjs/bignumber "4.1.0-0"]
                 [cljsjs/buffer "5.1.0-1"]
                 [cljsjs/filesaverjs "1.3.3-0"]
                 [cljsjs/react "16.5.2-0"]
                 [cljsjs/react-dom "16.5.2-0"]
                 [cljsjs/recharts "1.6.2-0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.taoensso/encore "2.92.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [day8.re-frame/async-flow-fx "0.1.0"]
                 [district0x/async-helpers "0.1.3"]
                 [district0x/bignumber "1.0.3"]
                 [district0x/cljs-ipfs-native "1.0.1"]
                 [district0x/cljs-solidity-sha3 "1.0.0"]
                 [district0x/district-cljs-utils "1.0.3"]
                 [district0x/district-encryption "1.0.0"]
                 [district0x/district-format "1.0.8"]
                 [district0x/district-graphql-utils "1.0.9"]
                 [district0x/district-parsers "1.0.0"]
                 [district0x/district-sendgrid "1.0.0"]
                 [district0x/district-server-config "1.0.1"]
                 [district0x/district-server-db "1.0.4"]
                 [district0x/district-server-graphql "1.0.15"]
                 [district0x/district-server-logging "1.0.6"]
                 [district0x/district-server-middleware-logging "1.0.0"]
                 [district0x/district-server-smart-contracts "1.2.4"]
                 [district0x/district-server-web3 "1.2.4"]
                 [district0x/district-server-web3-events "1.1.9"]
                 [district0x/district-ui-component-active-account "1.0.0"]
                 [district0x/district-ui-component-active-account-balance "1.0.1"]
                 [district0x/district-ui-component-form "0.2.13"]
                 [district0x/district-ui-component-notification "1.0.0"]
                 [district0x/district-ui-component-tx-button "1.0.0"]
                 [district0x/district-ui-conversion-rates "1.0.1"]
                 [district0x/district-ui-graphql "1.0.9"]
                 [district0x/district-ui-ipfs "1.0.0"]
                 [district0x/district-ui-logging "1.1.0"]
                 [district0x/district-ui-notification "1.0.1"]
                 [district0x/district-ui-now "1.0.1"]
                 [district0x/district-ui-reagent-render "1.0.1"]
                 [district0x/district-ui-router "1.0.5"]
                 [district0x/district-ui-router-google-analytics "1.0.1"]
                 [district0x/district-ui-smart-contracts "1.0.8"]
                 [district0x/district-ui-web3 "1.3.2"]
                 [district0x/district-ui-web3-account-balances "1.0.2"]
                 [district0x/district-ui-web3-accounts "1.0.6"]
                 [district0x/district-ui-web3-balances "1.0.2"]
                 [district0x/district-ui-web3-tx "1.0.11"]
                 [district0x/district-ui-web3-tx-id "1.0.1"]
                 [district0x/district-ui-web3-tx-log "1.0.13"]
                 [district0x/district-ui-window-size "1.0.1"]
                 [district0x/district-web3-utils "1.0.3"]
                 [district0x/error-handling "1.0.4"]
                 [district0x/re-frame-ipfs-fx "0.0.2"]
                 [funcool/bide "1.6.1-SNAPSHOT"]
                 [jamesmacaulay/cljs-promises "0.1.0"]
                 [medley "1.0.0"]
                 [mount "0.1.12"]
                 [org.clojure/clojurescript "1.10.439"]
                 [org.clojure/core.async "0.4.490"]
                 [print-foo-cljs "2.0.3"]
                 [re-frame "0.10.5"]
                 [reagent "0.8.1"]]

  :exclusions [express-graphql
               cljsjs/react-with-addons
               org.clojure/core.async
               district0x/async-helpers]

  :plugins [[deraen/lein-less4clj "0.7.0-SNAPSHOT"]
            [lein-auto "0.1.2"]
            [lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.18"]
            [lein-shell "0.5.0"]
            [lein-doo "0.1.8"]
            [lein-npm "0.6.2"]
            [lein-pdo "0.1.1"]]

  :doo {:paths {:karma "./node_modules/karma/bin/karma"}}

  :less4clj {:target-path "resources/public/css-compiled"
             :source-paths ["resources/public/css"]}

  :npm {:dependencies [["@sentry/node" "4.2.1"]
                       [better-sqlite3 "5.4.0"]
                       [chalk "2.3.0"]
                       [eccjs "0.3.1"]
                       [cors "2.8.4"]
                       [express "4.15.3"]
                       ;; needed until v0.6.13 is officially released
                       [express-graphql "./resources/libs/express-graphql-0.6.13.tgz"]
                       [graphql "0.13.1"]
                       [graphql-fields "1.0.2"]
                       [graphql-tools "3.0.1"]
                       [source-map-support "0.5.3"]
                       [ws "4.0.0"]
                       ;; district0x/district-server-web3 needs [ganache-core "2.0.2"]   who also needs "ethereumjs-wallet": "~0.6.0"
                       ;; https://github.com/ethereumjs/ethereumjs-wallet/issues/64
                       [ethereumjs-wallet "0.6.0"]
                       [truffle "5.1.0"]
                       ;; truffle script deps
                       [jsedn "0.4.1"]
                       [minimetoken "0.2.0"]
                       [openzeppelin-solidity "2.3.0"]
                       ["@truffle/hdwallet-provider" "1.0.25"]
                       [dotenv "8.0.0"]
                       ;; before its in cljsjs
                       [web3 "1.2.0"]

                       ;; Aragon dependencies:
                       ["@aragon/os" "4.2.0"]
                       ["@aragon/kits-base" "1.0.0"]
                       ["@aragon/id" "2.0.3"]
                       ;; Apps should be latest version Aragon deployed to mainnet, not newer
                       ["@aragon/apps-voting" "2.1.0"]
                       ["@aragon/apps-vault" "3.0.0"]
                       ["@aragon/apps-finance" "2.1.0"]
                       ["eth-ens-namehash" "2.0.8"]
                       ["web3-utils" "1.0.0-beta.55"]
                       ["bluebird" "3.5.2"]]}

  :source-paths ["src" "test"]

  :figwheel {:server-port 4177
             :css-dirs ["resources/public/css" "resources/public/css-compiled"]
             :repl-eval-timeout 60000}

  :aliases {"clean-prod-server" ["shell" "rm" "-rf" "server"]
            "watch-css" ["less4clj" "auto"]
            "build-css" ["less4clj" "once"]
            "build-prod-server" ["do" ["clean-prod-server"] ["cljsbuild" "once" "server"]]
            "build-prod-ui" ["do" ["clean"] ["cljsbuild" "once" "ui"]]
            "build-prod" ["pdo" ["build-prod-server"] ["build-prod-ui"] ["build-css"]]
            "build-tests" ["cljsbuild" "once" "server-tests"]
            "test" ["do" ["build-tests"] ["shell" "node" "server-tests/server-tests.js"]]
            "test-doo" ["doo" "node" "server-tests"]
            "test-doo-once" ["doo" "node" "server-tests" "once"]}

  :clean-targets ^{:protect false} [[:solc :build-path]
                                    ".cljs_node_repl"
                                    "dev-server/"
                                    "resources/public/css-compiled/"
                                    "resources/public/js/compiled/"
                                    "server-tests/"
                                    "server/"
                                    "target/"]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [org.clojure/clojurescript "1.10.439"]
                                  [org.clojure/core.async "0.4.490"]
                                  [binaryage/devtools "0.9.10"]
                                  [cider/piggieback "0.4.0"]
                                  [figwheel-sidecar "0.5.18"]
                                  [lein-doo "0.1.8"]
                                  [org.clojure/clojure "1.9.0"]
                                  [org.clojure/tools.reader "1.3.0"]]
                   :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]
                                  :timeout 120000}
                   :source-paths ["dev" "src"]
                   :resource-paths ["resources"]}}

  :cljsbuild {:builds [{:id "dev-server"
                        :source-paths ["src/district_registry/server" "src/district_registry/shared"]
                        :figwheel {:on-jsload "district-registry.server.dev/on-jsload"}
                        :compiler {:main "district_registry.server.dev"
                                   :output-to "dev-server/district-registry.js"
                                   :output-dir "dev-server"
                                   :target :nodejs
                                   :optimizations :none
                                   :static-fns true
                                   :fn-invoke-direct true
                                   :anon-fn-naming-policy :mapped
                                   :source-map true}}
                       {:id "dev"
                        :source-paths ["src/district_registry/ui" "src/district_registry/shared"]
                        :figwheel {:on-jsload "district.ui.reagent-render/rerender"}
                        :compiler {:main "district-registry.ui.core"
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "/js/compiled/out"
                                   :source-map-timestamp true
                                   :preloads [print.foo.preloads.devtools]
                                   :external-config {:devtools/config {:features-to-install :all}}}}
                       {:id "server"
                        :source-paths ["src"]
                        :compiler {:main "district-registry.server.core"
                                   :output-to "server/district-registry.js"
                                   :output-dir "server"
                                   :target :nodejs
                                   :optimizations :simple
                                   :source-map "server/district-registry.js.map"
                                   :pretty-print false}}
                       {:id "ui"
                        :source-paths ["src"]
                        :compiler {:main "district-registry.ui.core"
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :pseudo-names false}}
                       {:id "server-tests"
                        :source-paths ["src/district_registry/server" "src/district_registry/shared" "test/district_registry"]
                        :compiler {:main "district-registry.tests.runner"
                                   :output-to "server-tests/server-tests.js"
                                   :output-dir "server-tests"
                                   :target :nodejs
                                   :optimizations :none
                                   :verbose false
                                   :source-map true}}]})
