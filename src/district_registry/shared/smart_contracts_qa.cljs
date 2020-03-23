(ns district-registry.shared.smart-contracts-qa)

(def smart-contracts
  {:district-factory
   {:name "DistrictFactory",
    :address "0x60f33bcbaa71b95682a90831962a29edc7079492"},
   :kit-district
   {:name "KitDistrict",
    :address "0x5597abbddc2e45ce7f03b52b502ee1e4b2a0c3cb"},
   :ds-guard
   {:name "DSGuard",
    :address "0x9f888eab4543000260997fbd1cfecd60a4a3746a"},
   :param-change-registry
   {:name "ParamChangeRegistry",
    :address "0x91f1a27fcd2acbb3016cb2f1573bca605d6e3758"},
   :DNT
   {:name "District0xNetworkToken",
    :address "0xe450dcde6c059339a35eec0facbe62751cca6e8a"},
   :param-change-registry-db
   {:name "EternalDb",
    :address "0xf580e40cc19ba5a8c8033daeb91ac5763d7e04f6"},
   :param-change
   {:name "ParamChange",
    :address "0x53be703665a1039cd091d257679dc7a2cad79928"},
   :district-registry
   {:name "Registry",
    :address "0x99a0f05639700665fdcb70e170e59c5b4efd7fb8"},
   :minime-token-factory
   {:name "MiniMeTokenFactory",
    :address "0x8002dede013a00da671d8b150516ea6ee906276c"},
   :stake-bank
   {:name "StakeBank",
    :address "0x4888d43c0bea3c8fb27a8aa318588c88f0efa244"},
   :param-change-factory
   {:name "ParamChangeFactory",
    :address "0xd24590f8a6a06a1685ce17acc9b87f4b39508b7f"},
   :district-challenge
   {:name "DistrictChallenge",
    :address "0x5f89901adf673c64dcb30cd02f94872857d821a6"},
   :param-change-registry-fwd
   {:name "MutableForwarder",
    :address "0x9816ea8fcf4a0b29ef13396af0eccc7b86332105",
    :forwards-to :param-change-registry},
   :district-registry-db
   {:name "EternalDb",
    :address "0x99383ef3feaa8d8c4cd334773e47a592a1efcdd2"},
   :power
   {:name "Power",
    :address "0x30cb9ebdc3c297ec05671b7095671cf2875f178b"},
   :challenge
   {:name "Challenge",
    :address "0xe887c27a26728db3f9ad49c84a16e0d7e5e0b926"},
   :ENS
   {:name "ENS", :address "0x98df287b6c145399aaa709692c8d308357bc085d"},
   :district-registry-fwd
   {:name "MutableForwarder",
    :address "0x072c8803c0252f3b8769694393e34c2cbabd61f2",
    :forwards-to :district-registry},
   :migrations
   {:name "Migrations",
    :address "0xd79bec53D7DA4AAFddE2Cf35196EbFf17E029A75"},
   :district0x-emails
   {:name "District0xEmails",
    :address "0x44427cc6c85c57d3507f99441d672fbb410bae53"},
   :district
   {:name "District",
    :address "0x633dbbe07ab4229c610b710800c71945d6035f32"}})
