(ns district-registry.shared.smart-contracts) 

(def smart-contracts 
{:district-factory
 {:name "DistrictFactory",
  :address "0x76e2e86b7980fd6df0310d41f39a6ef7e9414550"},
 :ds-guard
 {:name "DSGuard",
  :address "0x1b0df820c40b7e20acf01068311a8813298ad06f"},
 :param-change-registry
 {:name "ParamChangeRegistry",
  :address "0x78009f5ea37757abc5f1b54f57341faef381b255"},
 :DNT
 {:name "District0xNetworkToken",
  :address "0x15eb7ea866ff47fcb94b5422e2cfb6f31c9e6757"},
 :param-change-registry-db
 {:name "EternalDb",
  :address "0x93e502d9e1399523615cb52ba7d8eb3ecd885fca"},
 :param-change
 {:name "ParamChange",
  :address "0x9276027439f65900ca74e9278e9f9f97ffcdab01"},
 :district-registry
 {:name "Registry",
  :address "0xf1e634de8997d42322668945ebea8b554dc6933b"},
 :minime-token-factory
 {:name "MiniMeTokenFactory",
  :address "0x24c51375f85def94f73c65701d4a2a14010ae0c7"},
 :power-factory
 {:name "PowerFactory",
  :address "0xf18b47db266a06b878a2fad42190afb688447fe2"},
 :param-change-factory
 {:name "ParamChangeFactory",
  :address "0x8075ff4bb21983dfd469ff4d4eb2a9877760704e"},
 :param-change-registry-fwd
 {:name "MutableForwarder",
  :address "0xe70e05502e3d410d1febce3b30943a0b271f5c04"},
 :district-registry-db
 {:name "EternalDb",
  :address "0xbb123fed696a108f1586c21e67b2ef75f210b329"},
 :challenge-factory
 {:name "ChallengeFactory",
  :address "0x5ee9e1a61cd9f18a627ae347535408ba766548be"},
 :district-registry-fwd
 {:name "MutableForwarder",
  :address "0x53de2dc24a27918d6aa1f2eb7a3784ba6dd78e95"},
 :stake-bank-factory
 {:name "StakeBankFactory",
  :address "0xec75e590731c3bdb43f4fd76ff11f14e134f711b"},
 :district
 {:name "District",
  :address "0x110df298adbbba0283db184c221e1573ac2e0e9c"}})