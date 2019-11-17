(ns district-registry.shared.routes)

(def routes [["/" :route/home]
             ["/about" :route/about]
             ["/submit" :route/submit]
             ["/detail/:address" :route/detail]
             ["/edit/:address" :route/edit]
             ["/my-account/:tab" :route/my-account]
             ["/privacy-policy" :route/privacy-policy]])
