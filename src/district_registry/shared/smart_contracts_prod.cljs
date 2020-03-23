(ns district-registry.shared.smart-contracts-prod)

(def smart-contracts
  {:district-factory
   {:name "DistrictFactory",
    :address "0xb764e4d3693e8710078231eef102d9a9fa210718"},
   :kit-district
   {:name "KitDistrict",
    :address "0x5146ecacdd58ee496ea289cefdf4d6b1389ffb2d"},
   :ds-guard
   {:name "DSGuard",
    :address "0xdc7bb58d93723bc66c8d14cd9a2d872434d0ef11"},
   :param-change-registry
   {:name "ParamChangeRegistry",
    :address "0x28fe8b17f8b160cfe2f0306c64a78d36a7c22eda"},
   :DNT
   {:name "District0xNetworkToken",
    :address "0x0abdace70d3790235af448c88547603b945604ea"},
   :param-change-registry-db
   {:name "EternalDb",
    :address "0x08067e66962d0552c7d0ecd282aab1fd8bdc7798"},
   :param-change
   {:name "ParamChange",
    :address "0xbfb67aa3bf93605bda7042b2f88295931324f1eb"},
   :district-registry
   {:name "Registry",
    :address "0x5ac7465eb6ee44628cc3f536bc186ada3cecdfd4"},
   :minime-token-factory
   {:name "MiniMeTokenFactory",
    :address "0xabf3cc75b8190c31e4b701e098f8967e28aff4dc"},
   :stake-bank
   {:name "StakeBank",
    :address "0xab1b0af33c57f663cf212c33324cc0dadd4e5336"},
   :param-change-factory
   {:name "ParamChangeFactory",
    :address "0x02256cff6ffa00d0d95800560da8451be1f39332"},
   :district-challenge
   {:name "DistrictChallenge",
    :address "0xe95fdf58223e96b6a771068f25ce01f086eaad7b"},
   :param-change-registry-fwd
   {:name "MutableForwarder",
    :address "0x6f0d1a2d55f4b25838ce907939eff84ff4468197",
    :forwards-to :param-change-registry},
   :district-registry-db
   {:name "EternalDb",
    :address "0xab00eca134b32c8a2cdeb02f84edfd91bdce2120"},
   :power
   {:name "Power",
    :address "0xcbfe3cd59528da1e68326b133e96ea8ded6ad9d4"},
   :challenge
   {:name "Challenge",
    :address "0x13b1cd8cb253eca8142ebf423e3b86f789763ec7"},
   :ENS
   {:name "ENS", :address "0x314159265dd8dbb310642f98f50c066173c1259b"},
   :district-registry-fwd
   {:name "MutableForwarder",
    :address "0xf8d2c216aeeddabeb1c80a3560e56a20bda434dd",
    :forwards-to :district-registry},
   :migrations
   {:name "Migrations",
    :address "0x163A3866860B216577FFb8918f333B6ebc33c85e"},
   :district0x-emails
   {:name "District0xEmails",
    :address "0x5065ef0724b76421aeaafa87ba752d6c5d5499b5"},
   :district
   {:name "District",
    :address "0xfffdc87fb02fa464b5445c561f13e629ae6810dd"}})
