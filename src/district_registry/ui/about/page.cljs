(ns district-registry.ui.about.page
  (:require
   [district-registry.ui.components.app-layout :refer [app-layout]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [re-frame.core :refer [subscribe]]))

(defmethod page :route/about []
  (fn []
    [app-layout
     [:section#main
      [:div.container
       [:div.box-wrap
        [:div.hero
         [:div.container
          [:div.header-image.sized
           [:img {:src "/images/about-header@2x.png"}]]
          [:div.page-title [:h1 "About"]]]]
        [:div.body-text
         [:div.container
          [:p.intro-text
           "The District Registry provides the district0x Network with a way of decentralizing the application process for new districts wishing to enter the network. By utilizing a Token Curated Registry design pattern, DNT holders can curate a whitelist of specific districts allowed to access the full range of district0x Network services."
           [:br] [:br]
           "We welcome all contributions from the public. If you have any thoughts or suggestions on how we can improve, please stop by the District Registry's GitHub, the district0x Reddit, or the district0x Telegram to chat with the creators and community."]
          [:div.cols
           [:p
            "What is district0x?" [:br]
            "District0x is a network of marketplaces and communities that exist as decentralized autonomous organizations on the district0x Network. All internet citizens will be able to deploy districts to the network free of charge, forever. The first district deployed Ethlance, is a decentralized job market which allows companies to hire and workers to find opportunities. Name Bazaar, the second district launched, is a peer-to-peer marketplace for the exchange of names registered via the Ethereum Name Service. Meme Factory, the third district launched, is a platform for the creation, issuance, and exchange of provably rare digital collectibles on the Ethereum blockchain."]
           [:p
            "More Information About district0x" [:br]
            "The districts launched by district0x are built on a standard open source framework comprised of Ethereum smart contracts and front-end libraries, called d0xINFRA. This powers the various districts with core functionalities that are necessary to operate an online marketplace or community; including the ability for users to post listings, filter and search those listings, rank peers and gather reputation, send invoices and collect payments. The framework is designed to be open and extendable, allowing districts to be customized and granted additional functionality through the use of auxiliary modules - which can be thought of like “plug-ins” or “extensions.”"]
           [:p
            "District0x is powered by Ethereum, Aragon, and IPFS. district0x has its own token, DNT, which is used as a means of facilitating open participation and coordination on the network. DNT can be used to signal what districts should be built and deployed by the district0x team and can be staked to gain access to voting rights in any district on the district0x Network."]
           [:p
            "Perhaps the coolest thing about district0x is that you don't need to know how to code or have any technical skill to launch a district. If you dream it - the team can build it! district0x makes use of a district proposal process to allow the community to determine what districts they would like to see built and deployed to the network next by the district0x team. Winning proposals are voted on by the community using DNT."]]]]]]]]))
