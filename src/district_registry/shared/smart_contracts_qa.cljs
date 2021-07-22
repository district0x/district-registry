(ns district-registry.shared.smart-contracts-qa)

(def smart-contracts
  {:ds-guard {:name "DSGuard" :address "0x1cb109f8c2596fae8836cc5fef660dbb44c7a9cb"}
   :minime-token-factory {:name "MiniMeTokenFactory" :address "0x2ccfbbdec1c601ff174dc7d81179630360010aef"}
   :DNT {:name "District0xNetworkToken" :address "0x0de9ca6f78930471ff242a3d07eaa32abba07677"}
   :district-registry-db {:name "EternalDb" :address "0x49b3ebdf3defac43f45498ce5b1c1379f631896a"}
   :param-change-registry-db {:name "EternalDb" :address "0xac8860b290bfced2609d965ebce125a9e8f7b08e"}
   :district-registry {:name "Registry" :address "0x093a9e3fd7ea37033c2396f56361deb90ec14764"}
   :district-registry-fwd {:name "MutableForwarder" :address "0x4e0505fe118c4187b07f2e99a5f8624e65afdad5" :forwards-to :district-registry}
   :param-change-registry {:name "ParamChangeRegistry" :address "0xa7c6ee146474c96c5eda3fc34d7c874db2510f1b"}
   :param-change-registry-fwd {:name "MutableForwarder" :address "0x38acd17c49c9c5160d884c3ec0ba736b60d0984e" :forwards-to :param-change-registry}
   :power {:name "Power" :address "0x79d9539d1c895f332782eb9a7b3c6318f3c961c9"}
   :stake-bank {:name "StakeBank" :address "0x8c731c0405ae74050b8165c74f7762d177df5809"}
   :challenge {:name "Challenge" :address "0x94a4733014767ade2eb2d95681008aeda80587bf"}
   :district-challenge {:name "DistrictChallenge" :address "0xc8555dc3f93e1d37e23c1a5811694284c13b0521"}
   :ENS {:name "ENS" :address "0x00000000000c2e074ec69a0dfb2997ba6c7d2e1e"}
   :district {:name "District" :address "0xcedc054ebe726b8a469a50c302d692eea5bfcb14"}
   :param-change {:name "ParamChange" :address "0x0eef698bcacfec097c1c8bdd73501a2849bbd40e"}
   :district-factory {:name "DistrictFactory" :address "0x40b47eb7da61e279f4f82d74b8ad239622343120"}
   :param-change-factory {:name "ParamChangeFactory" :address "0x6dc78010bb5b74db57f360a4a6b009013d6714a1"}
   :district0x-emails {:name "District0xEmails" :address "0xb4996f78c380e19a33703bd788270031849ba92b"}
   })
